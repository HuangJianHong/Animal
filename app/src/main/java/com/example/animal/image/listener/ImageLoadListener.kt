package com.example.animal.image.listener

import android.graphics.drawable.Drawable

/**
 * 图片加载完整生命周期监听。
 *
 * 业务可只重写关心的回调（均有默认空实现）。
 */
interface ImageLoadListener {

    /** 加载开始（主线程） */
    fun onStart() {}

    /**
     * 加载进度（仅网络图片，需 OkHttp 进度集成生效）。
     * @param percent 0~100
     */
    fun onProgress(percent: Int) {}

    /**
     * 加载成功。
     * @param resource 加载到的 Drawable
     */
    fun onSuccess(resource: Drawable) {}

    /**
     * 加载失败。
     * @param errorDrawable 失败占位图（可能为空）
     * @param throwable     失败原因
     */
    fun onError(errorDrawable: Drawable?, throwable: Throwable?) {}
}
