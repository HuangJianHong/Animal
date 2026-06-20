package com.example.animal.net.core

import com.example.animal.net.entity.ResponseBean
import com.example.animal.net.exception.ApiException
import com.example.animal.net.exception.ErrorCode
import com.example.animal.net.exception.ExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 回调式请求启动器（无需手动处理协程/线程）。
 *
 * 直接在 `viewModelScope` 或 `lifecycleScope` 上调用，框架自动完成：
 * - IO 线程发起请求、主线程回调结果；
 * - 自动展示/隐藏 Loading；
 * - 请求防重复；
 * - 页面销毁时随 scope 自动取消，杜绝销毁后回调空指针 / 内存泄漏。
 *
 * @param requestKey 防重复唯一标识，传 null 则不去重
 * @param loading    加载弹窗（可空），非空则自动 show/dismiss
 * @param loadingMsg Loading 文案
 * @param onStart    请求开始（主线程）
 * @param onSuccess  成功回调，返回纯 data（主线程）
 * @param onError    失败回调（主线程），默认不处理
 * @param onComplete 结束回调（无论成功失败，主线程）
 * @param block      实际请求（suspend，返回 ResponseBean）
 */
fun <T> CoroutineScope.launchHttp(
    requestKey: String? = null,
    loading: ILoadingView? = null,
    loadingMsg: String? = null,
    onStart: () -> Unit = {},
    onSuccess: (T) -> Unit,
    onError: (ApiException) -> Unit = {},
    onComplete: () -> Unit = {},
    block: suspend () -> ResponseBean<T>
): Job {
    // 防重复请求拦截
    if (requestKey != null && !DuplicateGuard.tryAcquire(requestKey)) {
        onError(ApiException(ErrorCode.DUPLICATE_REQUEST, ErrorCode.getMessage(ErrorCode.DUPLICATE_REQUEST)))
        return Job().apply { complete() }
    }

    return launch(Dispatchers.Main) {
        onStart()
        loading?.show(loadingMsg)
        try {
            // HttpManager.request 内部已切 IO 并解包
            val data = HttpManager.request(block)
            onSuccess(data)
        } catch (e: ApiException) {
            onError(e)
        } catch (e: Throwable) {
            onError(ExceptionHandler.handle(e))
        } finally {
            loading?.dismiss()
            onComplete()
            requestKey?.let { DuplicateGuard.release(it) }
        }
    }
}

/**
 * 文件下载回调式封装（带进度，进度回调在主线程）。
 */
fun CoroutineScope.launchDownload(
    url: String,
    destFile: java.io.File,
    onProgress: (percent: Int) -> Unit = {},
    onSuccess: (java.io.File) -> Unit,
    onError: (ApiException) -> Unit = {}
): Job = launch(Dispatchers.Main) {
    try {
        val file = HttpManager.download(url, destFile) { _, _, percent, _ ->
            // 进度回调切回主线程
            launch(Dispatchers.Main) { onProgress(percent) }
        }
        onSuccess(file)
    } catch (e: ApiException) {
        onError(e)
    } catch (e: Throwable) {
        onError(ExceptionHandler.handle(e))
    }
}
