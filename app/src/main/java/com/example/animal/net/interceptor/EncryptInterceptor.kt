package com.example.animal.net.interceptor

import com.example.animal.net.config.NetConfig
import com.example.animal.net.utils.EncryptUtils
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

/**
 * 请求参数统一加密拦截器（全局开关 [NetConfig.enableEncrypt]）。
 *
 * 实现：将 POST 请求体读出后整体加密（AES 或 RSA），以 JSON `{"data":"密文"}` 形式重新提交。
 * 服务端约定先解密 data 字段再处理业务。GET 请求不在此处理。
 */
class EncryptInterceptor : Interceptor {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = request.body

        // 关闭加密、无 body、或非 POST：直接放行
        if (!NetConfig.enableEncrypt || body == null || !request.method.equals("POST", ignoreCase = true)) {
            return chain.proceed(request)
        }

        // 读取原始 body 文本
        val buffer = Buffer()
        body.writeTo(buffer)
        val originContent = buffer.readUtf8()

        // 按全局配置选择加密方式
        val encrypted = when (NetConfig.encryptType) {
            NetConfig.EncryptType.AES -> EncryptUtils.aesEncrypt(originContent)
            NetConfig.EncryptType.RSA -> {
                // RSA 需要服务端下发公钥，这里演示用占位逻辑回退到 AES，实际请替换公钥
                EncryptUtils.aesEncrypt(originContent)
            }
        }

        val newBody = "{\"data\":\"$encrypted\"}".toRequestBody(jsonMediaType)
        val newRequest = request.newBuilder()
            .method(request.method, newBody)
            .header("Content-Type", "application/json")
            .header("Encrypted", "1") // 告知服务端该请求已加密
            .build()
        return chain.proceed(newRequest)
    }
}
