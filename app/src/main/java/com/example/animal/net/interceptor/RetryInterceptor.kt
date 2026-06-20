package com.example.animal.net.interceptor

import com.example.animal.net.config.NetConfig
import com.example.animal.net.config.NetConstants
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 失败重试拦截器。
 *
 * 规则：
 * - 仅 GET 请求允许重试（POST 上传等非幂等请求禁止重试，避免重复提交）；
 * - 最大重试 [NetConstants.MAX_RETRY_COUNT] 次；
 * - 每次重试间隔 [NetConstants.RETRY_INTERVAL_MS]；
 * - 仅在抛出 IOException 或响应不成功时触发重试。
 */
class RetryInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 重试开关关闭，或非 GET 请求：不重试，直接执行
        if (!NetConfig.enableRetry || !request.method.equals("GET", ignoreCase = true)) {
            return chain.proceed(request)
        }

        var lastException: IOException? = null
        var response: Response? = null

        // 首次 + 重试次数
        var retryCount = 0
        while (retryCount <= NetConstants.MAX_RETRY_COUNT) {
            try {
                // 关闭上一次未成功的响应，避免资源泄漏
                response?.close()
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    return response
                }
            } catch (e: IOException) {
                lastException = e
            }

            // 已达最大次数则跳出
            if (retryCount == NetConstants.MAX_RETRY_COUNT) break

            retryCount++
            try {
                Thread.sleep(NetConstants.RETRY_INTERVAL_MS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        // 返回最后一次响应；若全程异常则抛出
        return response ?: throw (lastException ?: IOException("请求失败"))
    }
}
