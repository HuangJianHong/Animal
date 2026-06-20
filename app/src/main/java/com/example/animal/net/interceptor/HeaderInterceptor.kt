package com.example.animal.net.interceptor

import android.os.Build
import com.example.animal.net.config.NetConfig
import com.example.animal.net.config.NetConstants
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 全局公共请求头拦截器。
 *
 * 自动为每个请求注入：设备型号、系统版本、App 版本、token、渠道号、平台标识。
 * 其中 token 为空时自动跳过，不携带该 Header。
 */
class HeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header(NetConstants.HEADER_DEVICE_MODEL, Build.MODEL ?: "unknown")
            .header(NetConstants.HEADER_OS_VERSION, "Android ${Build.VERSION.RELEASE}")
            .header(NetConstants.HEADER_APP_VERSION, NetConfig.appVersion)
            .header(NetConstants.HEADER_CHANNEL, NetConfig.channel)
            .header(NetConstants.HEADER_PLATFORM, NetConstants.PLATFORM_ANDROID)

        // token 为空自动跳过
        val token = NetConfig.token
        if (token.isNotEmpty()) {
            builder.header(NetConstants.HEADER_TOKEN, token)
        }

        return chain.proceed(builder.build())
    }
}
