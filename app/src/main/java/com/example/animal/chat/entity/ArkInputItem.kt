package com.example.animal.chat.entity

import com.google.gson.annotations.SerializedName

/**
 * input 数组内单条对话实体。
 *
 * 联调结论（多轮对话）：
 * - user 输入项可使用精简结构 `{role, content[input_text]}`；
 * - assistant 历史项必须补齐 `type:"message"` 与 `status:"completed"`，
 *   否则服务端返回 400：`MissingParameter: input.type / input.status`；
 * - [type]/[status] 为可空字段，user 项传 null，Gson 默认不序列化 null，故不会发送多余字段。
 *
 * @param type    item 类型：assistant 历史固定 "message"，user 传 null
 * @param role    角色："user"（用户/人设）或 "assistant"（AI 历史回复）
 * @param status  item 状态：assistant 历史固定 "completed"，user 传 null
 * @param content 内容数组（通常一条文本）
 */
data class ArkInputItem(
    @SerializedName("type") val type: String? = null,
    @SerializedName("role") val role: String,
    @SerializedName("status") val status: String? = null,
    @SerializedName("content") val content: List<ArkContentItem>
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"

        /** assistant 历史项必填的 item 类型 */
        const val TYPE_MESSAGE = "message"

        /** assistant 历史项必填的状态 */
        const val STATUS_COMPLETED = "completed"

        /** 构造一条 user 文本消息（精简结构，无需 type/status） */
        fun user(text: String): ArkInputItem =
            ArkInputItem(
                role = ROLE_USER,
                content = listOf(ArkContentItem(ArkContentItem.TYPE_INPUT_TEXT, text))
            )

        /** 构造一条 assistant 历史文本消息（补齐 type=message、status=completed） */
        fun assistant(text: String): ArkInputItem =
            ArkInputItem(
                type = TYPE_MESSAGE,
                role = ROLE_ASSISTANT,
                status = STATUS_COMPLETED,
                content = listOf(ArkContentItem(ArkContentItem.TYPE_OUTPUT_TEXT, text))
            )
    }
}
