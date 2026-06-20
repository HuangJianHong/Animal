package com.example.animal.net.interceptor

import android.util.Log
import com.example.animal.net.config.NetConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import java.nio.charset.Charset

/**
 * 日志拦截器（区分 debug/release）。
 *
 * - Debug：打印完整请求行、请求头、请求体、响应码、耗时、响应体；
 * - Release：完全关闭日志输出；
 * - 敏感字段脱敏：token、password 等不打印明文，统一替换为 ***。
 */
class LogInterceptor : Interceptor {

    private val tag = "NetLog"

    /** 需要脱敏的敏感字段（Header Key 与 Body 字段名通用匹配） */
    private val sensitiveKeys = listOf("token", "authorization", "password", "pwd", "secret")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Release 环境直接放行，不打印任何日志
        if (!NetConfig.isDebug) {
            return chain.proceed(request)
        }

        val sb = StringBuilder()
        sb.append("\n┌──────── Request ────────\n")
        sb.append("│ ${request.method} ${request.url}\n")

        // 请求头（脱敏）
        request.headers.forEach { (name, value) ->
            sb.append("│ $name: ${desensitizeHeader(name, value)}\n")
        }

        // 请求体（脱敏）
        request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset = body.contentType()?.charset(Charset.forName("UTF-8")) ?: Charsets.UTF_8
            sb.append("│ Body: ${desensitizeBody(buffer.readString(charset))}\n")
        }
        sb.append("└─────────────────────────")
        Log.d(tag, sb.toString())

        // 执行请求并统计耗时
        val startNs = System.nanoTime()
        val response: Response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            Log.e(tag, "HTTP FAILED: ${e.message}")
            throw e
        }
        val tookMs = (System.nanoTime() - startNs) / 1_000_000

        val respSb = StringBuilder()
        respSb.append("\n┌──────── Response ────────\n")
        respSb.append("│ ${response.code} ${response.message} (${tookMs}ms) ${response.request.url}\n")

        // 响应体（peek 读取，避免消费掉真正的流）
        val contentType = response.body?.contentType()?.toString().orEmpty()
        val isEventStream = contentType.contains("text/event-stream", ignoreCase = true)
        when {
            // SSE 流式响应：peek 会阻塞直到流结束，破坏实时性，这里不读取流体，
            // 流式内容由 ChatRepository 逐行打印日志
            isEventStream ->
                respSb.append("│ Body: <SSE 流式响应，分段日志见 ArkSSE 标签>\n")

            response.promisesBody() -> {
                val peekBody = response.peekBody(1024 * 1024) // 最多 1MB
                respSb.append("│ Body: ${desensitizeBody(peekBody.string())}\n")
            }
        }
        respSb.append("└──────────────────────────")
        Log.d(tag, respSb.toString())

        return response
    }

    /** Header 脱敏 */
    private fun desensitizeHeader(name: String, value: String): String =
        if (sensitiveKeys.any { name.contains(it, ignoreCase = true) }) "***" else value

    /** Body 中敏感字段脱敏（简单正则替换 "key":"value"） */
    private fun desensitizeBody(body: String): String {
        var result = body
        sensitiveKeys.forEach { key ->
            val regex = Regex("\"$key\"\\s*:\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE)
            result = result.replace(regex, "\"$key\":\"***\"")
        }
        return result
    }
}
