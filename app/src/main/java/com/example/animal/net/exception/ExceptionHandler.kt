package com.example.animal.net.exception

import com.example.animal.net.config.NetConfig
import com.google.gson.JsonParseException
import org.json.JSONException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

/**
 * 异常解析工具：把任意 [Throwable] 统一转换为 [ApiException]，并触发全局错误码处理。
 */
object ExceptionHandler {

    /**
     * 将原始异常解析为统一的 [ApiException]。
     */
    fun handle(throwable: Throwable): ApiException {
        val exception: ApiException = when (throwable) {
            // 已经是统一异常，直接返回
            is ApiException -> throwable

            // HTTP 状态码异常（Retrofit 抛出）
            is HttpException -> {
                val code = throwable.code()
                ApiException(code, ErrorCode.getMessage(code), throwable)
            }

            // 超时
            is SocketTimeoutException ->
                ApiException(ErrorCode.TIMEOUT, ErrorCode.getMessage(ErrorCode.TIMEOUT), throwable)

            // 域名解析失败 / 连接失败 —— 多归类为网络问题
            is UnknownHostException, is ConnectException ->
                ApiException(ErrorCode.NETWORK_ERROR, ErrorCode.getMessage(ErrorCode.NETWORK_ERROR), throwable)

            // SSL 证书异常
            is SSLHandshakeException, is SSLException ->
                ApiException(ErrorCode.SSL_ERROR, ErrorCode.getMessage(ErrorCode.SSL_ERROR), throwable)

            // 数据解析异常
            is JsonParseException, is JSONException ->
                ApiException(ErrorCode.PARSE_ERROR, ErrorCode.getMessage(ErrorCode.PARSE_ERROR), throwable)

            // 其它 IO 异常
            is IOException ->
                ApiException(ErrorCode.NETWORK_ERROR, ErrorCode.getMessage(ErrorCode.NETWORK_ERROR), throwable)

            // 兜底
            else ->
                ApiException(ErrorCode.UNKNOWN, throwable.message ?: ErrorCode.getMessage(ErrorCode.UNKNOWN), throwable)
        }
        // 统一触发全局错误码处理（如 401 跳登录）
        dispatchGlobalError(exception)
        return exception
    }

    /**
     * 全局错误码统一处理：401 触发登录失效回调等。
     */
    private fun dispatchGlobalError(e: ApiException) {
        when (e.code) {
            ErrorCode.UNAUTHORIZED -> {
                // 清除本地 token 并回调登录失效
                NetConfig.token = ""
                NetConfig.onLoginExpired?.invoke()
            }
            // 403 / 500 等可在此扩展全局 Toast 等逻辑
        }
    }
}
