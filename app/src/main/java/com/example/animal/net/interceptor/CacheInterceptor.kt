package com.example.animal.net.interceptor

import com.example.animal.net.config.NetConfig
import com.example.animal.net.config.NetConstants
import com.example.animal.net.utils.NetworkUtils
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 网络缓存拦截器（需配合 OkHttpClient.cache 使用）。
 *
 * 策略：
 * - 有网络：优先请求服务端，并写入缓存（缓存有效期 [NetConstants.CACHE_AGE_ONLINE]）；
 * - 无网络：强制读取本地缓存（允许使用 [NetConstants.CACHE_AGE_OFFLINE] 内的过期缓存）。
 *
 * 注意：本拦截器同时作为「应用拦截器(请求前)」与「网络拦截器(响应后)」逻辑合并实现，
 * 通过判断是否有网络改写 Cache-Control，兼顾在线/离线两种场景。
 */
class CacheInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 缓存开关关闭时直接放行
        if (!NetConfig.enableCache) {
            return chain.proceed(chain.request())
        }

        var request = chain.request()
        val hasNetwork = NetworkUtils.isConnected()

        // 无网络：改写请求强制走缓存
        if (!hasNetwork) {
            request = request.newBuilder()
                .cacheControl(CacheControl.FORCE_CACHE)
                .build()
        }

        val response = chain.proceed(request)

        return if (hasNetwork) {
            // 有网络：覆盖服务端可能返回的 no-cache，按自定义有效期缓存
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", "public, max-age=${NetConstants.CACHE_AGE_ONLINE}")
                .build()
        } else {
            // 无网络：允许使用过期缓存
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", "public, only-if-cached, max-stale=${NetConstants.CACHE_AGE_OFFLINE}")
                .build()
        }
    }
}
