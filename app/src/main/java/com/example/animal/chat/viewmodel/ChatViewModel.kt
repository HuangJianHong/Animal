package com.example.animal.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.animal.R
import com.example.animal.animal.entity.Animal
import com.example.animal.chat.entity.ArkInputItem
import com.example.animal.chat.entity.ArkRequest
import com.example.animal.chat.entity.ChatMessage
import com.example.animal.chat.model.ChatRepository
import com.example.animal.net.exception.ExceptionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 聊天页 ViewModel。
 *
 * 职责（MVVM 中的数据/逻辑层）：
 * - 内存维护完整对话记录与消息列表（StateFlow），不做任何本地持久化；
 * - 维护 AI 打字机实时文本、发送加载状态、异常状态（StateFlow）；
 * - 用协程 Job 管理 SSE 长连接，页面销毁时 cancel 全部协程、强制中断流式请求；
 * - sendMessage：拼接「人设 Prompt + 全部历史对话」组装 ArkRequest，经 Repository 发起 SSE；
 * - 捕获异常更新异常状态供页面弹窗，支持单条失败消息重发。
 *
 * 每次进入页面都会创建全新 ViewModel（全新空白对话），页面销毁即随之清空。
 *
 * @param animal 当前聊天的小动物（携带头像与专属人设 Prompt）
 */
class ChatViewModel(private val animal: Animal) : ViewModel() {

    private val repository = ChatRepository()

    /** 完整消息列表（含我方/AI 消息），UI 监听自动刷新 */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /** AI 打字机实时文本（当前正在输出的完整内容） */
    private val _typingText = MutableStateFlow("")
    val typingText: StateFlow<String> = _typingText.asStateFlow()

    /** 发送/接收加载状态 */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** 异常状态：非空时页面弹窗提示，提示后调用 [clearError] 复位 */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 当前 SSE 流式请求协程 Job */
    private var streamJob: Job? = null

    /** 消息自增 id */
    private var idSeq = 0L
    private fun nextId(): Long = ++idSeq

    /** 当前小动物名称（供顶部标题展示） */
    val animalName: String get() = animal.name

    /** 当前小动物头像（供顶部展示） */
    val animalAvatarRes: Int get() = animal.avatarResId

    /**
     * 发送一条新消息。
     *
     * @param text 用户输入文本
     */
    fun sendMessage(text: String) {
        val content = text.trim()
        if (content.isEmpty()) return

        // 1. 追加我方消息 + 一条 AI 占位消息（加载中）
        val userMsg = ChatMessage(
            id = nextId(),
            isFromMe = true,
            avatarResId = R.drawable.ic_avatar_user,
            content = content,
            status = ChatMessage.Status.SUCCESS
        )
        val aiMsg = ChatMessage(
            id = nextId(),
            isFromMe = false,
            avatarResId = animal.avatarResId,
            content = "",
            status = ChatMessage.Status.SENDING,
            relatedUserText = content
        )
        _messages.value = _messages.value + userMsg + aiMsg

        // 2. 发起流式请求
        startStream(aiMsg.id)
    }

    /**
     * 重发某条失败的 AI 消息。
     *
     * @param aiMessageId 失败 AI 消息 id
     */
    fun resend(aiMessageId: Long) {
        val target = _messages.value.firstOrNull { it.id == aiMessageId } ?: return
        if (!target.isFailed()) return
        startStream(aiMessageId)
    }

