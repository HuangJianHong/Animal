package com.example.animal.net.entity

import com.example.animal.net.exception.ApiException
import com.example.animal.net.exception.ErrorCode

/**
 * 全局统一后端返回基类。
 *
 * 约定返回结构：
 * ```json
 * { "code": 200, "msg": "success", "data": { ... } }
 * ```
 *
 * @param T data 数据体泛型
 */
data class ResponseBean<T>(
    /** 业务状态码 */
    val code: Int = ErrorCode.UNKNOWN,
    /** 提示信息（兼容 msg / message 两种字段名） */
    val msg: String? = null,
    val message: String? = null,
    /** 数据体 */
    val data: T? = null
) {

    /** 是否业务成功 */
    fun isSuccess(): Boolean = code == ErrorCode.SUCCESS

    /** 统一取提示文案 */
    fun obtainMsg(): String = msg ?: message ?: ErrorCode.getMessage(code)

    /**
     * 解包：成功返回 data，失败抛出 [ApiException]。
     * 框架内部统一调用，业务层拿到的就是纯 data。
     */
    fun parseData(): T {
        if (!isSuccess()) {
            throw ApiException(code, obtainMsg())
        }
        // data 为空时返回 Unit 兼容无 data 的接口；否则抛解析异常
        @Suppress("UNCHECKED_CAST")
        return data ?: (Unit as T)
    }
}
