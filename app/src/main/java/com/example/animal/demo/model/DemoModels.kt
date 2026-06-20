package com.example.animal.demo.model

/**
 * 示例数据模型，用于演示网络框架的请求/响应解析。
 */

/** 登录请求参数 */
data class LoginRequest(
    val username: String,
    val password: String
)

/** 登录返回数据 */
data class LoginResponse(
    val token: String,
    val userId: String
)

/** 用户信息 */
data class UserInfo(
    val userId: String,
    val nickname: String,
    val avatar: String?,
    val age: Int
)

/** 文件上传返回 */
data class UploadResult(
    val url: String,
    val size: Long
)
