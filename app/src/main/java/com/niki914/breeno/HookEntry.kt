package com.niki914.breeno

import android.app.Application
import android.content.Context
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.niki914.breeno.model.llm.XChatManager
import com.niki914.breeno.provider.messaging.MessagingProvider
import com.niki914.breeno.repository.XSettingsRepository
import com.niki914.core.BuildConfig
import com.niki914.core.ToolsNames
import com.niki914.core.logD
import com.niki914.core.logE
import com.niki914.core.logRelease
import com.niki914.hooker.adaption.ChatRVAdapterHooker
import com.niki914.tool.call.ToolManager
import com.niki914.tool.call.utils.AppInfoCache
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {

    companion object {
        /**
         * 使用 BuildConfig，启用混淆后这些调试属性会自动优化掉，不会泄露
         */
        private object Debug {
            val isDebug = BuildConfig.DEBUG

            val key: String
                get() {
                    if (!isDebug) throw SecurityException()
                    return TODO() // "sk-xxx"
                }

            val url: String
                get() {
                    if (!isDebug) throw SecurityException()
                    return TODO() // "https://xxx.api"
                }

            val model: String
                get() {
                    if (!isDebug) throw SecurityException()
                    return TODO() // "gemini-2.5-pro"
                }
        }

        const val BREENO_PACKAGE_NAME = "com.heytap.speechassist"

        val LATEST_SUPPORTED = listOf(
            "11.8.6",
            "11.9.1",
            "11.9.2"
        )

        private var lastQuery: String? = null
        private var lastCallTime: Long = 0L

        // 防抖动时间。在启动了小布主应用进程后，再用通过电源键唤起小布，这时由于用户输入捕获的实现是通过 UI，
        // 并且由于 recyclerview 实例化了两个，会导致重复回调，需要防抖
        private const val DEBOUNCE_DELAY = 150L

        lateinit var BreenoApplication: Application

        lateinit var versionName: String
    }

    private val repo by lazy { XSettingsRepository.getInstance() }

    private val supervisorContext = SupervisorJob()

    private val hookScope = CoroutineScope(supervisorContext)

    private val exceptionHandler = CoroutineExceptionHandler { context, exception ->
        logRelease(exception.message ?: "未知错误", exception)
        exception.trySendErrorNotification(true) // 在这里发送错误通知
    }

    private lateinit var chatManager: XChatManager

    @Volatile
    private var isLoadingAppInfos = false


    override fun onInit() = configs {
        isDebug = BuildConfig.DEBUG
        isEnableModuleAppResourcesCache = true
//        isEnableHookSharedPreferences = true

        debugLog {
            tag = "NIKI"
            isEnable = BuildConfig.DEBUG
        }
    }

    override fun onHook() = encase {
        loadApp(name = BREENO_PACKAGE_NAME) {
            onAppLifecycle(isOnFailureThrowToApp = false) {
                onCreate {
                    BreenoApplication = this

                    if (Debug.isDebug) {
                        Throwable("TestMessage").trySendErrorNotification(true)
                    }

                    try {
                        versionName = getVersionName(this) ?: ""
                        logRelease("小布版本: $versionName")

                        initializeAppInfoCache(this)
                        XSettingsRepository.getInstance(this)

                        chatManager = XChatManager(hookScope)
                        refreshConfig()
                        chatManager.preConnect() // url 配置好后立即预连接

                        hookInput()
                        hookOutput()
                    } catch (t: Throwable) {
                        logE("主 hook 失败", t)
                        t.trySendErrorNotification(true)
                    }
                }
            }
        }
    }

    private fun Throwable.trySendErrorNotification(shouldStartActivity: Boolean = true) {
        notify(
            "模块出错: $message",
            stackTraceToString(),
            shouldStartActivity
        )
    }

    private fun notify(title: String, content: String, shouldStartActivity: Boolean = true) {
        runCatching {
            MessagingProvider.sendNotificationWithProvider(
                BreenoApplication,
                title,
                content,
                shouldStartActivity
            )
        }
    }

    /**
     * 预初始化应用信息缓存
     * 在后台异步初始化，不阻塞主要流程
     */
    private fun initializeAppInfoCache(application: Application) {
        if (isLoadingAppInfos) return
        isLoadingAppInfos = true
        hookScope.launch(exceptionHandler) {
            try {
                logD("开始预初始化应用信息缓存...")
                val cache = AppInfoCache.getInstance(application)
                cache.getAllAppInfo()
                logD("应用信息缓存预初始化完成")
            } catch (e: Exception) {
                logE("预初始化应用信息缓存失败", e)
            }
            isLoadingAppInfos = false
        }
    }

    /**
     * 根据配置项设置来筛除工具
     */
    private fun filterTool(toolName: String): Boolean = when (toolName) {
        ToolsNames.LAUNCH_APP -> repo.getEnableApp()
        ToolsNames.LAUNCH_URI -> repo.getEnableUri()
        ToolsNames.GET_DEVICE_INFO -> repo.getEnableGetDeviceInfo()
        ToolsNames.SHELL -> repo.getEnableShellCmd()
        else -> false
    }

    private fun PackageParam.hookInput() {
        ChatRVAdapterHooker(versionName).hookWith(this) { userQuery ->
            val currentTime = System.currentTimeMillis()
            if (userQuery == lastQuery && currentTime - lastCallTime < DEBOUNCE_DELAY) {
                logD("忽略抖动: $userQuery[${currentTime - lastCallTime}]")
                lastCallTime = currentTime
                return@hookWith
            }

            lastQuery = userQuery
            lastCallTime = currentTime
            logD("捕获输入: $userQuery")
            hookScope.launch(exceptionHandler) {
                chat(userQuery)
            }
        }
    }

    private fun PackageParam.hookOutput() {
        chatManager.startHookLLMResponse(this)
    }

    private suspend fun chat(query: String) {
        refreshConfig()
        setChatManagerConfigs()
        chatManager.chat(query)
    }

    private fun setChatManagerConfigs() {
        chatManager.updateOtherChatPrefs {
            fallback = repo.getFallbackToBreeno()
            showToolCalling = repo.getEnableShowToolCalling()

            enableRootAccess = repo.getEnableRootAccess()
            isBlackList = repo.getIsBlackList()
            list = repo.getKeywords()
        }
    }

    private fun refreshConfig() {
        if (Debug.isDebug) {
            setWithDebugConfig()
        } else {
            setWithLocalConfig()
        }
    }

    private fun setWithLocalConfig() {
        chatManager.updateChatConfig {
            apiKey = repo.getAPIKey()
            modelName = repo.getModelName()
            prompt = repo.getSystemPrompt()

            val proxy = repo.getProxy()

            val t = ToolManager.getTools(::filterTool)
            if (t.isNotEmpty()) {
                logD("可用工具: $t")
                tools = t
            }
            network {
                baseUrl = repo.getUrl()
                connectTimeout = repo.getTimeout()
                if (proxy.first != null && proxy.second != null)
                    socksProxy(proxy.first!!, proxy.second!!)
            }

            logRelease("配置已刷新: " + this@updateChatConfig.toString())
        }
    }

    private fun setWithDebugConfig() {
        val proxy = repo.getProxy()

        chatManager.updateChatConfig {
            apiKey = Debug.key
            modelName = Debug.model
            prompt = repo.getSystemPrompt()

            tools = ToolManager.getTools() // 全部开放
            network {
                baseUrl = Debug.url
                connectTimeout = repo.getTimeout()
                if (proxy.first != null && proxy.second != null)
                    socksProxy(proxy.first!!, proxy.second!!)
            }
        }
    }

    /**
     * 获取自己应用内部的版本名
     */
    private fun getVersionName(context: Context): String? {
        val manager = context.packageManager
        var name: String? = null
        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            name = info.versionName
            if (name !in LATEST_SUPPORTED) {
                logRelease("未适配的小布助手版本")
            }
        } catch (e: Exception) {
            logE("版本号获取失败", e)
        }

        return name
    }
}