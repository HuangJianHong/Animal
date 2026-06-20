package com.example.animal.chat.entity

import com.google.gson.annotations.SerializedName

/**
 * SSE 流式分段返回解析实体。
 *
 * 火山方舟 responses 接口的 SSE 每个 `data:` 行是一个 JSON 事件，典型形如：
 * ```
 * data: {"type":"response.output_text.delta","delta":"你"}
 * data: {"type":"response.output_text.delta","delta":"好"}
 * data: {"type":"response.completed", ...}
 * ```
 * 本实体用于提取增量回复文本（[incrementalText]）与判断流结束标记（[isStreamEnd]）。
 *
 * @param type  事件类型
 * @param delta 增量文本（output_text.delta 事件携带）
 * @param text  完整文本（部分事件直接携带全量文本，作为兜底）
 */
data class SseStreamResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("delta") val delta: String? = null,
    @SerializedName("text") val text: String? = null
) {

    /**
     * 提取本段增量文本：优先 delta，其次 text，没有则返回空串。
     */
    fun incrementalText(): String = when {
        !delta.isNullOrEmpty() -> delta
        type == TYPE_TEXT_DELTA && !text.isNullOrEmpty() -> text
        else -> ""
    }

    /**
     * 是否为流结束标记。
     */
    fun isStreamEnd(): Boolean = type == TYPE_COMPLETED || type == TYPE_ERROR

    companion object {
        /** 增量文本事件 */
        const val TYPE_TEXT_DELTA = "response.output_text.delta"

        /** 整体完成事件（结束标记） */
        const val TYPE_COMPLETED = "response.completed"

        /** 错误事件 */
        const val TYPE_ERROR = "error"

        /** 原始 SSE 文本流结束标记 */
        const val DONE_FLAG = "[DONE]"
    }
}
