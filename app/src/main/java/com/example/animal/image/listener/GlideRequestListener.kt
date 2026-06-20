package com.example.animal.image.listener

import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * 桥接 Glide 原生 [RequestListener] 与业务 [ImageLoadListener]。
 *
 * 同时负责：成功/失败回调分发、清理进度监听、把失败交给重试逻辑处理。
 *
 * @param url          网络图片地址（用于清理进度监听）
 * @param listener     业务监听
 * @param onFailed     失败处理回调，返回 true 表示已处理（如已安排重试），不再回调 onError
 */
class GlideRequestListener(
    private val url: String?,
    private val listener: ImageLoadListener?,
    private val onFailed: ((Throwable?) -> Boolean)? = null
) : RequestListener<Drawable> {

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable>,
        isFirstResource: Boolean
    ): Boolean {
        url?.let { ImageProgressManager.removeListener(it) }
        val handled = onFailed?.invoke(e) ?: false
        if (!handled) {
            listener?.onError(null, e)
        }
        // 返回 false：交还 Glide 走默认 error 占位逻辑
        return false
    }

    override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable>,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        url?.let { ImageProgressManager.removeListener(it) }
        listener?.onSuccess(resource)
        return false
    }
}
