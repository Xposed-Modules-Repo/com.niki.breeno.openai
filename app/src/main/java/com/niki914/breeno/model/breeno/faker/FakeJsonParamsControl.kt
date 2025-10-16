package com.niki914.breeno.model.breeno.faker

import com.niki914.breeno.model.breeno.faker.json.LastStreamFrameJson
import com.niki914.breeno.model.breeno.faker.json.StreamFrameJson
import com.niki914.core.logV

/**
 * 维护一轮对话必须的属性，这些属性是由小布助手分配（可能是云端下发？），如果不使用这些属性就不能成功欺骗 UI
 */
class FakeJsonParamsControl {
    var sessionId: String? = null
        private set
    var recordId: String? = null
        private set
    var originalRecordId: String? = null
        private set
    var roomId: String? = null
        private set
    var sequence: Int = -1
        private set

    private var timestamp: Long? = null // 官方的逻辑是，除了第一个以外其他的帧的时间戳都相同

    private var roomListener: (() -> Unit)? = null

    /**
     * 监听以适应对话记录
     */
    fun setOnNewRoomListener(l: (() -> Unit)?) {
        roomListener = l
    }

    val isAvailable: Boolean
        get() = (sessionId != null &&
                recordId != null &&
                originalRecordId != null &&
                roomId != null)

    fun setFinished() {
        sessionId = null
        recordId = null
        originalRecordId = null
        sequence = 0
    }

    fun refresh(
        sessionId: String,
        recordId: String,
        originalRecordId: String,
        roomId: String
    ) {
        this.sessionId = sessionId
        this.recordId = recordId
        this.originalRecordId = originalRecordId
        if (this.roomId != roomId) {
            roomListener?.invoke()
        }
        this.roomId = roomId
        this.sequence = 0
    }

    fun getLastStreamFrame(
        query: String
    ): String? {
        LastStreamFrameJson.get(
            query,
            sessionId ?: return null,
            recordId ?: return null,
            originalRecordId ?: return null,
            roomId ?: return null,
            sequence,
            timestamp ?: return null
        ).let {
            logV("获取末帧[sequence=$sequence]") // 检查是否有错误
            setFinished()
            return it
        }
    }

    fun getStreamFrame(
        content: String
    ): String? {
        var isFirst = false
        if (sequence == 0) {
            isFirst = true
            timestamp = System.currentTimeMillis()
        }

        StreamFrameJson.get(
            content,
            isFirst,
            sessionId ?: return null,
            recordId ?: return null,
            originalRecordId ?: return null,
            roomId ?: return null,
            sequence,
            timestamp ?: return null
        ).let {
            logV("获取对话帧[sequence=$sequence]")
            sequence++
            return it
        }
    }
}