package com.example.animal.image.listener

import android.os.Handler
import android.os.Looper
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Glide 网络图片加载「进度」管理器（基于 OkHttp 拦截器实现）。
 *
 * 原理：用自定义 [OkHttpClient]（带 [ProgressInterceptor]）替换 Glide 默认网络栈，
 * 拦截响应体读取过程，按 url 分发进度给注册的监听者。
 *
 * 用法：
 * - 在 AppGlideModule 中通过 [okHttpClient] 替换网络栈；
 * - 加载前 [addListener]，加载结束 [removeListener]。
 */
object ImageProgressManager {

    private val mainHandler = Handler(Looper.getMainLooper())

    /** url -> 进度回调（主线程回调 percent） */
    private val listeners = ConcurrentHashMap<String, (Int) -> Unit>()

    /** url -> 上次进度，避免重复回调相同百分比 */
    private val progressCache = ConcurrentHashMap<String, Int>()

    /** 注册某 url 的进度监听 */
    fun addListener(url: String, onProgress: (Int) -> Unit) {
        listeners[url] = onProgress
    }

    /** 移除某 url 的进度监听 */
    fun removeListener(url: String) {
        listeners.remove(url)
        progressCache.remove(url)
    }

    /** 内部：分发进度 */
    private fun dispatch(url: String, bytesRead: Long, contentLength: Long, done: Boolean) {
        val listener = listeners[url] ?: return
        val percent = if (contentLength > 0) (bytesRead * 100 / contentLength).toInt() else if (done) 100 else 0
        val last = progressCache[url] ?: -1
        if (percent == last && !done) return
        progressCache[url] = percent
        mainHandler.post { listener(percent) }
        if (done) removeListener(url)
    }

    /**
     * 提供给 AppGlideModule 使用的、带进度拦截器的 OkHttpClient。
     */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addNetworkInterceptor(ProgressInterceptor())
            .build()
    }

    /** 进度拦截器：包裹响应体 */
    private class ProgressInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            val body = response.body ?: return response
            val url = request.url.toString()
            return response.newBuilder()
                .body(ProgressResponseBody(url, body))
                .build()
        }
    }

    /** 统计读取字节数并回调进度的响应体 */
    private class ProgressResponseBody(
        private val url: String,
        private val delegate: ResponseBody
    ) : ResponseBody() {

        private val bufferedSource: BufferedSource by lazy { source(delegate.source()).buffer() }

        override fun contentType(): MediaType? = delegate.contentType()
        override fun contentLength(): Long = delegate.contentLength()
        override fun source(): BufferedSource = bufferedSource

        private fun source(source: Source): Source {
            // 捕获外层 ResponseBody，避免与 ForwardingSource.delegate 字段冲突
            val responseBody = delegate
            return object : ForwardingSource(source) {
                private var totalBytesRead = 0L
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    val fullLength = responseBody.contentLength()
                    if (bytesRead == -1L) {
                        totalBytesRead = fullLength
                    } else {
                        totalBytesRead += bytesRead
                    }
                    dispatch(url, totalBytesRead, fullLength, bytesRead == -1L)
                    return bytesRead
                }
            }
        }
    }
}
