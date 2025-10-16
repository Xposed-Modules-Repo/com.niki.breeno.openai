package com.niki914.tool.call.utils

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.niki914.core.logD
import com.niki914.core.logE
import com.niki914.core.logV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.text.similarity.LevenshteinDistance
import java.util.concurrent.ConcurrentHashMap

/**
 * 应用信息缓存管理类
 * 使用deferred异步获取应用信息，避免阻塞主线程
 * 优化后，所有获取数据的方法都会确保缓存已初始化并就绪。
 *
 * ai 写的
 *
 * @param application 用于获取PackageManager的Application上下文，在第一次调用getInstance时提供
 */
class AppInfoCache private constructor(private val application: Application) {

    // 伴生对象，用于实现单例模式和初始化逻辑
    companion object {
        @Volatile
        private var instance: AppInfoCache? = null

        // 初始化任务的Deferred，标记缓存是否已准备好。
        // 使用一个Job来表示初始化过程，一旦完成，后续await会立即返回。
        @Volatile
        private var initializeJob: Deferred<Boolean>? = null

        /**
         * 获取AppInfoCache的单例实例。
         * 首次调用时需要传入Application上下文以完成初始化。
         * 后续调用可以直接获取实例，无需再次传入Application。
         */
        fun getInstance(application: Application): AppInfoCache {
            // 双重检查锁定，确保线程安全地创建单例
            return instance ?: synchronized(this) {
                instance ?: AppInfoCache(application).also {
                    instance = it
                    // 在创建实例时，立即启动初始化过程
                    // 这里使用 runBlocking 是为了在单例创建时同步等待初始化任务的启动，
                    // 确保 initializeJob 被赋值，但实际的await发生在数据访问方法中。
                    // 确保 initializeJob 在单例返回之前被设置，以便后续调用能够await。
                    // 真正的 await 发生在 ensureInitialized 内部
                    runBlocking {
                        ensureInitializedInternal(application)
                    }
                }
            }
        }

        /**
         * 如果AppInfoCache实例已经存在，可以直接获取。
         * 如果从未调用过带Application参数的getInstance，此方法会抛出异常。
         */
        fun getInstance(): AppInfoCache {
            return instance
                ?: throw IllegalStateException("AppInfoCache must be initialized with Application context first by calling getInstance(application).")
        }


        // 标记缓存是否已完成初始化
        @Volatile
        private var _isInitialized = false

        /**
         * 内部方法：确保缓存已初始化并就绪。
         * 如果未初始化或正在初始化，会等待其完成。
         * 线程安全且幂等。
         */
        private suspend fun ensureInitializedInternal(application: Application) {
            // 快速路径：如果已初始化，立即返回
            if (_isInitialized) {
                return
            }

            // 如果没有正在进行的初始化任务，或之前的任务已完成，则启动新任务
            if (initializeJob == null || initializeJob?.isCompleted == true) {
                synchronized(this) { // 再次同步，防止多个线程同时进入
                    if (initializeJob == null || initializeJob?.isCompleted == true) {
                        logV("应用信息缓存未就绪，启动初始化...")
                        // 使用 Dispatchers.IO 启动异步任务
                        initializeJob = CoroutineScope(Dispatchers.IO).async {
                            try {
                                logD("开始异步获取应用信息...")
                                val startTime = System.currentTimeMillis()

                                val packageManager = application.packageManager
                                // 建议在实际应用中使用 GET_UNINSTALLED_PACKAGES 或 PackageManager.MATCH_UNINSTALLED_PACKAGES
                                // 以获取所有应用，包括那些被用户禁用或卸载了但数据仍然保留的应用。
                                // 这里为了简化，沿用你的 GET_META_DATA。
                                val installedApps =
                                    packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

                                // 清空旧缓存
                                instance?.appInfoCache?.clear() // 使用 instance?. 确保安全调用
                                instance?.nameToPackageMap?.clear()

                                // 构建缓存
                                installedApps.forEach { appInfo ->
                                    try {
                                        val packageName = appInfo.packageName
                                        val appName =
                                            packageManager.getApplicationLabel(appInfo).toString()
                                        val isSystemApp =
                                            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                                        val appInfoObj = AppInfo(
                                            packageName = packageName,
                                            appName = appName,
                                            isSystemApp = isSystemApp
                                        )

                                        instance?.appInfoCache?.put(packageName, appInfoObj)
                                        instance?.nameToPackageMap?.put(
                                            appName.lowercase(),
                                            packageName
                                        )
                                    } catch (e: Exception) {
                                        logE("处理应用信息失败: ${appInfo.packageName}", e)
                                    }
                                }

                                _isInitialized = true // 标记初始化完成
                                val endTime = System.currentTimeMillis()
                                logD("应用信息缓存初始化完成，耗时: ${endTime - startTime}ms，共${instance?.appInfoCache?.size}个应用")
                                true // 成功返回true
                            } catch (e: Exception) {
                                logE("初始化应用信息缓存失败", e)
                                _isInitialized = false // 标记初始化失败
                                false // 失败返回false
                            }
                        }
                    }
                }
            }

            // 等待初始化任务完成
            val success = initializeJob!!.await()
            if (!success) {
                // 如果初始化失败，抛出异常，让外部调用者知道
                throw IllegalStateException("App info cache initialization failed after waiting.")
            }
        }
    }

