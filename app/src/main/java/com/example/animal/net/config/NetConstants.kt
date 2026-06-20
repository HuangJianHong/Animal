package com.example.animal.net.config

/**
 * 网络全局常量统一管理类
 *
 * 规范：项目中所有网络相关的"魔法值"（超时时间、请求头 Key、缓存参数、加密 Key 等）
 * 一律抽离到本类，严禁在业务代码中硬编码。
 */
object NetConstants {

    // ============================ 超时配置（单位：秒） ============================
    /** 默认连接超时：10s */
    const val DEFAULT_CONNECT_TIMEOUT = 10L

    /** 默认读取超时：15s */
    const val DEFAULT_READ_TIMEOUT = 15L

    /** 默认写入超时：15s */
    const val DEFAULT_WRITE_TIMEOUT = 15L

    // ============================ 公共请求头 Key ============================
    /** 设备型号 */
    const val HEADER_DEVICE_MODEL = "Device-Model"

    /** 系统版本（Android 版本号） */
    const val HEADER_OS_VERSION = "OS-Version"

    /** App 版本名 */
    const val HEADER_APP_VERSION = "App-Version"

    /** 鉴权 token */
    const val HEADER_TOKEN = "Authorization"

    /** 渠道号 */
    const val HEADER_CHANNEL = "Channel"

    /** 平台标识 */
    const val HEADER_PLATFORM = "Platform"

    /** 平台固定值：android */
    const val PLATFORM_ANDROID = "android"

    /** 多 BaseUrl 动态切换使用的特殊请求头（不会真正发给服务端，仅用于本地解析） */
    const val HEADER_BASE_URL = "Base-Url-Name"

    // ============================ 缓存配置 ============================
    /** 缓存目录名 */
    const val CACHE_DIR_NAME = "net_http_cache"

    /** 缓存磁盘大小：50MB */
    const val CACHE_MAX_SIZE = 50L * 1024 * 1024

    /** 有网络时缓存有效期：60s（秒） */
    const val CACHE_AGE_ONLINE = 60

    /** 无网络时允许使用的过期缓存时长：7 天（秒） */
    const val CACHE_AGE_OFFLINE = 7 * 24 * 60 * 60

    // ============================ 重试配置 ============================
    /** 最大重试次数：2 次 */
    const val MAX_RETRY_COUNT = 2

    /** 重试间隔：1 秒（毫秒） */
    const val RETRY_INTERVAL_MS = 1000L

    // ============================ 防重复请求配置 ============================
    /** 相同请求的去重时间窗口：1000ms */
    const val DUPLICATE_REQUEST_INTERVAL_MS = 1000L

    // ============================ Cookie / 加密存储 ============================
    /** Cookie 持久化使用的 SharedPreferences 文件名 */
    const val SP_COOKIE = "net_cookie_store"

    /** 业务配置（token、渠道号等）使用的 SharedPreferences 文件名 */
    const val SP_CONFIG = "net_config_store"

    /** AES 加密密钥（生产环境请放到更安全的位置，如 native 层 / 服务端下发） */
    const val AES_KEY = "0123456789abcdef"

    /** AES 加密向量 IV */
    const val AES_IV = "abcdef0123456789"

    // ============================ 时间格式 ============================
    /** 默认时间格式 */
    const val DATE_FORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss"
}