    /**
     * 启动 SSE 流式请求并将增量文本渲染到指定 AI 消息。
     *
     * 流畅度关键：把「网络收字」与「UI 吐字」解耦——
     * - SSE 的增量文本是按网络分块到达的（一次可能来好几个字、再停顿），直接渲染会一卡一卡；
     * - 这里先把收到的字符放入 [pending] 缓冲区，再由独立的「打字机协程」按固定节奏匀速逐字吐出，
     *   积压越多每帧吐字越多，兼顾长文本速度与短文本的逐字打字感，视觉上平滑自然。
     */
    private fun startStream(aiMessageId: Long) {
        // 取消上一个未结束的流
        streamJob?.cancel()

        // 重置该 AI 消息为加载态
        updateMessage(aiMessageId) { it.copy(content = "", status = ChatMessage.Status.SENDING) }

        // 组装请求体：人设 + 全部历史对话
        val request = buildRequest()

        streamJob = viewModelScope.launch {
            val displayed = StringBuilder()   // 已渲染到 UI 的文本
            val pending = StringBuilder()     // 已收到、待渲染的缓冲文本
            var streamFinished = false        // 网络流是否读取完毕
            var streamError: Throwable? = null
            _loading.value = true

            // 打字机协程：按固定节奏匀速把 pending 中的字符吐到 UI
            val typer = launch {
                while (isActive) {
                    if (pending.isNotEmpty()) {
                        // 积压越多每帧吐字越多（1~MAX_CHARS_PER_FRAME），避免长文本等待过久
                        val step = (pending.length / 10)
                            .coerceIn(1, MAX_CHARS_PER_FRAME)
                            .coerceAtMost(pending.length)
                        displayed.append(pending, 0, step)
                        pending.delete(0, step)
                        val cur = displayed.toString()
                        _typingText.value = cur
                        updateMessage(aiMessageId) {
                            it.copy(content = cur, status = ChatMessage.Status.STREAMING)
                        }
                        delay(TYPING_FRAME_MS)
                    } else if (streamFinished) {
                        break
                    } else {
                        // 缓冲为空但流未结束：等待下一段网络数据
                        delay(TYPING_FRAME_MS)
                    }
                }
            }

            try {
                repository.streamChat(request).collect { delta ->
                    // 只负责把网络增量塞进缓冲，渲染节奏交给打字机协程
                    pending.append(delta)
                }
                streamFinished = true
            } catch (ce: CancellationException) {
                // 页面销毁/主动取消：随父协程取消 typer，直接结束
                throw ce
            } catch (e: Throwable) {
                streamError = e
                streamFinished = true
            }

            // 等打字机把剩余缓冲全部吐完，保证最终文本完整
            typer.join()

            if (streamError != null) {
                // 统一异常转换（无网络/超时/401/429/500 等）
                val apiException = ExceptionHandler.handle(streamError!!)
                updateMessage(aiMessageId) {
                    it.copy(content = apiException.errorMsg, status = ChatMessage.Status.FAILED)
                }
                _error.value = apiException.errorMsg
            } else {
                val finalText = displayed.toString().ifEmpty { "（暂时没有想到要说什么…）" }
                updateMessage(aiMessageId) {
                    it.copy(content = finalText, status = ChatMessage.Status.SUCCESS)
                }
            }
            _loading.value = false
            _typingText.value = ""
        }
    }

    /**
     * 组装 ArkRequest 的 input 数组：
     * 1. 第一条 user 消息 = 当前小动物完整人设 Prompt；
     * 2. 按时间顺序追加全部历史 user / assistant 对话（排除加载中/失败的占位消息）；
     * 3. 每次全量传入，保证 AI 多轮记忆连贯。
     */
    private fun buildRequest(): ArkRequest {
        val input = mutableListOf<ArkInputItem>()
        // 人设作为第一条 user 消息
        input.add(ArkInputItem.user(animal.systemPrompt))
        // 历史对话
        _messages.value.forEach { msg ->
            when {
                msg.isFromMe ->
                    input.add(ArkInputItem.user(msg.content))
                // 仅纳入成功的 AI 回复，排除加载中/失败/空内容
                msg.status == ChatMessage.Status.SUCCESS && msg.content.isNotBlank() ->
                    input.add(ArkInputItem.assistant(msg.content))
            }
        }
        return ArkRequest(input = input)
    }

    /** 按 id 更新消息（生成新 List 触发 StateFlow 刷新与列表 diff） */
    private fun updateMessage(id: Long, transform: (ChatMessage) -> ChatMessage) {
        _messages.value = _messages.value.map { if (it.id == id) transform(it) else it }
    }

    /** 弹窗提示后复位异常状态 */
    fun clearError() {
        _error.value = null
    }

    /**
     * 强制释放：中断 SSE、取消协程、清空全部对话上下文与消息列表。
     * 页面 onDestroy 主动调用，确保不残留长连接与对话上下文。
     */
    fun release() {
        streamJob?.cancel()
        streamJob = null
        _messages.value = emptyList()
        _typingText.value = ""
        _loading.value = false
    }

    /**
     * ViewModel 销毁（页面 finish）时兜底取消协程。
     * viewModelScope 会自动取消，这里显式 cancel 流式 Job 以立即中断长连接。
     */
    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        streamJob = null
    }

    companion object {
        /** 打字机每帧间隔（毫秒），约 60fps，越小吐字越快 */
        private const val TYPING_FRAME_MS = 16L

        /** 打字机每帧最多吐字数：缓冲积压越多越接近此上限，避免长文本等待过久 */
        private const val MAX_CHARS_PER_FRAME = 5
    }

    /**
     * ViewModel 工厂：用于注入当前 [Animal]。
     */
    class Factory(private val animal: Animal) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(animal) as T
        }
    }
}
