package com.example.animal.net.core

import com.example.animal.net.config.BaseUrlManager
import com.example.animal.net.config.NetConfig
import com.example.animal.net.config.NetConstants
import com.example.animal.net.interceptor.BaseUrlInterceptor
import com.example.animal.net.interceptor.CacheInterceptor
import com.example.animal.net.interceptor.EncryptInterceptor
import com.example.animal.net.interceptor.HeaderInterceptor
import com.example.animal.net.interceptor.LogInterceptor
import com.example.animal.net.interceptor.RetryInterceptor
import com.example.animal.net.utils.GsonHelper
import com.example.animal.net.utils.PersistentCookieJar
import com.example.animal.net.utils.SslHelper
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Retrofit / OkHttp 核心管理类（双重检查锁单例）。
 *
 * 职责：
 * 1. 全局唯一 [OkHttpClient]，所有请求共享一个连接池/线程池，避免多实例内存浪费；
 * 2. 按 baseUrl 缓存多个 [Retrofit]，支持多域名；
 * 3. 统一装配全部拦截器、SSL、Cookie、缓存、Gson。
 */
object RetrofitClient {

    /** 全局唯一 OkHttpClient（双重检查锁懒加载） */
    @Volatile
    private var okHttpClient: OkHttpClient? = null

    /** 按 baseUrl 缓存的 Retrofit，多域名复用 */
    private val retrofitMap = ConcurrentHashMap<String, Retrofit>()

    /** 按接口类型缓存的 ApiService 代理，避免重复创建 */
    private val serviceMap = ConcurrentHashMap<String, Any>()

    /**
     * 获取全局唯一 OkHttpClient。
     */
    fun getOkHttpClient(): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: buildOkHttpClient().also { okHttpClient = it }
        }
    }

    /**
     * 构建 OkHttpClient：装配超时、缓存、Cookie、SSL 及全部拦截器。
     */
    private fun buildOkHttpClient(): OkHttpClient {
        val cacheDir = File(NetConfig.appContext.cacheDir, NetConstants.CACHE_DIR_NAME)
        val cache = Cache(cacheDir, NetConstants.CACHE_MAX_SIZE)
        val sslParams = SslHelper.getSSLParams()

        return OkHttpClient.Builder()
            // 动态超时（秒）
            .connectTimeout(NetConfig.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(NetConfig.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(NetConfig.writeTimeout, TimeUnit.SECONDS)
            // 网络缓存
            .cache(cache)
            // Cookie 持久化
            .cookieJar(PersistentCookieJar())
            // HTTPS / SSL 适配
            .sslSocketFactory(sslParams.sslSocketFactory, sslParams.trustManager)
            .hostnameVerifier(SslHelper.hostnameVerifier())
            // 失败自动重连（OkHttp 内置）
            .retryOnConnectionFailure(true)
            // ============ 应用拦截器（顺序很重要）============
            .addInterceptor(BaseUrlInterceptor())   // 1. 动态域名切换
            .addInterceptor(HeaderInterceptor())    // 2. 公共请求头
            .addInterceptor(EncryptInterceptor())   // 3. 参数加密
            .addInterceptor(CacheInterceptor())     // 4. 缓存策略
            .addInterceptor(RetryInterceptor())     // 5. 失败重试
            .addInterceptor(LogInterceptor())       // 6. 日志（放最后，打印最终报文）
            .build()
    }

    /**
     * 获取（或创建）指定 baseUrl 的 Retrofit。
     * @param baseUrl 为空时使用当前环境默认主域名。
     */
    fun getRetrofit(baseUrl: String = BaseUrlManager.getMainUrl()): Retrofit {
        return retrofitMap.getOrPut(baseUrl) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(GsonHelper.gson))
                .build()
        }
    }

    /**
     * 创建（或复用）API 接口实例。
     * @param service 接口 Class
     * @param baseUrl 指定域名，默认当前环境主域名
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> create(service: Class<T>, baseUrl: String = BaseUrlManager.getMainUrl()): T {
        val key = baseUrl + "_" + service.name
        return serviceMap.getOrPut(key) {
            getRetrofit(baseUrl).create(service) as Any
        } as T
    }

    /**
     * 重置客户端（如动态修改了超时/SSL 开关后需重建生效）。
     */
    fun reset() {
        synchronized(this) {
            okHttpClient = null
            retrofitMap.clear()
            serviceMap.clear()
        }
    }
}
