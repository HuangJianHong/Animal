package com.example.animal.net.entity

import com.example.animal.net.exception.ApiException

/**
 * 请求结果密封类，用于回调式（非协程）请求的统一结果包装。
 *
 * 协程方式可直接 try/catch [ApiException]；回调方式则通过本类区分成功/失败。
 */
sealed class ApiResult<out T> {

    /** 成功，携带数据 */
    data class Success<T>(val data: T) : ApiResult<T>()

    /** 失败，携带统一异常 */
    data class Error(val exception: ApiException) : ApiResult<Nothing>()

    /** 便捷处理：成功回调 */
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    /** 便捷处理：失败回调 */
    inline fun onError(action: (ApiException) -> Unit): ApiResult<T> {
        if (this is Error) action(exception)
        return this
    }
}
