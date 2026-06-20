package com.example.animal.image.module

import android.content.Context
import android.graphics.drawable.PictureDrawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.caverock.androidsvg.SVG
import com.example.animal.image.config.ImageConfig
import com.example.animal.image.listener.ImageProgressManager
import java.io.InputStream

/**
 * 自定义 Glide 全局配置 Module。
 *
 * 标注 [GlideModule] 后，kapt 注解处理器会生成 GeneratedAppGlideModule，
 * 应用启动时自动应用本类的全部配置（无需手动初始化）。
 *
 * 统一配置：内存/磁盘缓存、默认占位与解码格式、日志级别、OkHttp 进度集成、SVG 解析。
 */
@GlideModule
class AppGlideModuleImpl : AppGlideModule() {

    /**
     * 全局选项配置。
     */
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // 1. 内存缓存 & BitmapPool（按屏数自适应，低内存时由系统回调自动释放）
        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(ImageConfig.MEMORY_CACHE_SCREENS)
            .setBitmapPoolScreens(ImageConfig.BITMAP_POOL_SCREENS)
            .build()
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
        builder.setBitmapPool(LruBitmapPool(calculator.bitmapPoolSize.toLong()))

        // 2. 磁盘缓存：自定义目录 + 最大值（Debug/Release 差异化大小见 ImageConfig）
        builder.setDiskCache(
            InternalCacheDiskCacheFactory(
                context,
                ImageConfig.DISK_CACHE_DIR,
                ImageConfig.diskCacheMaxSize
            )
        )

        // 3. 全局默认请求参数：解码格式（预压缩）、硬件解码、磁盘缓存策略、统一占位
        val defaultOptions = RequestOptions().apply {
            // 预压缩：RGB_565 省一半内存，降低 OOM
            format(if (ImageConfig.enablePreCompress) DecodeFormat.PREFER_RGB_565 else DecodeFormat.PREFER_ARGB_8888)
            // 硬件解码开关（关闭则强制软件解码）
            if (!ImageConfig.enableHardwareDecode) disallowHardwareConfig()
            // 智能磁盘缓存策略：自动区分原图/缩略图缓存
            diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            // 全局统一占位
            placeholder(ImageConfig.placeholderLoading)
            error(ImageConfig.placeholderError)
            fallback(ImageConfig.placeholderEmpty)
        }
        builder.setDefaultRequestOptions(defaultOptions)

        // 4. 日志级别：Debug 详细日志，Release 仅错误
        builder.setLogLevel(if (ImageConfig.isDebug) Log.VERBOSE else Log.ERROR)
    }

    /**
     * 注册自定义组件。
     */
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // 1. 用带进度拦截器的 OkHttpClient 替换默认网络栈，支持图片加载进度回调
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(ImageProgressManager.okHttpClient)
        )

        // 2. 注册 SVG 解析链：InputStream -> SVG -> PictureDrawable
        registry.register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }

    /**
     * 关闭 manifest 解析（已用注解方式注册，提升初始化速度）。
     */
    override fun isManifestParsingEnabled(): Boolean = false
}
