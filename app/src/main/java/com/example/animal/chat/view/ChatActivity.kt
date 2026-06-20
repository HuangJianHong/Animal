package com.example.animal.chat.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animal.R
import com.example.animal.animal.entity.Animal
import com.example.animal.chat.viewmodel.ChatViewModel
import com.example.animal.databinding.ActivityChatBinding
import com.example.animal.image.core.ImageLoader
import kotlinx.coroutines.launch

/**
 * 聊天页。
 *
 * MVVM 约束：本 Activity 仅负责 UI 展示与用户事件分发，
 * 全部数据、网络、对话逻辑交给 [ChatViewModel]，通过 StateFlow 驱动 UI。
 *
 * 生命周期：onDestroy 时调用 [ChatViewModel.release] 立刻中断 SSE、取消协程、
 * 清空内存对话与消息列表；每次进入都是全新空白对话。
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var animal: Animal

    /** 通过 Factory 注入当前小动物 */
    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.Factory(animal)
    }

    private val chatAdapter by lazy {
        ChatAdapter(onResend = { messageId -> viewModel.resend(messageId) })
    }

    /** 上一次消息数量，用于区分「新增消息」与「流式内容更新」 */
    private var lastMessageCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 解析传入的小动物实体（无效则直接关闭）
        @Suppress("DEPRECATION")
        val parsed = intent.getSerializableExtra(Animal.EXTRA_KEY) as? Animal
        if (parsed == null) {
            finish()
            return
        }
        animal = parsed

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTitle()
        initRecyclerView()
        initInput()
        observeViewModel()
    }

    /** 顶部：动物头像 + 名称 + 返回 */
    private fun initTitle() {
        binding.tvTitleName.text = viewModel.animalName
        ImageLoader.loadCircle(binding.ivTitleAvatar, viewModel.animalAvatarRes)
        binding.ivBack.setOnClickListener { finish() }
    }

    /** 消息列表 */
    private fun initRecyclerView() {
        // 消息从顶部自然向下排列（不足一屏时贴顶，避免顶部出现大片空白）
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = chatAdapter
        // 关闭默认变更动画：打字机高频局部刷新时，淡入/位移动画会造成闪烁与卡顿
        binding.rvMessages.itemAnimator = null
    }

    /** 底部输入区：空文本置灰发送按钮 + 清空 + 发送 */
    private fun initInput() {
        // 初始置灰
        updateSendButton(false)

        binding.etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSendButton(!s.isNullOrBlank())
            }
        })

        // 一键清空输入框
        binding.ivClear.setOnClickListener { binding.etInput.setText("") }

        // 发送
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text?.toString().orEmpty()
            if (text.isBlank()) return@setOnClickListener
            viewModel.sendMessage(text)
            binding.etInput.setText("")
        }
    }

    /** 发送按钮可用状态 + 视觉置灰 */
    private fun updateSendButton(enabled: Boolean) {
        binding.btnSend.isEnabled = enabled
        binding.btnSend.alpha = if (enabled) 1f else 0.6f
    }

    /** 监听 ViewModel 数据流，自动刷新 UI */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 消息列表
                launch {
                    viewModel.messages.collect { list ->
                        // 是否新增了消息（新一轮发送/回复）——新增时强制贴底；
                        // 否则为流式内容更新，仅当用户当前停留在底部时才自动跟随，
                        // 避免用户上翻历史时被强制拉回。
                        val isNewMessage = list.size != lastMessageCount
                        val shouldStick = isNewMessage || isAtBottom()
                        lastMessageCount = list.size
                        chatAdapter.submitList(list) {
                            if (list.isNotEmpty() && shouldStick) scrollToBottom()
                        }
                    }
                }
                // 异常弹窗
                launch {
                    viewModel.error.collect { msg ->
                        if (!msg.isNullOrEmpty()) {
                            showErrorDialog(msg)
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    /** 列表当前是否停留在底部（或内容不足一屏），用于决定流式时是否自动跟随 */
    private fun isAtBottom(): Boolean {
        val lm = binding.rvMessages.layoutManager as? LinearLayoutManager ?: return true
        val last = lm.findLastVisibleItemPosition()
        return last == RecyclerView.NO_POSITION || last >= chatAdapter.itemCount - 1
    }

    /** 滚动到最后一条消息，贴底显示最新内容（含正在流式增长的长文本底部） */
    private fun scrollToBottom() {
        val last = chatAdapter.itemCount - 1
        if (last < 0) return
        val lm = binding.rvMessages.layoutManager as? LinearLayoutManager ?: return
        // 用一个足够大的负偏移，把最后一项（即便高度超过一屏）的底部对齐到列表底部，
        // 始终展示最新文字；RecyclerView 会自动 clamp，不会过度滚动。
        lm.scrollToPositionWithOffset(last, -BIG_SCROLL_OFFSET)
    }

    /** 统一异常弹窗提示（无网络/超时/401/429/500 等） */
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.chat_error_title)
            .setMessage(message)
            .setPositiveButton(R.string.chat_dialog_confirm, null)
            .show()
    }

    /**
     * 页面销毁：强制中断 SSE、取消协程、清空内存对话与消息列表。
     * App 切后台不持久化任何上下文（仅内存维护），再次进入即全新对话。
     */
    override fun onDestroy() {
        viewModel.release()
        super.onDestroy()
    }

    companion object {
        /** 贴底滚动用的大偏移量：保证超长消息底部对齐列表底部 */
        private const val BIG_SCROLL_OFFSET = 100_000
    }
}
