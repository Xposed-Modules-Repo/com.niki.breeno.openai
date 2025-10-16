package com.niki914.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.random.Random
import kotlin.reflect.KClass

inline fun <reified A : Activity> Activity.startActivity(intentPreference: Intent.() -> Unit = {}) {
    val i = Intent(this, A::class.java)
    i.intentPreference()
    startActivity(i)
}

/**
 * 伪造 json 中用到的 32 位随机
 */
fun generateRandomHexString(length: Int): String {
    val chars = "0123456789abcdef"
    return (1..length)
        .map { chars.random(Random) }
        .joinToString("")
}

fun Context.toast(msg: String) = CoroutineScope(Dispatchers.Main).launch {
    Toast.makeText(this@toast, msg, Toast.LENGTH_SHORT).show()
}

suspend fun <R> count(tag: String, block: suspend () -> R): R {
    val startTime = System.currentTimeMillis()
    val r = block()
    logV("$tag 用时 ${System.currentTimeMillis() - startTime}ms")
    return r
}

fun Pair<String?, Int?>.proxyToString(): String {
    if (first.isNullOrBlank() || second == null) {
        return ""
    }
    return "$first:$second".trim(' ', '\n')
}

fun String.parseToProxyPair(): Pair<String?, Int?> {
    val proxyString = this

    val parts = proxyString.split(":")

    if (parts.size == 2) {
        val host = parts[0].trim() // 获取主机部分并去除空白
        val port = parts[1].trim().toIntOrNull() // 获取端口部分并尝试转换为Int，如果失败则为null

        // 检查主机是否为空，且端口是否成功解析
        if (host.isNotBlank() && port != null) {
            return host to port
        }
    }

    // 如果解析失败（例如格式不正确，或端口不是数字），返回null
    return null to null
}

inline fun <reified T : Any> getSealedChildren(
    noinline filter: (KClass<out T>) -> T?
): List<T> {
    return getSealedChildren(T::class, filter)
}

fun <T : Any> getSealedChildren(
    sealedClass: KClass<T>,
    filter: (KClass<out T>) -> T?
): List<T> {
    require(sealedClass.isSealed) { "传入的参数必须是封装类" }

    // 递归收集所有子类的函数
    fun collectSubclasses(kClass: KClass<out T>): List<KClass<out T>> {
        return if (kClass.isSealed) {
            // 如果是密封类, 递归收集其直接子类的子类
            kClass.sealedSubclasses.flatMap { collectSubclasses(it) }
        } else {
            // 如果不是密封类, 直接返回自身
            listOf(kClass)
        }
    }

    // 获取所有子类 (包括间接子类), 然后应用 filter
    return collectSubclasses(sealedClass).mapNotNull(filter)
}

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false // 根据需要调整，决定输出的 JSON 字符串是否带格式
    encodeDefaults = true // 编码时是否包含默认值
}

fun JsonObject.parseToString(): String {
    return json.encodeToString(this)
}

fun String.parseToJsonObj(): JsonObject? {
    // 直接解析为 JsonElement
    return try {
        json.parseToJsonElement(this).jsonObject
    } catch (_: Throwable) {
        null
    }
}

inline fun <reified T> JsonObject.getAs(key: String): T? {
    val p = get(key)?.jsonPrimitive ?: return null
    logD("getAs: ${T::class}")
    val data =
        when (T::class) { // 踩过一个坑，when 上面写的是 class.java 下面也要 class.java，不然 kotlin.xx 和 java.lang.xx 匹配不了
            String::class -> {
                if (p.isString) {
                    p.contentOrNull
                } else {
                    p.toString()
                }
            }

            Int::class -> p.intOrNull
            Long::class -> p.longOrNull
            Float::class -> p.floatOrNull
            Double::class -> p.doubleOrNull
            Boolean::class -> p.booleanOrNull
            else -> {
                logE("不支持的类型: ${T::class}, key: $key, content: ${p.content}")
                null
            }
        }
    return data as? T
}