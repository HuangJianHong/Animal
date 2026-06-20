package com.example.animal.image.cache

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.example.animal.image.config.ImageConfig
import java.io.File
import java.math.BigDecimal
import java.util.concurrent.Executors

/**
 * 图片缓存管理工具。
 *
 * 提供：一键清理内存缓存、一键清理磁盘缓存、获取缓存大小、判断图片是否已缓存、低内存释放等。
 */
object ImageCacheManager {

    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 清理内存缓存（必须在主线程调用）。
     */
    fun clearMemoryCache(context: Context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Glide.get(context).clearMemory()
        } else {
            mainHandler.post { Glide.get(context).clearMemory() }
        }
    }

    /**
     * 清理磁盘缓存（必须在子线程调用，内部已切线程）。
     * @param onComplete 清理完成回调（主线程）
     */
    fun clearDiskCache(context: Context, onComplete: (() -> Unit)? = null) {
        val appContext = context.applicationContext
        ioExecutor.execute {
            Glide.get(appContext).clearDiskCache()
            onComplete?.let { mainHandler.post(it) }
        }
    }

    /**
     * 一键清理全部缓存（内存 + 磁盘）。
     */
    fun clearAll(context: Context, onComplete: (() -> Unit)? = null) {
        clearMemoryCache(context)
        clearDiskCache(context, onComplete)
    }

    /**
     * 低内存时主动释放：根据系统回调等级裁剪缓存。
     * @param level ComponentCallbacks2 的 level
     */
    fun trimMemory(context: Context, level: Int) {
        Glide.get(context).trimMemory(level)
    }

    /**
     * 获取磁盘缓存目录大小（字节）。子线程计算，主线程回调。
     */
    fun getDiskCacheSize(context: Context, callback: (Long) -> Unit) {
        val appContext = context.applicationContext
        ioExecutor.execute {
            val dir = File(appContext.cacheDir, ImageConfig.DISK_CACHE_DIR)
            val size = folderSize(dir)
            mainHandler.post { callback(size) }
        }
    }

    /**
     * 获取格式化后的缓存大小字符串（如 "12.34MB"）。
     */
    fun getFormattedCacheSize(context: Context, callback: (String) -> Unit) {
        getDiskCacheSize(context) { size -> callback(formatSize(size)) }
    }

    /**
     * 判断某张网络图片是否已存在磁盘缓存。子线程查询，主线程回调。
     *
     * 原理：使用 onlyRetrieveFromCache 的 downloadOnly，命中则返回文件，否则抛异常。
     */
    fun isCached(context: Context, url: String, callback: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        ioExecutor.execute {
            var future: FutureTarget<File>? = null
            val cached = try {
                future = Glide.with(appContext)
                    .downloadOnly()
                    .load(url)
                    .onlyRetrieveFromCache(true)
                    .submit()
                future.get() != null
            } catch (e: Exception) {
                false
            } finally {
                future?.let { Glide.with(appContext).clear(it) }
            }
            mainHandler.post { callback(cached) }
        }
    }

    /** 递归计算文件夹大小 */
    private fun folderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { folderSize(it) } ?: 0L
    }

    /** 字节大小格式化 */
    private fun formatSize(size: Long): String {
        if (size <= 0) return "0B"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            size >= gb -> "${BigDecimal(size / gb).setScale(2, BigDecimal.ROUND_HALF_UP)}GB"
            size >= mb -> "${BigDecimal(size / mb).setScale(2, BigDecimal.ROUND_HALF_UP)}MB"
            size >= kb -> "${BigDecimal(size / kb).setScale(2, BigDecimal.ROUND_HALF_UP)}KB"
            else -> "${size}B"
        }
    }
}
