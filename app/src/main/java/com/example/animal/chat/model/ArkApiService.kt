package com.example.animal.chat.model

import com.example.animal.chat.entity.ArkRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * 火山方舟 responses 接口（SSE 流式）Retrofit 定义。
 *
 * 复用项目全局 Retrofit/OkHttp（见 RetrofitClient），不自行创建网络实例。
 *
 * 说明：
 * - 返回 [Call] 而非 suspend，便于在 Repository 中持有 Call、在协程取消时主动 cancel 中断长连接；
 * - @Streaming 告知 Retrofit 不缓冲整个响应体，按流读取；
 * - Authorization 通过 @Header 传入（Bearer {ARK_API_KEY}）；
 * - Accept 声明接收 SSE 流式数据；Content-Type 由 Gson 转换器自动设为 application/json。
 */
interface ArkApiService {

    @Streaming
    @Headers("Accept: text/event-stream")
    @POST("responses")
    fun createResponseStream(
        @Header("Authorization") authorization: String,
        @Body request: ArkRequest
    ): Call<ResponseBody>
}
