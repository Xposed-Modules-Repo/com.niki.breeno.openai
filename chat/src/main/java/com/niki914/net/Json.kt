package com.niki914.net

@Deprecated("")
class Json

//private val gson by lazy { Gson() }
//
//private val prettyGson by lazy {
//    GsonBuilder().setPrettyPrinting().create()
//}
//
//
//fun Any?.toPrettyJson(): String {
//    val result = if (this is CharSequence) {
//        prettyGson.toJson(this.toJsonElement())
//    } else
//        prettyGson.toJson(this)
//    return if (result == "null") "" else result
//}
//
//fun Any?.toJson(): String {
//    val result = gson.toJson(this)
//    return if (result == "null") "" else result
//}
//
//fun CharSequence.toJsonElement(): JsonElement? = try {
//    JsonParser.parseString(this.toString())
//} catch (_: Throwable) {
//    null
//}
//
//inline fun <reified T> String.toJsonClass(): T? {
//    return try {
//        Gson().fromJson(this, T::class.java)
//    } catch (_: Exception) {
//        null
//    }
//}