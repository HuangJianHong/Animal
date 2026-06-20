package com.example.animal.chat.entity

import com.google.gson.annotations.SerializedName

/**
 * SSE 流式分段返回解析实体。
 *
 * 火山方舟 responses 接口的 SSE 每个 `data:` 行是一个 JSON 事件，典型形如：
 * ```
 * data: {"type":"response.reasoning_summary_text.delta","delta":"用户在问..."}  // 思考过程，需丢弃
 * data: {"type":"response.output_text.delta","delta":"你"}                       // 正文增量
 * data: {"type":"response.output_text.delta","delta":"好"}
 * data: {"type":"response.output_text.done","text":"你好！..."}
 * data: {"type":"response.completed", ...}
 * data: [DONE]
 * ```
 * 联调结论：deepseek 模型会先输出 `reasoning_summary_text.delta`（思考过程，同样带 delta 字段），
 * 必须按 [type] 精确匹配 `response.output_text.delta` 才下发，否则会把思考过程当成正文渲染。
 *
 * 本实体用于提取增量回复文本（[incrementalText]）与判断流结束标记（[isStreamEnd]）。
 *
 * @param type  事件类型
 * @param delta 增量文本（多种 *.delta 事件都会携带，需结合 type 区分）
 * @param text  完整文本（*.done 事件直接携带全量文本）
 */
data class SseStreamResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("delta") val delta: String? = null,
    @SerializedName("text") val text: String? = null
) {

    /**
     * 提取本段正文增量文本：仅 `response.output_text.delta` 事件的 delta 才视为正文。
     * 其余事件（思考过程、done 全量、其它）一律返回空串，避免重复或污染正文。
     */
    fun incrementalText(): String =
        if (type == TYPE_TEXT_DELTA && !delta.isNullOrEmpty()) delta else ""

    /**
     * 是否为流结束标记。
     */
    fun isStreamEnd(): Boolean = type == TYPE_COMPLETED || type == TYPE_ERROR

    companion object {
        /** 正文增量文本事件 */
        const val TYPE_TEXT_DELTA = "response.output_text.delta"

        /** 整体完成事件（结束标记） */
        const val TYPE_COMPLETED = "response.completed"

        /** 错误事件 */
        const val TYPE_ERROR = "error"

        /** 原始 SSE 文本流结束标记 */
        const val DONE_FLAG = "[DONE]"
    }
}
