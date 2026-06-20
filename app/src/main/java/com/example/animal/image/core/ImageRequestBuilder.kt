package com.example.animal.image.core

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.animal.image.config.ImageConfig
import com.example.animal.image.listener.GlideRequestListener
import com.example.animal.image.listener.ImageLoadListener
import com.example.animal.image.listener.ImageProgressManager
import com.example.animal.image.transform.BlurTransformation
import com.example.animal.image.transform.GrayscaleTransformation
import com.example.animal.image.transform.MaskTransformation

/**
 * 图片加载链式构建器（核心）。
 *
 * 通过 [ImageLoader.with] 创建，支持一行链式调用配置：来源、占位、形状变换、
 * 缩放、缓存策略、强制刷新、缩略图、GIF 控制、重试、监听等，最后 [into] 加载到 ImageView。
 *
 * 生命周期：[requestManager] 来自 Glide.with(view/activity/fragment)，页面销毁时自动取消请求。
 */
class ImageRequestBuilder internal constructor(
    private val requestManager: RequestManager
) {

    /** 形状变换类型 */
    private enum class ShapeType { NONE, CIRCLE, ROUNDED, GRANULAR_ROUNDED, BLUR, GRAY, MASK }

    private var source: Any? = null

    @DrawableRes
    private var placeholder: Int = ImageConfig.placeholderLoading

    @DrawableRes
    private var error: Int = ImageConfig.placeholderError

    @DrawableRes
    private var fallback: Int = ImageConfig.placeholderEmpty

    private var shapeType = ShapeType.NONE
    private var cornerRadiusPx = 0
    private var granularCorners = FloatArray(4) // tl, tr, br, bl
    private var blurRadius = ImageConfig.DEFAULT_BLUR_RADIUS
    private var blurSampling = ImageConfig.DEFAULT_BLUR_SAMPLING
    private var maskContext: Context? = null
    private var maskResId = 0

    private var scaleType = ImageConfig.defaultScaleType
    private var skipMemoryCache = false
    private var diskCacheNone = false
    private var forceRefresh = false
    private var thumbnailScale = 0f
    private var gifAutoPlay = true
    private var overrideWidth = 0
    private var overrideHeight = 0

    private var retryCount = 0
    private var retryInterval = ImageConfig.DEFAULT_RETRY_INTERVAL_MS
    private var listener: ImageLoadListener? = null
    private var duplicateCheck = true

    private val mainHandler = Handler(Looper.getMainLooper())

    // ============================ 来源 ============================
    /** 设置图片来源：url / resId / File / Uri / ByteArray / Bitmap / Drawable 等 */
    fun load(source: Any?): ImageRequestBuilder = apply { this.source = source }

    // ============================ 占位资源（可单独覆盖全局默认） ============================
    fun placeholder(@DrawableRes resId: Int): ImageRequestBuilder = apply { placeholder = resId }
    fun error(@DrawableRes resId: Int): ImageRequestBuilder = apply { error = resId }
    fun fallback(@DrawableRes resId: Int): ImageRequestBuilder = apply { fallback = resId }

    // ============================ 形状变换（一行调用） ============================
    /** 圆形（头像） */
    fun circle(): ImageRequestBuilder = apply { shapeType = ShapeType.CIRCLE }

    /** 统一圆角，单位 dp */
    fun rounded(radiusDp: Int = ImageConfig.DEFAULT_CORNER_RADIUS_DP): ImageRequestBuilder = apply {
        shapeType = ShapeType.ROUNDED
        cornerRadiusPx = dp2px(radiusDp)
    }

    /** 单边/自定义四角圆角，单位 dp（左上、右上、右下、左下） */
    fun roundedCorners(tlDp: Int, trDp: Int, brDp: Int, blDp: Int): ImageRequestBuilder = apply {
        shapeType = ShapeType.GRANULAR_ROUNDED
        granularCorners = floatArrayOf(
            dp2px(tlDp).toFloat(), dp2px(trDp).toFloat(),
            dp2px(brDp).toFloat(), dp2px(blDp).toFloat()
        )
    }

    /** 高斯模糊 */
    fun blur(
        radius: Int = ImageConfig.DEFAULT_BLUR_RADIUS,
        sampling: Int = ImageConfig.DEFAULT_BLUR_SAMPLING
    ): ImageRequestBuilder = apply {
        shapeType = ShapeType.BLUR
        blurRadius = radius
        blurSampling = sampling
    }

    /** 灰度滤镜 */
    fun gray(): ImageRequestBuilder = apply { shapeType = ShapeType.GRAY }

    /** 蒙版遮罩 */
    fun mask(context: Context, @DrawableRes maskRes: Int): ImageRequestBuilder = apply {
        shapeType = ShapeType.MASK
        maskContext = context.applicationContext
        maskResId = maskRes
    }

    // ============================ 缩放模式 ============================
    fun scaleType(type: ImageConfig.ScaleType): ImageRequestBuilder = apply { scaleType = type }
    fun centerCrop(): ImageRequestBuilder = scaleType(ImageConfig.ScaleType.CENTER_CROP)
    fun centerInside(): ImageRequestBuilder = scaleType(ImageConfig.ScaleType.CENTER_INSIDE)
    fun fitCenter(): ImageRequestBuilder = scaleType(ImageConfig.ScaleType.FIT_CENTER)

    // ============================ 缓存 / 刷新 ============================
    /** 跳过内存缓存 */
    fun skipMemoryCache(skip: Boolean = true): ImageRequestBuilder = apply { skipMemoryCache = skip }

    /** 跳过磁盘缓存 */
    fun skipDiskCache(skip: Boolean = true): ImageRequestBuilder = apply { diskCacheNone = skip }

    /** 强制刷新网络图（忽略全部缓存，重新拉取） */
    fun forceRefresh(force: Boolean = true): ImageRequestBuilder = apply { forceRefresh = force }

    // ============================ 缩略图 / 原图 ============================
    /**
     * 缩略图模式：先加载缩略图（scale 比例），再加载原图。
     * @param scale 0~1，例如 0.25 表示先加载 1/4 分辨率缩略图
     */
    fun thumbnail(scale: Float = 0.25f): ImageRequestBuilder = apply { thumbnailScale = scale }

    // ============================ GIF ============================
    /** 是否自动播放 GIF（false 则仅显示第一帧静态图） */
    fun gifAutoPlay(autoPlay: Boolean): ImageRequestBuilder = apply { gifAutoPlay = autoPlay }

    // ============================ 尺寸 ============================
    /** 指定目标采样尺寸（大图采样压缩，避免 OOM） */
    fun override(width: Int, height: Int): ImageRequestBuilder = apply {
        overrideWidth = width
        overrideHeight = height
    }

    // ============================ 重试 / 监听 ============================
    /** 失败重试次数与间隔 */
    fun retry(
        count: Int = ImageConfig.DEFAULT_RETRY_COUNT,
        intervalMs: Long = ImageConfig.DEFAULT_RETRY_INTERVAL_MS
    ): ImageRequestBuilder = apply {
        retryCount = count
        retryInterval = intervalMs
    }

    /** 加载监听（开始/进度/成功/失败） */
    fun listener(l: ImageLoadListener?): ImageRequestBuilder = apply { listener = l }

    /** 关闭防重复加载检测 */
    fun noDuplicateCheck(): ImageRequestBuilder = apply { duplicateCheck = false }

    // ============================ 执行加载 ============================
    /**
     * 加载到目标 ImageView。
     */
    fun into(view: ImageView) {
        val urlKey = source as? String

        // 防重复加载拦截
        if (duplicateCheck && urlKey != null && !DuplicateLoadGuard.shouldLoad(view, urlKey)) {
            return
        }

        // 注册网络进度监听
        if (urlKey != null && listener != null) {
            ImageProgressManager.addListener(urlKey) { percent -> listener?.onProgress(percent) }
        }
        listener?.onStart()

        executeInto(view, urlKey, attempt = 0)
    }

    /** 仅预加载到缓存，不显示 */
    fun preload() {
        buildRequest().preload()
    }

    /** 实际执行（含重试递归） */
    private fun executeInto(view: ImageView, urlKey: String?, attempt: Int) {
        val request = buildRequest().listener(
            GlideRequestListener(
                url = urlKey,
                listener = listener,
                onFailed = {
                    // 还有重试次数则延迟重试，并标记为已处理（不回调 onError）
                    if (attempt < retryCount) {
                        mainHandler.postDelayed({
                            executeInto(view, urlKey, attempt + 1)
                        }, retryInterval)
                        true
                    } else {
                        false
                    }
                }
            )
        )
        request.into(view)
    }

    /** 构建带全部配置的 RequestBuilder */
    private fun buildRequest(): RequestBuilder<android.graphics.drawable.Drawable> {
        val options = buildOptions()
        var builder = requestManager.load(source).apply(options)
        // 缩略图模式
        if (thumbnailScale in 0.01f..0.99f) {
            builder = builder.thumbnail(thumbnailScale)
        }
        return builder
    }

    /** 组装 RequestOptions */
    private fun buildOptions(): RequestOptions {
        var options = RequestOptions()
            .placeholder(placeholder)
            .error(error)
            .fallback(fallback)

        // 尺寸采样
        if (overrideWidth > 0 && overrideHeight > 0) {
            options = options.override(overrideWidth, overrideHeight)
        }

        // 缓存策略
        if (skipMemoryCache || forceRefresh) options = options.skipMemoryCache(true)
        options = when {
            diskCacheNone || forceRefresh -> options.diskCacheStrategy(DiskCacheStrategy.NONE)
            else -> options.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        }
        // 强制刷新：变更签名使缓存失效
        if (forceRefresh) {
            options = options.signature(ObjectKey(System.currentTimeMillis().toString()))
        }
        // GIF 不自动播放：仅显示首帧
        if (!gifAutoPlay) options = options.dontAnimate()

        // 组合变换：缩放 + 形状
        val transformations = mutableListOf<Transformation<Bitmap>>()
        when (scaleType) {
            ImageConfig.ScaleType.CENTER_CROP -> transformations.add(CenterCrop())
            ImageConfig.ScaleType.CENTER_INSIDE -> transformations.add(CenterInside())
            ImageConfig.ScaleType.FIT_CENTER -> transformations.add(FitCenter())
            ImageConfig.ScaleType.NONE -> {}
        }
        when (shapeType) {
            ShapeType.CIRCLE -> transformations.add(CircleCrop())
            ShapeType.ROUNDED -> transformations.add(RoundedCorners(cornerRadiusPx))
            ShapeType.GRANULAR_ROUNDED -> transformations.add(
                GranularRoundedCorners(
                    granularCorners[0], granularCorners[1],
                    granularCorners[2], granularCorners[3]
                )
            )
            ShapeType.BLUR -> transformations.add(BlurTransformation(blurRadius, blurSampling))
            ShapeType.GRAY -> transformations.add(GrayscaleTransformation())
            ShapeType.MASK -> maskContext?.let {
                transformations.add(MaskTransformation(it, maskResId))
            }
            ShapeType.NONE -> {}
        }
        if (transformations.isNotEmpty()) {
            options = options.transform(MultiTransformation(transformations))
        }
        return options
    }

    /** dp 转 px */
    private fun dp2px(dp: Int): Int =
        (dp * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
}
