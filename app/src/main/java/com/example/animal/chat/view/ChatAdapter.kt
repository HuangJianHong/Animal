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
 * - 使用 DiffUtil 局部刷新，配合打字机实时更新。
 *
 * @param onResend 失败消息重发回调（回传消息 id）
 */
class ChatAdapter(
    private val onResend: (Long) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(o: ChatMessage, n: ChatMessage): Boolean = o.id == n.id
            override fun areContentsTheSame(o: ChatMessage, n: ChatMessage): Boolean = o == n
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserHolder -> holder.bind(msg)
            is AiHolder -> holder.bind(msg)
        }
    }

    /** 我方消息 */
    inner class UserHolder(private val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            binding.tvContent.text = msg.content
            ImageLoader.loadCircle(binding.ivAvatar, msg.avatarResId)
        }
    }

    /** AI 小动物消息 */
    inner class AiHolder(private val binding: ItemChatAiBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            ImageLoader.loadCircle(binding.ivAvatar, msg.avatarResId)

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
                // 流式中 / 完成：正常展示文本
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