    sealed class FindAppResult {
        data class Found(val packageName: String) : FindAppResult()
        data object NotFound : FindAppResult()
        data class BestMatches(val packageNames: List<String>) : FindAppResult()
    }

    /**
     * 应用信息数据类
     */
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val isSystemApp: Boolean = false
    )

    // 缓存应用信息的Map
    private val appInfoCache = ConcurrentHashMap<String, AppInfo>()

    // 应用名到包名的映射，支持模糊匹配
    private val nameToPackageMap = ConcurrentHashMap<String, String>()

    /**
     * 根据应用名模糊匹配查找包名。
     * 此方法会自动确保缓存已初始化并就绪，无需外部手动调用初始化。
     *
     * @param appName 应用名（支持模糊匹配）
     * @return 匹配的包名，如果没有找到返回null
     */
    suspend fun findPackageNameByAppName(appName: String): FindAppResult =
        withContext(Dispatchers.IO) {
            // 在所有数据查询方法内部，首先确保缓存已初始化并就绪
            ensureInitializedInternal(application) // 现在传入 Application 实例

            val searchName = appName.lowercase().trim()

            // 1. 精确匹配
            nameToPackageMap[searchName]?.let { packageName ->
                logV("精确匹配找到应用: $appName -> $packageName")
                return@withContext FindAppResult.Found(packageName)
            }

            // 2. 模糊匹配 - 应用名包含搜索词
            val fuzzyMatches = appInfoCache.values.filter { appInfo ->
                appInfo.appName.lowercase().contains(searchName)
            }

            when (fuzzyMatches.size) {
                0 -> {
                    logV("未找到匹配的应用: $appName")
                    return@withContext FindAppResult.NotFound
                }

                1 -> {
                    val match = fuzzyMatches.first()
                    logV("模糊匹配找到应用: $appName -> ${match.packageName}")
                    return@withContext FindAppResult.Found(match.packageName)
//                    return@withContext FindAppResult.BestMatches(listOf("${match.appName}[${match.packageName}]"))
                }

                else -> {
                    val list = fuzzyMatches.map { "${it.appName}[${it.packageName}]" }
                    logV("多个匹配: $list")
                    return@withContext FindAppResult.BestMatches(list)
                }
            }
        }

    /**
     * 根据应用名模糊匹配查找包名。
     * 此方法会自动确保缓存已初始化并就绪，无需外部手动调用初始化。
     *
     * @param appName 应用名（支持模糊匹配）
     * @return 匹配的包名，如果没有找到返回null
     */
    suspend fun findPackageNameByAppNameSmart(appName: String): FindAppResult =
        withContext(Dispatchers.IO) {
            // 在所有数据查询方法内部，首先确保缓存已初始化并就绪
            ensureInitializedInternal(application) // 现在传入 Application 实例

            val searchName = appName.lowercase().trim()

            val key =
                nameToPackageMap.values.firstOrNull { it == searchName }

            // 1. 精确匹配
            key?.let { k ->
                nameToPackageMap[k]?.let { packageName ->
                    logV("精确匹配找到应用: $appName -> $packageName")
                    return@withContext FindAppResult.Found(packageName)
                }
            }

            // 2. 使用编辑距离进行模糊匹配
            logV("精确匹配失败，使用编辑距离进行模糊匹配...")
            val bestMatches = findBestMatchedAppInfosByAppName(appName, 5)

            when (bestMatches.size) {
                0 -> {
                    logV("未找到匹配的应用: $appName")
                    return@withContext FindAppResult.NotFound
                }

                1 -> {
                    val match = bestMatches.first()
                    logV("编辑距离匹配找到应用: $appName -> ${match.packageName}")
                    return@withContext FindAppResult.Found(match.packageName)
                }

                else -> {
                    // 多个匹配时，优先选择非系统应用
                    val userApps = bestMatches.filter { !it.isSystemApp }
                    val selectedApp = if (userApps.isNotEmpty()) {
                        // 如果有非系统应用，优先选择第一个非系统应用
                        userApps.first()
                    } else {
                        // 否则，选择第一个匹配到的应用
                        bestMatches.first()
                    }

                    logV("多个匹配，选择: $appName -> ${selectedApp.packageName}")

                    // 返回候选列表供用户选择
                    val candidates = bestMatches.map { "${it.appName}[${it.packageName}]" }
                    return@withContext FindAppResult.BestMatches(candidates)
                }
            }
        }

    /**
     * 根据应用名查找最佳匹配结果，使用编辑距离算法。
     *
     * @param appName 用户输入的搜索词
     * @param take 返回的最大匹配数量
     * @return 包含AppInfo对象的列表，按相似度排序
     */
    private suspend fun findBestMatchedAppInfosByAppName(
        appName: String,
        take: Int
    ): List<AppInfo> =
        withContext(Dispatchers.IO) {
            ensureInitializedInternal(application)

            val searchName = appName.lowercase().trim()
            val levenshteinDistance = LevenshteinDistance.getDefaultInstance()

            // 使用编辑距离进行匹配，计算相似度
            return@withContext appInfoCache.values
                .mapNotNull { appInfo ->
                    val distance =
                        levenshteinDistance.apply(appInfo.appName.lowercase(), searchName)
                    val maxLength = maxOf(appInfo.appName.length, searchName.length)
                    val similarity = 1.0 - (distance.toDouble() / maxLength)

                    // 过滤掉相似度过低的结果（阈值可调整）
                    if (similarity >= 0.3) {
                        appInfo to similarity
                    } else {
                        null
                    }
                }
                .sortedByDescending { it.second } // 按相似度降序排序，相似度越高越靠前
                .take(take) // 取前N个结果
                .map { it.first } // 返回 AppInfo 对象列表
                .also {
                    logV("通过编辑距离找到 ${it.size} 个匹配项，返回前${take}个。")
                }
        }

    /**
     * 获取所有应用信息。
     * 此方法会自动确保缓存已初始化并就绪，无需外部手动调用初始化。
     */
    suspend fun getAllAppInfo(): List<AppInfo> = withContext(Dispatchers.IO) {
        ensureInitializedInternal(application)
        appInfoCache.values.toList()
    }

    /**
     * 根据包名获取应用信息。
     * 此方法会自动确保缓存已初始化并就绪，无需外部手动调用初始化。
     */
    suspend fun getAppInfoByPackageName(packageName: String): AppInfo? =
        withContext(Dispatchers.IO) {
            ensureInitializedInternal(application)
            appInfoCache[packageName]
        }

    /**
     * 清空缓存。
     * 注意：清空后，下次获取数据时会重新触发初始化。
     */
    fun clearCache() {
        appInfoCache.clear()
        nameToPackageMap.clear()
        _isInitialized = false
        initializeJob = null // 清空Job，允许重新初始化
        logD("AppInfoCache 已清空。")
    }

    /**
     * 获取缓存状态。
     * 外部可以判断缓存是否就绪，但通常不再需要显式检查，因为get方法会自动等待。
     */
    fun isReady(): Boolean = _isInitialized
}