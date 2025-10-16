package com.niki914.chat.inner

import com.google.gson.Gson
import com.niki914.chat.beans.ChatEvent
import com.niki914.chat.beans.FunctionCall
import com.niki914.chat.beans.ToolCall
import kotlinx.coroutines.flow.FlowCollector

/**
 * 封装工具调用处理逻辑的类。因为 openai 的请求喜欢把函数调用拆开来传，非常麻烦
 * 负责累积流式传输的工具调用片段，并提供已完成的工具调用意图。
 *
 * ai 写的
 *
 * 假设：
 * 1. function.name 总会在该 ToolCall 的第一个 delta 中完整出现。
 * 2. 同一个流中，不会出现在一个工具调用（Tool Call）的参数还没有完全接收完的时候，又开始一个新的工具调用。
 * 3. 当 ToolCallDelta 的 ID 为空时，它属于当前正在处理的 ToolCall。
 * 4. 就算是无参的调用也至少会构成 {} 空 json 体
 */
internal class ToolCallHandler private constructor(private val flowCollector: FlowCollector<ChatEvent>) { // 构造函数现在接收 FlowCollector

    companion object {
        fun bindTo(flowCollector: FlowCollector<ChatEvent>) = ToolCallHandler(flowCollector)
    }

    // 存储当前正在进行的工具调用及其累积信息
    // 由于 ID 可能为空，我们只维护一个当前活跃的 ToolCallAccumulator
    private var currentActiveToolCall: ToolCallAccumulator? = null
    private val gson = Gson()

    /**
     * 内部数据类，用于存储一个工具调用的所有累积信息。
     * @param initialToolCall 首次接收到的包含 id, type, name 等信息的 ToolCall 对象。
     * @param argumentsBuilder 用于累积 arguments 字符串的 StringBuilder。
     */
    private data class ToolCallAccumulator(
        val initialToolCall: ToolCall,
        val argumentsBuilder: StringBuilder
    )

    /**
     * 处理来自流的单个 ToolCall 片段。
     *
     * @param toolCallDelta 从 Delta 中解析出的 ToolCall 对象。
     */
    suspend fun handle(toolCallDelta: ToolCall) { // 修改为 suspend 函数，因为 emit 是 suspend 函数
//        try {
        val callId: String? = toolCallDelta.id
        val argumentsChunk: String? = toolCallDelta.function?.arguments

        if (callId != null) {
            // 如果存在 ID，意味着一个新的工具调用开始
            // 此时，无论之前是否有未完成的工具调用，都将其“完成”或舍弃。
            // 确保在开始新的工具调用之前，尝试完成并发出当前的工具调用（如果存在且未完成）
            // 这是为了处理假设3：就算是无参的调用也至少会构成 {} 空 json 体，
            // 并且防止因为流的结束而导致最后一个ToolCall没有被完整处理。
            currentActiveToolCall = ToolCallAccumulator(toolCallDelta, StringBuilder())
        }

        // 统一处理参数追加逻辑，无论 ID 是否为空
        // 关键：只在这里追加一次 argumentsChunk
        argumentsChunk?.let {
            currentActiveToolCall?.argumentsBuilder?.append(it)
        }

        // 每次收到 chunk 后，都尝试检查是否可以完成并发送工具调用
        checkAndEmitCompletedToolCall()
//        } catch (t: Throwable) {
//            logRelease("ToolCallHandler#handle 异常", t)
//            clear()
//        }
    }

    /**
     * 检查当前活跃的工具调用是否已完成解析，如果完成则通过 FlowCollector 发出。
     *
     * 每次收到新的 arguments chunk 或流结束时都应调用此方法。
     */
    private suspend fun checkAndEmitCompletedToolCall() {
        currentActiveToolCall?.let { accumulator ->
            val fullArguments = accumulator.argumentsBuilder.toString()
            // 只有当 fullArguments 看起来像一个完整的 JSON 对象时才尝试解析
            // 简单的判断，实际可能需要更严格的 JSON 校验
            if (fullArguments.isNotEmpty() &&
                (fullArguments.startsWith("{") && fullArguments.endsWith("}"))
            ) {
//                try {
                gson.fromJson(fullArguments, Map::class.java) // 尝试解析完整的 JSON

                // 如果解析成功，则创建一个新的 ToolCall 对象，其 arguments 字段为完整的 JSON 字符串
                val completedToolCall = ToolCall(
                    id = accumulator.initialToolCall.id,
                    type = accumulator.initialToolCall.type,
                    function = FunctionCall(
                        name = accumulator.initialToolCall.function?.name,
                        arguments = fullArguments
                    )
                )
                flowCollector.emit(ChatEvent.ToolCallIntent(completedToolCall))

                // 成功解析并发送后，清除当前活跃的工具调用，等待下一个
                clear()
//                } catch (t: Throwable) {
//                    // 如果不是完整的 JSON，或者解析出错，则不清除 currentActiveToolCall，
//                    // 等待后续的 chunk。
//                    // 只有当出现严重错误，且确定当前 ToolCall 无法恢复时才清除。
//                    // 这里我们假设流会给出完整的 JSON。
//                    // 如果流中断，后续的处理会在 forceCompleteCurrentToolCall() 或流结束时进行。
//                }
            }
        }
    }

    private fun clear() {
        currentActiveToolCall = null
    }
}