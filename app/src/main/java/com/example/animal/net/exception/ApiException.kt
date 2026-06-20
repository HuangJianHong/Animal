package com.example.animal.net.exception

/**
 * 统一自定义网络异常。
 *
 * 框架内所有网络错误最终都会被转换成 [ApiException]，业务层只需捕获它即可，
 * 通过 [code] 判断错误类型、通过 [message] 展示提示。
 *
 * @param code     错误码（见 [ErrorCode]）
 * @param errorMsg 错误提示文案
 * @param cause    原始异常（便于排查）
 */
class ApiException(
    val code: Int,
    val errorMsg: String,
    cause: Throwable? = null
) : Exception(errorMsg, cause) {

    /** 是否为登录失效 */
    val isTokenExpired: Boolean get() = code == ErrorCode.UNAUTHORIZED

    /** 是否为无网络 */
    val isNoNetwork: Boolean get() = code == ErrorCode.NO_NETWORK

    override fun toString(): String = "ApiException(code=$code, msg=$errorMsg)"
}
