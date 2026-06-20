package com.example.animal.demo

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.animal.R
import com.example.animal.image.cache.ImageCacheManager
import com.example.animal.image.core.ImageLoader
import com.example.animal.image.core.ImagePermissionHelper
import com.example.animal.image.core.RecyclerViewPauseHelper
import com.example.animal.image.listener.ImageLoadListener
import java.io.File

/**
 * Glide 图片框架多场景调用示例集合。
 *
 * 仅作演示用途，展示各业务场景的标准用法。
 */
object ImageLoadDemo {

    private const val DEMO_URL = "https://www.example.com/avatar.jpg"
    private const val DEMO_GIF = "https://www.example.com/loading.gif"

    /** 场景 1：圆形头像 */
    fun avatar(imageView: ImageView, url: String) {
        ImageLoader.loadCircle(imageView, url)
    }

    /** 场景 2：圆角图片（自定义占位、12dp 圆角） */
    fun roundedWithPlaceholder(imageView: ImageView, url: String) {
        ImageLoader.with(imageView)
            .load(url)
            .rounded(12)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .centerCrop()
            .into(imageView)
    }

    /** 场景 3：仅顶部两个圆角（卡片头图） */
    fun topRoundedCard(imageView: ImageView, url: String) {
        ImageLoader.with(imageView)
            .load(url)
            .roundedCorners(tlDp = 12, trDp = 12, brDp = 0, blDp = 0)
            .into(imageView)
    }

    /** 场景 4：列表图片（缩略图优先 + 失败重试 2 次） */
    fun listItem(imageView: ImageView, url: String) {
        ImageLoader.with(imageView)
            .load(url)
            .rounded(8)
            .thumbnail(0.25f)
            .retry(count = 2, intervalMs = 1000L)
            .into(imageView)
    }

    /** 场景 5：大图预览（采样压缩 + 完整加载监听 + 进度） */
    fun bigImagePreview(imageView: ImageView, url: String, onPercent: (Int) -> Unit) {
        ImageLoader.with(imageView)
            .load(url)
            .override(2048, 2048) // 大图采样，避免 OOM
            .fitCenter()
            .listener(object : ImageLoadListener {
                override fun onStart() { /* 显示 Loading */ }
                override fun onProgress(percent: Int) = onPercent(percent)
                override fun onSuccess(resource: Drawable) { /* 隐藏 Loading */ }
                override fun onError(errorDrawable: Drawable?, throwable: Throwable?) { /* 提示失败 */ }
            })
            .into(imageView)
    }

    /** 场景 6：GIF 动图（控制是否自动播放） */
    fun gif(imageView: ImageView, autoPlay: Boolean) {
        ImageLoader.with(imageView)
            .load(DEMO_GIF)
            .gifAutoPlay(autoPlay)
            .into(imageView)
    }

    /** 场景 7：高斯模糊背景 */
    fun blurBackground(imageView: ImageView, url: String) {
        ImageLoader.loadBlur(imageView, url, radius = 25)
    }

    /** 场景 8：本地文件 / Content Uri / 资源 / Base64 */
    fun localSources(imageView: ImageView, file: File, uri: Uri, base64: String) {
        ImageLoader.loadFile(imageView, file)
        ImageLoader.loadUri(imageView, uri)
        ImageLoader.loadRes(imageView, R.drawable.ic_image_empty)
        ImageLoader.loadBase64(imageView, base64)
    }

    /** 场景 9：强制刷新网络图（忽略缓存，重新拉取） */
    fun forceRefresh(imageView: ImageView, url: String) {
        ImageLoader.with(imageView)
            .load(url)
            .forceRefresh()
            .into(imageView)
    }

    /** 场景 10：预加载列表图片到缓存 */
    fun preloadList(context: Context, urls: List<String>) {
        ImageLoader.preload(context, urls)
    }

    /** 场景 11：下载原图到本地（带进度） */
    fun downloadOriginal(context: Context, url: String) {
        val dest = File(context.getExternalFilesDir(null), "download/origin.jpg")
        ImageLoader.download(
            context = context,
            url = url,
            destFile = dest,
            onProgress = { percent -> /* 更新下载进度条 */ },
            onSuccess = { savedFile -> /* 提示保存成功 savedFile.absolutePath */ },
            onError = { /* 提示下载失败 */ }
        )
    }

    /** 场景 12：相册图片（先做权限检查） */
    fun loadFromGallery(context: Context, imageView: ImageView, uri: Uri) {
        ImagePermissionHelper.checkBeforeLoad(
            context = context,
            onGranted = { ImageLoader.loadUri(imageView, uri) },
            onDenied = { permission -> /* 提示去申请 permission */ }
        )
    }

    /** 场景 13：RecyclerView 滑动暂停加载，提升帧率 */
    fun optimizeList(recyclerView: RecyclerView) {
        RecyclerViewPauseHelper.attach(recyclerView)
    }

    /** 场景 14：缓存管理 */
    fun cacheManage(context: Context) {
        // 获取缓存大小
        ImageCacheManager.getFormattedCacheSize(context) { sizeStr -> /* 展示 sizeStr */ }
        // 判断是否已缓存
        ImageCacheManager.isCached(context, DEMO_URL) { cached -> /* cached */ }
        // 一键清理全部缓存
        ImageCacheManager.clearAll(context) { /* 清理完成提示 */ }
    }
}
