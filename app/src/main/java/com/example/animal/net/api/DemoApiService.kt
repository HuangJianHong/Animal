package com.example.animal.net.api

import com.example.animal.demo.model.LoginRequest
import com.example.animal.demo.model.LoginResponse
import com.example.animal.demo.model.UploadResult
import com.example.animal.demo.model.UserInfo
import com.example.animal.net.config.NetConstants
import com.example.animal.net.entity.ResponseBean
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * 示例业务 Api 接口，演示框架支持的全部请求方式。
 *
 * 所有方法均为 suspend 挂起函数，配合 [com.example.animal.net.core.HttpManager.request] 使用，
 * 自动完成线程切换与统一解包。
 */
interface DemoApiService : BaseApi {

    /** 1. 普通 GET 请求（Query 参数） */
    @GET("user/info")
    suspend fun getUserInfo(@Query("userId") userId: String): ResponseBean<UserInfo>

    /** 2. 普通 POST JSON 请求（@Body） */
    @POST("user/login")
    suspend fun login(@Body request: LoginRequest): ResponseBean<LoginResponse>

    /** 3. Form 表单提交 */
    @FormUrlEncoded
    @POST("user/feedback")
    suspend fun feedback(
        @Field("content") content: String,
        @Field("contact") contact: String
    ): ResponseBean<Unit>

    /** 4. 单文件上传（Multipart） */
    @Multipart
    @POST("file/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): ResponseBean<UploadResult>

    /** 5. 多文件上传（Multipart） */
    @Multipart
    @POST("file/uploadMulti")
    suspend fun uploadFiles(@Part files: List<MultipartBody.Part>): ResponseBean<List<UploadResult>>

    /** 6. 带额外表单字段的文件上传 */
    @Multipart
    @POST("file/uploadWithDesc")
    suspend fun uploadWithDesc(
        @Part file: MultipartBody.Part,
        @Part("desc") desc: RequestBody
    ): ResponseBean<UploadResult>

    /**
     * 7. 指定其它域名的请求（多 BaseUrl 演示）。
     * 通过 Header 声明使用名为 "file" 的域名（需提前 BaseUrlManager.registerUrl 注册）。
     */
    @Headers("${NetConstants.HEADER_BASE_URL}: file")
    @GET("upload/token")
    suspend fun getUploadToken(): ResponseBean<String>
}
