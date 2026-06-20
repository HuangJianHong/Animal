package com.example.animal.chat.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.animal.chat.entity.ChatMessage
import com.example.animal.databinding.ItemChatAiBinding
import com.example.animal.databinding.ItemChatUserBinding
import com.example.animal.image.core.ImageLoader

/**
 * 聊天消息适配器。
 *
 * - 双 ViewType：我方消息（右侧气泡）/ AI 小动物消息（左侧气泡 + 头像）；
 * - AI 消息根据状态展示：加载动画 / 打字机文本 / 失败错误文案 + 重发按钮；
 * - 关键优化：流式打字时仅做「局部刷新（payload）」——只更新文本内容与状态可见性，
 *   不重新加载头像、不重建整个 item，避免高频刷新导致的头像闪烁与布局抖动错位。
 *
 * @param onResend 失败消息重发回调（回传消息 id）
 */
class ChatAdapter(
    private val onResend: (Long) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2

        /** 局部刷新标记：仅文本/状态变化（典型为流式逐字输出） */
        private const val PAYLOAD_CONTENT = "payload_content"

        /** 打字机光标：流式输出过程中追加在文本末尾，提示「正在输出」 */
        private const val TYPING_CURSOR = "▌"

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(o: ChatMessage, n: ChatMessage): Boolean = o.id == n.id
            override fun areContentsTheSame(o: ChatMessage, n: ChatMessage): Boolean = o == n

            /**
             * 同一条消息（id 相同）内容发生变化时，返回局部刷新标记，
             * 让 RecyclerView 走 payload 分支只更新文本，避免重载头像 / 重建整项。
             */
            override fun getChangePayload(o: ChatMessage, n: ChatMessage): Any? =
                if (o.id == n.id) PAYLOAD_CONTENT else null
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isFromMe) TYPE_USER else TYPE_AI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserHolder(ItemChatUserBinding.inflate(inflater, parent, false))
        } else {
            AiHolder(ItemChatAiBinding.inflate(inflater, parent, false))
        }
    }

    /** 全量绑定：加载头像 + 渲染内容 */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserHolder -> holder.bind(msg)
            is AiHolder -> holder.bind(msg)
        }
    }

    /** 局部绑定：仅在收到 [PAYLOAD_CONTENT] 时只刷新文本/状态，不触碰头像 */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            // 无 payload：走全量绑定（含头像加载）
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        val msg = getItem(position)
        when (holder) {
            is UserHolder -> holder.bindContent(msg)
            is AiHolder -> holder.bindContent(msg)
        }
    }

    /** 我方消息 */
    inner class UserHolder(private val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: ChatMessage) {
            ImageLoader.loadCircle(binding.ivAvatar, msg.avatarResId)
            bindContent(msg)
        }

        fun bindContent(msg: ChatMessage) {
            binding.tvContent.text = msg.content
        }
    }

    /** AI 小动物消息 */
    inner class AiHolder(private val binding: ItemChatAiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: ChatMessage) {
            // 头像仅在全量绑定时加载一次，流式刷新不再重复请求，避免闪烁
            ImageLoader.loadCircle(binding.ivAvatar, msg.avatarResId)
            bindContent(msg)
        }

        fun bindContent(msg: ChatMessage) {
            when (msg.status) {
                // 等待 AI 首个字：显示加载动画，隐藏文本与重发
                ChatMessage.Status.SENDING -> {
                    binding.pbLoading.visibility = View.VISIBLE
                    binding.tvContent.visibility = View.GONE
                    binding.tvResend.visibility = View.GONE
                }
                // 失败：红色错误文案 + 重发按钮
                ChatMessage.Status.FAILED -> {
                    binding.pbLoading.visibility = View.GONE
                    binding.tvContent.visibility = View.VISIBLE
                    binding.tvContent.setTextColor(Color.parseColor("#E53935"))
                    binding.tvContent.text = msg.content
                    binding.tvResend.visibility = View.VISIBLE
                    binding.tvResend.setOnClickListener { onResend(msg.id) }
                }
                // 流式中：正常文本 + 末尾打字机光标
                ChatMessage.Status.STREAMING -> {
                    binding.pbLoading.visibility = View.GONE
                    binding.tvContent.visibility = View.VISIBLE
                    binding.tvContent.setTextColor(Color.parseColor("#1A1A1A"))
                    binding.tvContent.text = msg.content + TYPING_CURSOR
                    binding.tvResend.visibility = View.GONE
                }
                // 完成：正常展示最终文本
                else -> {
                    binding.pbLoading.visibility = View.GONE
                    binding.tvContent.visibility = View.VISIBLE
                    binding.tvContent.setTextColor(Color.parseColor("#1A1A1A"))
                    binding.tvContent.text = msg.content
                    binding.tvResend.visibility = View.GONE
                }
            }
        }
    }
}
