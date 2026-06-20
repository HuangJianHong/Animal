package com.example.animal.chat.entity

import com.google.gson.annotations.SerializedName

/**
 * input 数组内单条对话实体。
 *
 * @param role    角色："user"（用户/人设）或 "assistant"（AI 历史回复）
 * @param content 内容数组（通常一条文本）
 */
data class ArkInputItem(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: List<ArkContentItem>
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"

        /** 构造一条 user 文本消息 */
        fun user(text: String): ArkInputItem =
            ArkInputItem(ROLE_USER, listOf(ArkContentItem(ArkContentItem.TYPE_INPUT_TEXT, text)))

        /** 构造一条 assistant 文本消息 */
        fun assistant(text: String): ArkInputItem =
            ArkInputItem(ROLE_ASSISTANT, listOf(ArkContentItem(ArkContentItem.TYPE_OUTPUT_TEXT, text)))
    }
}
