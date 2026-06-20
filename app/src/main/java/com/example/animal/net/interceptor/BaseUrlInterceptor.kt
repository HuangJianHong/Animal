package com.example.animal.net.interceptor

import com.example.animal.net.config.BaseUrlManager
import com.example.animal.net.config.NetConstants
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 多 BaseUrl 动态切换拦截器。
 *
 * 接口方法上通过注解头声明要使用的域名别名：
 * ```kotlin
 * @Headers("${NetConstants.HEADER_BASE_URL}: file")
 * @GET("upload/token")
 * suspend fun xxx(): ResponseBean<String>
 * ```
 * 本拦截器读取该 Header，将请求的 host/scheme/port 替换为对应别名域名，并移除该临时 Header。
 */
class BaseUrlInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originRequest = chain.request()
        val urlName = originRequest.header(NetConstants.HEADER_BASE_URL)
            ?: return chain.proceed(originRequest) // 未指定，使用默认 baseUrl

        // 移除临时 Header
        val builder = originRequest.newBuilder().removeHeader(NetConstants.HEADER_BASE_URL)

        val newBaseUrl = BaseUrlManager.getUrlByName(urlName).toHttpUrlOrNull()
            ?: return chain.proceed(builder.build())

        // 用新域名重建 URL（保留原 path 与 query）
        val newUrl = originRequest.url.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()

        return chain.proceed(builder.url(newUrl).build())
    }
}
