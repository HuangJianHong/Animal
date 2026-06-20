package com.example.animal.chat.entity

import androidx.annotation.DrawableRes

/**
 * 页面展示用聊天消息实体（仅内存维护，不做任何本地持久化）。
 *
 * @param id             消息唯一 id（用于列表 diff 与定位重发）
 * @param isFromMe       是否我方消息（true=右侧用户气泡，false=左侧 AI 小动物气泡）
 * @param avatarResId    头像资源 id（我方=用户头像，AI=当前小动物头像）
 * @param content        文本内容（AI 流式过程中会不断追加；失败时存放错误文案）
 * @param status         发送/接收状态
 * @param relatedUserText 该条 AI 消息对应的用户输入文本（用于失败重发）
 */
data class ChatMessage(
    val id: Long,
    val isFromMe: Boolean,
    @DrawableRes val avatarResId: Int,
    val content: String,
    val status: Status = Status.SUCCESS,
    val relatedUserText: String = ""
) {

    /** 消息状态 */
    enum class Status {
        /** 发送中 / 等待 AI 首个字（展示加载动画） */
        SENDING,

        /** AI 流式输出中（打字机进行中） */
        STREAMING,

        /** 完成 */
        SUCCESS,

        /** 失败（展示错误文案 + 重发按钮） */
        FAILED
    }

    /** 是否处于加载态（用于 UI 展示 loading） */
    fun isLoading(): Boolean = status == Status.SENDING

    /** 是否失败（用于 UI 展示重发按钮） */
    fun isFailed(): Boolean = status == Status.FAILED
}
