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
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            // 新消息贴底显示
            stackFromEnd = true
        }
        binding.rvMessages.adapter = chatAdapter
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
                        chatAdapter.submitList(list) {
                            if (list.isNotEmpty()) {
                                binding.rvMessages.scrollToPosition(list.size - 1)
                            }
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
}
