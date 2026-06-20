package com.example.animal.net.core

import com.example.animal.net.config.NetConfig
import com.example.animal.net.entity.ApiResult
import com.example.animal.net.entity.ResponseBean
import com.example.animal.net.exception.ApiException
import com.example.animal.net.exception.ErrorCode
import com.example.animal.net.exception.ExceptionHandler
import com.example.animal.net.utils.NetworkUtils
import com.example.animal.net.utils.ProgressListener
import com.example.animal.net.utils.ProgressResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

/**
 * 统一请求发起入口（协程版）。
 *
 * 所有业务请求最终通过这里发起，内部统一：
 * 1. 请求前网络检测（无网直接抛 [ErrorCode.NO_NETWORK]）；
 * 2. 自动切到 IO 线程执行（[Dispatchers.IO]）；
 * 3. 统一解包 [ResponseBean] -> data；
 * 4. 统一异常转换为 [ApiException]。
 */
object HttpManager {

    /** 获取业务 Api 接口实例（全局复用） */
    fun <T> api(service: Class<T>): T = RetrofitClient.create(service)

    /**
     * 发起请求并解包，成功返回纯 data，失败抛 [ApiException]。
     * 适合在 suspend 函数 / viewModelScope 中调用。
     */
    suspend fun <T> request(block: suspend () -> ResponseBean<T>): T = withContext(Dispatchers.IO) {
        // 请求前网络检测
        if (!NetworkUtils.isConnected()) {
            throw ApiException(ErrorCode.NO_NETWORK, ErrorCode.getMessage(ErrorCode.NO_NETWORK))
        }
        try {
            block().parseData()
        } catch (e: Throwable) {
            throw ExceptionHandler.handle(e)
        }
    }

    /**
     * 发起请求并返回 [ApiResult]（不抛异常，适合需要显式处理成功/失败的场景）。
     */
    suspend fun <T> requestResult(block: suspend () -> ResponseBean<T>): ApiResult<T> =
        try {
            ApiResult.Success(request(block))
        } catch (e: ApiException) {
            ApiResult.Error(e)
        } catch (e: Throwable) {
            ApiResult.Error(ExceptionHandler.handle(e))
        }

    /**
     * 文件下载（带进度回调）。
     *
     * @param url       完整下载地址
     * @param destFile  保存目标文件
     * @param listener  进度回调（IO 线程回调）
     * @return 下载完成的文件
     */
    suspend fun download(
        url: String,
        destFile: File,
        listener: ProgressListener? = null
    ): File = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isConnected()) {
            throw ApiException(ErrorCode.NO_NETWORK, ErrorCode.getMessage(ErrorCode.NO_NETWORK))
        }
        try {
            // 复用全局 OkHttpClient，附加进度网络拦截器
            val client = RetrofitClient.getOkHttpClient().newBuilder()
                .addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    val body = response.body
                    if (body != null && listener != null) {
                        response.newBuilder()
                            .body(ProgressResponseBody(body, listener))
                            .build()
                    } else {
                        response
                    }
                }
                .build()

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ApiException(response.code, ErrorCode.getMessage(response.code))
                }
                val body = response.body ?: throw ApiException(ErrorCode.UNKNOWN, "响应体为空")
                // 确保父目录存在
                destFile.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            destFile
        } catch (e: Throwable) {
            throw ExceptionHandler.handle(e)
        }
    }
}
