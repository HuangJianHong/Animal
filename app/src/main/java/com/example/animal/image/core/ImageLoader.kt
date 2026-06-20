package com.example.animal.image.core

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.example.animal.image.config.ImageConfig
import com.example.animal.image.listener.ImageProgressManager
import java.io.File
import java.util.concurrent.Executors

/**
 * 图片加载统一入口（单例）。
 *
 * - [with] 创建链式构建器，自动绑定调用方生命周期；
 * - 提供头像圆形、圆角、模糊、灰度等一行式便捷方法；
 * - 提供预加载、下载原图（带进度）、清理请求等能力。
 *
 * 所有来源类型均支持：网络 url、drawable/mipmap、File、Content Uri、Bitmap 二进制(ByteArray)、Base64。
 */
object ImageLoader {

    private val ioExecutor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    // ============================ 链式入口（自动生命周期绑定） ============================
    /** 绑定 ImageView 所在生命周期 */
    fun with(view: ImageView): ImageRequestBuilder = ImageRequestBuilder(Glide.with(view))

    /** 绑定 Activity 生命周期 */
    fun with(activity: Activity): ImageRequestBuilder = ImageRequestBuilder(Glide.with(activity))

    /** 绑定 Fragment 生命周期 */
    fun with(fragment: Fragment): ImageRequestBuilder = ImageRequestBuilder(Glide.with(fragment))

    /** 绑定任意 View 生命周期 */
    fun with(view: View): ImageRequestBuilder = ImageRequestBuilder(Glide.with(view))

    /** 使用 ApplicationContext（无生命周期，慎用于会销毁的页面） */
    fun with(context: Context): ImageRequestBuilder = ImageRequestBuilder(Glide.with(context))

    // ============================ 便捷加载方法 ============================
    /** 普通加载（自动使用全局默认占位与缩放） */
    fun load(view: ImageView, source: Any?) {
        with(view).load(source).into(view)
    }

    /** 圆形头像 */
    fun loadCircle(view: ImageView, source: Any?) {
        with(view).load(source).circle().into(view)
    }

    /** 圆角图片，radiusDp 单位 dp */
    fun loadRounded(view: ImageView, source: Any?, radiusDp: Int = ImageConfig.DEFAULT_CORNER_RADIUS_DP) {
        with(view).load(source).rounded(radiusDp).into(view)
    }

    /** 高斯模糊 */
    fun loadBlur(view: ImageView, source: Any?, radius: Int = ImageConfig.DEFAULT_BLUR_RADIUS) {
        with(view).load(source).blur(radius).into(view)
    }

    /** 灰度 */
    fun loadGray(view: ImageView, source: Any?) {
        with(view).load(source).gray().into(view)
    }

    /** 加载 Base64 图片 */
    fun loadBase64(view: ImageView, base64: String) {
        val pureBase64 = base64.substringAfter("base64,", base64) // 兼容 data:image/png;base64, 前缀
        val bytes = runCatching { Base64.decode(pureBase64, Base64.DEFAULT) }.getOrNull()
        with(view).load(bytes).into(view)
    }

    /** 加载本地 File */
    fun loadFile(view: ImageView, file: File) = load(view, file)

    /** 加载 Content Uri（相册/分区存储） */
    fun loadUri(view: ImageView, uri: Uri) = load(view, uri)

    /** 加载本地资源（drawable/mipmap） */
    fun loadRes(view: ImageView, @DrawableRes resId: Int) = load(view, resId)

    // ============================ 预加载 ============================
    /** 预加载网络图片到缓存（提前缓存列表图，优化打开速度） */
    fun preload(context: Context, url: String) {
        Glide.with(context.applicationContext).load(url).preload()
    }

    /** 批量预加载 */
    fun preload(context: Context, urls: List<String>) {
        urls.forEach { preload(context, it) }
    }

    // ============================ 下载原图（带进度回调） ============================
    /**
     * 下载原图到指定文件，带下载进度回调。
     *
     * @param onProgress 进度回调（主线程，0~100）
     * @param onSuccess  成功回调（主线程），返回保存后的文件
     * @param onError    失败回调（主线程）
     */
    fun download(
        context: Context,
        url: String,
        destFile: File,
        onProgress: (Int) -> Unit = {},
        onSuccess: (File) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val appContext = context.applicationContext
        // 注册进度监听（主线程回调）
        ImageProgressManager.addListener(url, onProgress)
        ioExecutor.execute {
            var future: FutureTarget<File>? = null
            try {
                future = Glide.with(appContext).downloadOnly().load(url).submit()
                val cacheFile = future.get()
                // 拷贝缓存文件到目标路径
                destFile.parentFile?.mkdirs()
                cacheFile.copyTo(destFile, overwrite = true)
                mainHandler.post { onSuccess(destFile) }
            } catch (e: Throwable) {
                mainHandler.post { onError(e) }
            } finally {
                ImageProgressManager.removeListener(url)
                future?.let { Glide.with(appContext).clear(it) }
            }
        }
    }

    // ============================ 取消 / 清理请求 ============================
    /** 取消并清理某个 ImageView 的加载请求（页面销毁可手动调用，正常由生命周期自动处理） */
    fun clear(view: ImageView) {
        Glide.with(view).clear(view)
    }

    /** 暂停某宿主的全部加载（如列表滑动时） */
    fun pauseRequests(context: Context) {
        Glide.with(context.applicationContext).pauseRequests()
    }

    /** 恢复某宿主的全部加载 */
    fun resumeRequests(context: Context) {
        Glide.with(context.applicationContext).resumeRequests()
    }
}
