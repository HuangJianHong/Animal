package com.example.animal.image.config

import androidx.annotation.DrawableRes
import com.example.animal.BuildConfig
import com.example.animal.R

/**
 * 图片加载框架全局配置中心。
 *
 * 规范：所有占位资源 ID、缓存数值、默认参数一律在此集中管理，
 * 业务代码与框架内部严禁硬编码资源 ID 或缓存大小。
 */
object ImageConfig {

    /** 是否 Debug 环境（控制日志开关、缓存路径可见等） */
    val isDebug: Boolean = BuildConfig.DEBUG

    // ============================ 缓存配置 ============================
    /** 磁盘缓存目录名 */
    const val DISK_CACHE_DIR = "glide_image_cache"

    /**
     * 磁盘缓存最大值（字节）。
     * Debug 给大一些方便调试（256MB），Release 限制为 100MB。
     */
    val diskCacheMaxSize: Long =
        if (BuildConfig.DEBUG) 256L * 1024 * 1024 else 100L * 1024 * 1024

    /**
     * 内存缓存占最大可用内存的比例（0~1）。
     * 0.2 表示使用 20% 可用内存做图片内存缓存，兼顾流畅与防 OOM。
     */
    const val MEMORY_CACHE_SCREENS = 2f

    /** BitmapPool 占屏数（复用 Bitmap，降低 GC） */
    const val BITMAP_POOL_SCREENS = 3f

    // ============================ 默认占位资源 ============================
    /** 加载中占位图 */
    @DrawableRes
    val placeholderLoading: Int = R.drawable.ic_image_placeholder

    /** 加载失败占位图 */
    @DrawableRes
    val placeholderError: Int = R.drawable.ic_image_error

    /** 无图（url 为空）占位图 */
    @DrawableRes
    val placeholderEmpty: Int = R.drawable.ic_image_empty

    // ============================ 默认加载参数 ============================
    /** 默认缩放模式 */
    enum class ScaleType { CENTER_CROP, CENTER_INSIDE, FIT_CENTER, NONE }

    /** 全局默认缩放模式 */
    var defaultScaleType: ScaleType = ScaleType.CENTER_CROP

    /**
     * 是否开启图片预压缩（默认 RGB_565，单像素 2 字节，相比 ARGB_8888 省一半内存）。
     * 开启可显著降低 OOM 概率；对画质要求极高的页面可单独覆盖为 false。
     */
    var enablePreCompress: Boolean = true

    /**
     * 是否允许硬件位图解码（Hardware Bitmap）。
     * 开启可提升大图渲染速度、降低内存；注意：需要软件画布处理的场景（如 SVG）会自动禁用。
     */
    var enableHardwareDecode: Boolean = true

    /** 默认圆角半径（dp） */
    const val DEFAULT_CORNER_RADIUS_DP = 8

    /** 默认高斯模糊半径 */
    const val DEFAULT_BLUR_RADIUS = 25

    /** 默认模糊采样率（越大越省内存、越模糊） */
    const val DEFAULT_BLUR_SAMPLING = 4

    // ============================ 重试 / 防重复 ============================
    /** 加载失败默认重试次数 */
    const val DEFAULT_RETRY_COUNT = 2

    /** 默认重试间隔（毫秒） */
    const val DEFAULT_RETRY_INTERVAL_MS = 1000L

    /** 同一 ImageView 重复加载相同 url 的去重时间窗口（毫秒） */
    const val DUPLICATE_LOAD_INTERVAL_MS = 500L

    // ============================ 大图采样 ============================
    /** 超大图阈值（像素面积），超过则自动降采样 */
    const val HUGE_IMAGE_PIXELS = 4096 * 4096
}
