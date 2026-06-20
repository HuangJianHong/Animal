package com.example.animal.chat.model

import android.util.Log
import com.example.animal.chat.entity.ArkRequest
import com.example.animal.chat.entity.SseStreamResponse
import com.example.animal.net.config.NetConfig
import com.example.animal.net.core.RetrofitClient
import com.example.animal.net.exception.ApiException
import com.example.animal.net.exception.ErrorCode
import com.example.animal.net.utils.GsonHelper
import com.example.animal.net.utils.NetworkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 聊天数据仓库：隔离网络层，封装火山方舟 SSE 流式 responses 接口。
 *
 * - 复用项目全局 Retrofit/OkHttp（[RetrofitClient]），不直接操作 OkHttp 实例；
 * - 以 Kotlin Flow 形式逐段对外发射 AI 增量回复文本（打字机数据源）；
 * - 通过 callbackFlow + awaitClose，在协程取消时主动 cancel OkHttp Call，强制中断 SSE 长连接；
 * - 统一将各类错误转换为 [ApiException]（401/429/400/500/超时/无网络等）。
 */
class ChatRepository {

    /** 复用全局 Retrofit，指定火山方舟 BaseUrl 创建接口实例 */
    private val api: ArkApiService by lazy {
        RetrofitClient.create(ArkApiService::class.java, NetConfig.ARK_BASE_URL)
    }

    private val gson = GsonHelper.gson

    /**
     * 发起 SSE 流式对话请求。
     *
     * @param request 已组装好的请求体（人设 + 历史 + 最新消息）
     * @return 增量文本 Flow：每段 emit 一次新增的回复文本；正常结束 Flow 完成；异常以 [ApiException] 抛出
     */
    fun streamChat(request: ArkRequest): Flow<String> = callbackFlow {
        // 请求前网络检测：无网络直接抛出
        if (!NetworkUtils.isConnected()) {
            close(ApiException(ErrorCode.NO_NETWORK, ErrorCode.getMessage(ErrorCode.NO_NETWORK)))
            return@callbackFlow
        }

        val authorization = "Bearer ${NetConfig.ARK_API_KEY}"
        val call = api.createResponseStream(authorization, request)

        // 在 IO 线程执行阻塞式流读取
        val worker = launch(Dispatchers.IO) {
            try {
                val response = call.execute()
                // HTTP 层错误：401 鉴权 / 429 限流 / 400 参数 / 500 服务异常等
                if (!response.isSuccessful) {
                    val code = response.code()
                    if (NetConfig.isDebug) {
                        Log.e(TAG, "SSE HTTP 错误 code=$code, body=${response.errorBody()?.string()}")
                    }
                    close(ApiException(code, ErrorCode.getMessage(code)))
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    close(ApiException(ErrorCode.UNKNOWN, "响应体为空"))
                    return@launch
                }

                // 逐行解析 SSE 分段数据
                body.source().use { source ->
                    while (isActive && !source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isEmpty()) continue
                        if (NetConfig.isDebug) Log.d(TAG, "SSE< $line")

                        // 仅处理 data: 行
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data.isEmpty()) continue
                        // 文本流结束标记
                        if (data == SseStreamResponse.DONE_FLAG) break

                        val parsed = runCatching {
                            gson.fromJson(data, SseStreamResponse::class.java)
                        }.getOrNull() ?: continue

                        // 事件流结束标记
                        if (parsed.isStreamEnd()) break

                        // 提取增量文本，实时下发用于打字机渲染
                        val delta = parsed.incrementalText()
                        if (delta.isNotEmpty()) trySend(delta)
                    }
                }
                // 正常读取完成
                close()
            } catch (e: CancellationException) {
                // 协程被取消（页面销毁），正常结束
                close()
            } catch (e: Throwable) {
                // 网络超时、连接失败、解析异常等
                close(e)
            }
        }

        // 下游取消（页面 onDestroy / ViewModel onCleared）时：主动中断长连接
        awaitClose {
            call.cancel()
            worker.cancel()
            if (NetConfig.isDebug) Log.d(TAG, "SSE 连接已关闭并中断")
        }
    }

    companion object {
        private const val TAG = "ArkSSE"
    }
}
