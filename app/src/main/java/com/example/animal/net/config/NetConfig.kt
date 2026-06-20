package com.example.animal.net.config

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.example.animal.BuildConfig

/**
 * 全局网络配置中心（单例）
 *
 * 职责：
 * 1. 持有 ApplicationContext（网络检测、缓存目录、Cookie 存储均依赖它）；
 * 2. 统一管理各类「开关」与「动态参数」：超时、加密、SSL 放行、缓存、token、渠道号等；
 * 3. 提供登录失效全局回调入口。
 *
 * 使用：在 Application.onCreate 中调用 [NetConfig.init]。
 */
@SuppressLint("StaticFieldLeak")
object NetConfig {

    /** ApplicationContext，init 后赋值 */
    lateinit var appContext: Context
        private set

    /** 是否为 Debug 环境（来自 BuildConfig.DEBUG） */
    val isDebug: Boolean = BuildConfig.DEBUG

    // ============================ 火山方舟（Ark）大模型常量 ============================
    /** 火山方舟 API Key（鉴权使用，请求头 Authorization: Bearer {ARK_API_KEY}） */
    const val ARK_API_KEY = "ark-43370095-7ded-45a5-9fb6-d7fd10ec9c03-acfea"

    /** 固定模型 ID */
    const val ARK_MODEL_ID = "deepseek-v4-flash-260425"

    /** 火山方舟 responses 接口 BaseUrl（结尾保留斜杠，供 Retrofit 拼接 responses） */
    const val ARK_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3/"

    // ============================ 动态超时（秒），支持运行时修改 ============================
    @Volatile
    var connectTimeout: Long = NetConstants.DEFAULT_CONNECT_TIMEOUT

    @Volatile
    var readTimeout: Long = NetConstants.DEFAULT_READ_TIMEOUT

    @Volatile
    var writeTimeout: Long = NetConstants.DEFAULT_WRITE_TIMEOUT

    // ============================ 全局开关 ============================
    /** 是否开启请求参数加密（AES/RSA） */
    @Volatile
    var enableEncrypt: Boolean = false

    /** 是否开启网络缓存拦截 */
    @Volatile
    var enableCache: Boolean = true

    /** 是否开启失败重试 */
    @Volatile
    var enableRetry: Boolean = true

    /**
     * 是否信任所有证书（忽略 SSL 异常）。
     * 仅测试环境可开启；正式环境默认强制关闭，走自定义证书校验。
     */
    @Volatile
    var trustAllCerts: Boolean = isDebug

    /** 加密方式 */
    enum class EncryptType { AES, RSA }

    /** 当前加密方式，默认 AES */
    @Volatile
    var encryptType: EncryptType = EncryptType.AES

    // ============================ 业务动态参数 ============================
    /** 渠道号（打包渠道，可在 init 时传入） */
    @Volatile
    var channel: String = "official"

    /** App 版本名（init 时自动读取） */
    @Volatile
    var appVersion: String = "1.0.0"

    /** 登录态 token：持久化到 SharedPreferences */
    var token: String
        get() = configSp.getString(KEY_TOKEN, "") ?: ""
        set(value) {
            configSp.edit().putString(KEY_TOKEN, value).apply()
        }

    /** 登录失效（401）全局回调，由业务层设置（如跳转登录页） */
    @Volatile
    var onLoginExpired: (() -> Unit)? = null

    private const val KEY_TOKEN = "key_token"

    /** 业务配置 SharedPreferences（token 等） */
    private val configSp: SharedPreferences by lazy {
        appContext.getSharedPreferences(NetConstants.SP_CONFIG, Context.MODE_PRIVATE)
    }

    /**
     * 初始化网络框架。建议在 Application.onCreate 调用。
     *
     * @param context  上下文（内部转 ApplicationContext，避免内存泄漏）
     * @param env      初始运行环境
     * @param channel  打包渠道号
     */
    fun init(
        context: Context,
        env: BaseUrlManager.Env = if (BuildConfig.DEBUG) BaseUrlManager.Env.TEST else BaseUrlManager.Env.PROD,
        channel: String = "official"
    ) {
        this.appContext = context.applicationContext
        this.channel = channel
        BaseUrlManager.switchEnv(env)
        // 自动读取 App 版本名
        runCatching {
            val pm = appContext.packageManager
            val info = pm.getPackageInfo(appContext.packageName, 0)
            appVersion = info.versionName ?: "1.0.0"
        }
    }

    /**
     * 动态修改超时配置（秒）。修改后对后续新建的请求生效。
     */
    fun updateTimeout(connect: Long = connectTimeout, read: Long = readTimeout, write: Long = writeTimeout) {
        connectTimeout = connect
        readTimeout = read
        writeTimeout = write
    }
}
