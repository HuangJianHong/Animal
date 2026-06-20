package com.example.animal.demo

import com.example.animal.demo.model.LoginRequest
import com.example.animal.demo.model.LoginResponse
import com.example.animal.demo.model.UploadResult
import com.example.animal.demo.model.UserInfo
import com.example.animal.net.api.DemoApiService
import com.example.animal.net.core.HttpManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 数据仓库层：封装具体接口调用，向 ViewModel 暴露 suspend 函数。
 *
 * 所有方法走 [HttpManager.request]，自动 IO 线程 + 解包 + 异常转换，
 * 返回的是纯 data，失败则抛 [com.example.animal.net.exception.ApiException]。
 */
class DemoRepository {

    private val api: DemoApiService = HttpManager.api(DemoApiService::class.java)

    /** 登录 */
    suspend fun login(username: String, password: String): LoginResponse =
        HttpManager.request { api.login(LoginRequest(username, password)) }

    /** 获取用户信息 */
    suspend fun getUserInfo(userId: String): UserInfo =
        HttpManager.request { api.getUserInfo(userId) }

    /** 单文件上传 */
    suspend fun uploadFile(file: File): UploadResult {
        val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        return HttpManager.request { api.uploadFile(part) }
    }

    /** 多文件上传 */
    suspend fun uploadFiles(files: List<File>): List<UploadResult> {
        val parts = files.map { file ->
            val body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("files", file.name, body)
        }
        return HttpManager.request { api.uploadFiles(parts) }
    }
}
