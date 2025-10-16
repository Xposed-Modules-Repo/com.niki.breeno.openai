package com.niki914.breeno.model.llm

import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.breeno.HookEntry
import com.niki914.breeno.model.breeno.BreenoUIInterceptor
import com.niki914.breeno.model.llm.prefs.DynamicOtherChatPrefsHolder
import com.niki914.breeno.model.llm.prefs.OtherChatPrefsBuilder
import com.niki914.chat.Chat
import com.niki914.chat.ChatConfigBuilder
import com.niki914.chat.beans.ChatEvent
import com.niki914.chat.beans.Message
import com.niki914.chat.beans.ToolCall
import com.niki914.chat.beans.ToolDefinition
import com.niki914.core.logD
import com.niki914.core.logE
import com.niki914.core.logRelease
import com.niki914.core.logV
import com.niki914.tool.call.ToolManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * TODO 参考 ChatTestingViewModel 优化对话管理
 * @see com.niki914.breeno.viewmodel.ChatTestingViewModel
 */
class XChatManager(
    private val scope: CoroutineScope,
    initialApiKey: String = "",
    initialModel: String = "",
    initialPrompt: String? = null,
    initialTools: List<ToolDefinition>? = null,
) {
    private val otherChatPrefsHolder = DynamicOtherChatPrefsHolder()

    private val uIInterceptor = BreenoUIInterceptor() // 用于注入 UI、阻塞官方响应等

    private val breenoSemaphore =
        BreenoSemaphore() // 用于控制 UI 注入，额，就是等官方 ID 拿到后才放行原来的代码块


    private val showToolCalling: Boolean
        get() = otherChatPrefsHolder.getPrefs().showToolCalling

    private val fallback: String
        get() = otherChatPrefsHolder.getPrefs().fallback

    private val enableRootAccess: Boolean
        get() = otherChatPrefsHolder.getPrefs().enableRootAccess

    private val isBlackList: Boolean
        get() = otherChatPrefsHolder.getPrefs().isBlackList

    private val list: String
        get() = otherChatPrefsHolder.getPrefs().list

    // TODO 解耦
    private fun getExtras(): Map<String, Any?> = mapOf(
        "enable_root_access" to enableRootAccess,
        "is_black_list" to isBlackList,
        "list" to list
    )


    private val application by lazy { HookEntry.BreenoApplication }

    private var chat = Chat(
        initialApiKey,
        initialModel,
        initialPrompt,
        initialTools
    )

    private var chatJob: Job? = null

    init {
        newRoom()
        uIInterceptor.setOnIDsReadyStateChangedListener { isReady ->
            if (isReady) {
                breenoSemaphore.enable()
            }
        }
        uIInterceptor.setOnNewRoomListener {
            newRoom()
        }
    }

    fun preConnect() = chat.preConnect()

    fun newRoom() {
        chat.clear()
    }

    fun startHookLLMResponse(p: PackageParam) {
        uIInterceptor.startHookLLMResponse(p)
    }

    fun updateChatConfig(block: ChatConfigBuilder.() -> Unit) {
        chat.updateConfig(block)
    }

    fun updateOtherChatPrefs(block: OtherChatPrefsBuilder.() -> Unit) {
        otherChatPrefsHolder.update(block)
    }

    fun chat(query: String) {
        if (fallback.isNotBlank() && query.contains(fallback)) {
            logD("正在使用小布: { query: $query, fallback: $fallback }")
            uIInterceptor.shouldBlock = false
            chatJob?.cancel()
            return
        }

        logD("正在使用自定义大模型: { query: $query, fallback: $fallback }")
        uIInterceptor.shouldBlock = true
        chatJob?.cancel()
        val filtered = query.replaceFirst("^小布小布，?".toRegex(), "")
        chatJob = scope.launch {
            breenoSemaphore.withPermit {
                logD("chat 取得锁")
                handleChat(query, Message.User(filtered))
            }
        }
    }

    private suspend fun execute(toolCall: ToolCall): Message.Tool {
        val extras = getExtras()
        val result = ToolManager.executeFunction(toolCall, application, null, extras)
        return Message.Tool(toolCall.id!!, toolCall.function!!.name!!, result)
    }

    /**
     * 一收到函数调用就开始异步执行，不用等到最后一起来，这样响应更快
     */
    private suspend fun deferredExecute(toolCall: ToolCall): Deferred<Message.Tool> {
        return CompletableDeferred<Message.Tool>().apply {
            complete(execute(toolCall))
        }
    }

    private fun String.chunkAndInject() {
        // 官方 api 每次控制显示 50 个字，安全起见我也这样搞 TODO 但是用快的模型经常渲染跟不上实际输出，还没研究过如何解决
        chunked(50).forEach { chuckedText ->
            uIInterceptor.injectStreamFrame(chuckedText)
        }
    }

    private suspend fun handleChat(query: String, vararg message: Message) {
        val toolCalls: MutableList<ToolCall> = mutableListOf()
        val toolResult: MutableList<Deferred<Message.Tool>> = mutableListOf()
        val sb = StringBuilder()

        val start = System.currentTimeMillis()
        chat.sendMessage(*message)
            .catch { t ->
                val errorString = "错误:\n${t.message}"

                logRelease("发生异常，终止流: $errorString", t)
                try {
                    uIInterceptor.apply {
                        errorString.chunkAndInject()
                        injectLastStreamFrame(query)
                    }
                } catch (t: Throwable) {
                    logRelease("注入终止流 UI 失败", t)
                }
            }
            .collect { data ->
                when (data) {
                    ChatEvent.Started -> {
                        logV("流开始")
//                    breenoCtrl.injectStreamFrame("") // 这么搞感觉有点卡
                    }

                    is ChatEvent.Content -> {
                        if (sb.isEmpty()) {
                            logE("请求发起到首字输出用时: ${System.currentTimeMillis() - start}ms")
                        }
                        logV("注入数据帧: ${data.content}")
                        data.content.chunkAndInject()
                        sb.append(data.content)
                    }

                    is ChatEvent.ToolCallIntent -> {
                        // 立即异步调用解析到的工具调用
                        val deferred = deferredExecute(data.toolCall)

                        logD("show tool calling: $showToolCalling")
                        if (showToolCalling) {
                            data.toolCall.function?.name?.let {
                                if (sb.isNotBlank())
                                    "\n".chunkAndInject()
                                "`{tool: $it}`\n".chunkAndInject()
                            }
                        }
                        toolResult.add(deferred)
                        toolCalls.add(data.toolCall)
                    }

                    is ChatEvent.Completed -> {
                        // 1. 检查是否有异常，有则在 UI 显示然后终止 flow
                        data.throwable?.apply {
                            val errorString = "错误:\n${this.message}"

                            logRelease("发生异常，终止流: $errorString", this)
                            try {
                                uIInterceptor.apply {
                                    errorString.chunkAndInject()
                                    injectLastStreamFrame(query)
                                }
                            } catch (t: Throwable) {
                                logRelease("注入终止流 UI 失败", t)
                            }
                            return@collect
                        }

                        // 2. 收集数据然后添加成 assistant 消息
                        val content = sb.toString() // 为 null 有的 api 会报错
                        val calls = toolCalls.getOrNull()

                        val assistantMessage = Message.Assistant(content, calls)
                        chat.append(assistantMessage)

                        // 3. 在需要的时候处理函数调用
                        if (toolCalls.isNotEmpty()) {
                            val r = toolResult.awaitAll()
                            handleChat(query, *(r.toTypedArray()))
                        } else {
                            uIInterceptor.injectLastStreamFrame(query)
                        }
                    }
                }
            }
    }


    // 要么有数据要么 null
    private fun StringBuilder.getOrNull(): String? {
        return if (isEmpty()) {
            null
        } else {
            toString()
        }
    }

    private fun <T> List<T>.getOrNull(): List<T>? {
        return if (isEmpty()) {
            null
        } else {
            toList()
        }
    }
}