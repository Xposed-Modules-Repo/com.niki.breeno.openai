import com.zephyr.log.logE
import com.zephyr.log.logI
import com.zephyr.provider.TAG
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

/**
 * 一个“工作单元”，封装了执行块下载所需的所有逻辑和上下文。
 *
 * @param taskUrl 用于公平调度的分组键 (Task ID)
 * @param logPrefix 日志前缀
 * @param deferred 用于向调用者报告完成或失败
 * @param block 要执行的挂起 lambda (实际的下载I/O逻辑)
 */
data class ChunkWorkItem(
    val taskUrl: String,
    val logPrefix: String,
    val deferred: CompletableDeferred<Unit>,
    val block: suspend () -> Unit
)

/**
 * 全局下载调度器 (单例)
 * * 实现了你构想的“管理类”：
 * 1. 维护一个按任务分组的公平队列 (taskQueues)。
 * 2. 使用 Round-Robin 算法 (pollFairly) 来“调整”和分发工作。
 * 3. 使用一个固定大小的信号量 (semaphore) 来“消费”工作，实现并发控制。
 */
object GlobalDownloadScheduler {

    // 1. “通过大小为2信号量来消费”
    private val semaphore = Semaphore(2)

    // 2. “维护另一个数据结构”
    //    Map<TaskURL, Queue<WorkItem>>
    private val taskQueues = ConcurrentHashMap<String, LinkedList<ChunkWorkItem>>()
    private val queueMutex = Mutex() // 保护对 taskQueues 结构的访问

    // 调度器循环，使用 SupervisorJob 确保一个 chunk 的失败不会杀死调度器
    private val dispatcherScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 一个“信号”，用于在有新工作或有 worker 完成时唤醒调度器
    private val workSignal = Channel<Unit>(Channel.CONFLATED)

    // 记录上次从哪个队列中提取了工作，用于实现公平的 Round-Robin
    private var lastPolledKeyIndex = 0

    init {
        logI(TAG, "GlobalDownloadScheduler: Initializing dispatcher...")
        dispatcherScope.launch { runDispatcher() }
    }

    /**
     * MultiThreadDownloader 调用此函数来“注册”其所有工作块。
     * 此函数将挂起，直到所有提交的 workItems 完成或失败。
     */
    suspend fun executeTask(workItems: List<ChunkWorkItem>) {
        if (workItems.isEmpty()) return

        // 使用 coroutineScope 来等待所有 deferreds 完成
        // 这也确保了如果一个 chunk 失败 (deferred.await() 抛出异常), 
        // 整个 executeTask 调用也会抛出异常, 从而通知 MultiThreadDownloader。
        try {
            coroutineScope {
                // 1. “一股脑向某个管理类注册这些任务块”
                queueMutex.withLock {
                    workItems.forEach { item ->
                        val queue = taskQueues.getOrPut(item.taskUrl) { LinkedList() }
                        queue.add(item)
                    }
                }
                // 2. 唤醒调度器，有新工作了
                workSignal.send(Unit)

                // 3. 启动一个"监听"协程来等待此任务的所有 deferreds
                val jobs = workItems.map { item ->
                    launch {
                        item.deferred.await() // 挂起直到这个 chunk 完成或失败
                    }
                }

                // jobs.joinAll() // 注意：这里不需要 joinAll，
                // 因为 coroutineScope 会自动等待所有子协程 (launch) 完成。
                // 而这些 launch 协程又在等待 deferred.await()。
            }
        } finally {
            // 任务完成（或失败）后，从 Map 中清理，防止内存泄漏
            queueMutex.withLock {
                val taskUrl = workItems.first().taskUrl
                taskQueues.remove(taskUrl)
                logI(TAG, "GlobalDownloadScheduler: Task $taskUrl completed. Cleaning up queue.")
            }
        }
    }

    /**
     * 调度器的主循环 (Consumer)
     */
    private suspend fun runDispatcher() {
        while (true) {
            // 1. 等待工作信号 (来自 submitTask 或 worker 释放)
            workSignal.receive()

            // 2. 循环，只要信号量有空闲且队列中有工作
            while (semaphore.tryAcquire()) {
                // 3. “调整当前队列的结构” (公平轮询)
                val workItem = pollFairly()

                if (workItem == null) {
                    // 没有工作，归还信号量并继续等待
                    semaphore.release()
                    break // 退出内部 while 循环, 返回去等 workSignal
                }

                // 4. “消费” - 启动一个 worker 协程来执行这个 chunk
                logI(TAG, "Scheduler: [${workItem.logPrefix}] Got permit. Starting work.")
                dispatcherScope.launch {
                    try {
                        workItem.block() // 执行实际的下载I/O
                        workItem.deferred.complete(Unit)
                    } catch (e: Exception) {
                        logE(TAG, "Scheduler: [${workItem.logPrefix}] Work failed")
                        logE(TAG, e.stackTraceToString())
                        workItem.deferred.completeExceptionally(e)
                    } finally {
                        logI(TAG, "Scheduler: [${workItem.logPrefix}] Work done. Releasing permit.")
                        semaphore.release() // 释放信号量
                        workSignal.send(Unit) // 唤醒调度器，检查是否还有更多工作
                    }
                }
            }
        }
    }

    /**
     * 核心公平算法 (Round-Robin)
     */
    private suspend fun pollFairly(): ChunkWorkItem? = queueMutex.withLock {
        if (taskQueues.isEmpty()) return null

        val keys = taskQueues.keys.toList()
        if (lastPolledKeyIndex >= keys.size) {
            lastPolledKeyIndex = 0 // 循环
        }

        val startIdx = lastPolledKeyIndex
        var i = startIdx

        do {
            val key = keys[i]
            val queue = taskQueues[key]

            val item = queue?.poll() // 从队列头取一个
            if (item != null) {
                if (queue.isEmpty()) {
                    // 如果这是该任务的最后一个 chunk，我们可以在下次轮询时跳过它
                    // (但为了简单，让它在 finally 中被移除)
                }
                lastPolledKeyIndex = (i + 1) % keys.size // 下次从下一个 task 开始
                return item
            }

            i = (i + 1) % keys.size // 检查下一个 task
        } while (i != startIdx) // 循环了整整一圈

        return null // 所有队列都为空
    }
}