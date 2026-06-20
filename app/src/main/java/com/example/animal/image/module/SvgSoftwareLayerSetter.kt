package com.example.animal.image.module

import android.graphics.drawable.PictureDrawable
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target

/**
 * SVG 渲染辅助：PictureDrawable 不支持硬件加速，
 * 加载 SVG 时需将目标 ImageView 切换为软件渲染层，否则可能显示异常。
 *
 * 加载 SVG 时把本监听器加入即可。
 */
class SvgSoftwareLayerSetter : RequestListener<PictureDrawable> {

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<PictureDrawable>,
        isFirstResource: Boolean
    ): Boolean {
        (target as? ImageViewTarget<*>)?.view?.setLayerType(ImageView.LAYER_TYPE_NONE, null)
        return false
    }

    override fun onResourceReady(
        resource: PictureDrawable,
        model: Any,
        target: Target<PictureDrawable>,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        (target as? ImageViewTarget<*>)?.view?.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null)
        return false
    }
}
