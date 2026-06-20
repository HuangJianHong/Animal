package com.example.animal.chat.entity

import com.google.gson.annotations.SerializedName

/**
 * content 数组内的单条文本内容实体。
 *
 * @param type content 类型：
 *             - 用户输入使用 "input_text"
 *             - 助手历史回复使用 "output_text"
 * @param text 文本内容
 */
data class ArkContentItem(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String
) {
    companion object {
        const val TYPE_INPUT_TEXT = "input_text"
        const val TYPE_OUTPUT_TEXT = "output_text"
    }
}
