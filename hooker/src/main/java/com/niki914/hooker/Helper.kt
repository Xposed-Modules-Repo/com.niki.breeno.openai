package com.niki914.hooker

import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.core.logE
import com.niki914.core.logV
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

inline fun <reified T> Any.getField(fieldName: String): T? {
    return XposedHelpers.getObjectField(
        this,
        fieldName
    ) as? T
}

inline fun <reified T> Any.call(methodName: String, vararg params: Any?): T? {
    return XposedHelpers.callMethod(
        this,
        methodName,
        *params
    ) as? T
}

// 总是那么写代码太长了
fun XC_LoadPackage.LoadPackageParam.getClass(name: String): Class<*> {
    return XposedHelpers.findClass(name, this.classLoader)
}

fun XC_LoadPackage.LoadPackageParam.findAndHookConstructor(
    className: String,
    vararg params: Any?,
    beforeCalled: (param: XC_MethodHook.MethodHookParam?) -> Unit = {},
    afterCalled: (param: XC_MethodHook.MethodHookParam?) -> Unit = {}
) {
    try {
        XposedHelpers.findAndHookConstructor(
            getClass(className),
            *params,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    try {
                        beforeCalled(param)
                    } catch (t: Throwable) {
                        logE("回调 before hooked 出错", t)
                    }
                }

                override fun afterHookedMethod(param: MethodHookParam?) {
                    try {
                        afterCalled(param)
                    } catch (t: Throwable) {
                        logE("回调 after hooked 出错", t)
                    }
                }
            }
        )
    } catch (t: Throwable) {
        logE("HookConstructor 失败", t)
    }
}

fun XC_LoadPackage.LoadPackageParam.findAndHookMethod(
    className: String,
    methodName: String,
    vararg params: Any?,
    beforeCalled: (param: XC_MethodHook.MethodHookParam?) -> Unit = {},
    afterCalled: (param: XC_MethodHook.MethodHookParam?) -> Unit = {}
) {
    try {
        XposedHelpers.findAndHookMethod(
            getClass(className),
            methodName,
            *params,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    try {
                        beforeCalled(param)
                    } catch (t: Throwable) {
                        logE("回调 before hooked 出错", t)
                    }
                }

                override fun afterHookedMethod(param: MethodHookParam?) {
                    try {
                        afterCalled(param)
                    } catch (t: Throwable) {
                        logE("回调 after hooked 出错", t)
                    }
                }
            }
        )
    } catch (t: Throwable) {
        logE("HookMethod 失败", t)
    }
}

fun XC_LoadPackage.LoadPackageParam.findAndHookMethod(
    className: String,
    methodName: String,
    vararg params: Any?,
    replacement: (param: XC_MethodHook.MethodHookParam?) -> Any? = {},
) {
    try {
        XposedHelpers.findAndHookMethod(
            getClass(className),
            methodName,
            *params,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                    return replacement(param)
                }
            }
        )
    } catch (t: Throwable) {
        logE("HookMethod 失败", t)
    }
}

abstract class BaseHooker<T, R> {

    open val TAG: String = this::class.simpleName.toString()

    fun hookWith(params: PackageParam, callback: (T) -> R) {
        tryInternal { params.hookInternal(callback) }
    }

    private fun tryInternal(action: () -> Unit) {
        try {
            logV("Hook $TAG 开始")
            action()
        } catch (t: Throwable) {
            logE("Hook $TAG 失败", t)
        } finally {
            logV("Hook $TAG 完成")
        }
    }

    protected open fun PackageParam.hookInternal(callback: (T) -> R) {}
}