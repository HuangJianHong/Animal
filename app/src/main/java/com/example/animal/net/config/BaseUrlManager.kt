package com.example.animal.net.config

/**
 * 多 BaseUrl / 多环境管理
 *
 * 功能：
 * 1. 维护「测试 / 预发 / 生产」三套环境域名，可一键动态切换；
 * 2. 支持通过名称注册多个业务域名（如主站、文件服务、支付服务），
 *    配合 [com.example.animal.net.config.NetConstants.HEADER_BASE_URL] 注解头实现单接口指定域名。
 */
object BaseUrlManager {

    /** 运行环境枚举 */
    enum class Env {
        /** 测试环境 */
        TEST,

        /** 预发环境 */
        PRE,

        /** 生产环境 */
        PROD
    }

    /** 各环境对应的主域名（默认域名） */
    private val mainUrlMap = mutableMapOf(
        Env.TEST to "https://test-api.example.com/",
        Env.PRE to "https://pre-api.example.com/",
        Env.PROD to "https://api.example.com/"
    )

    /** 当前环境，默认生产环境 */
    @Volatile
    var currentEnv: Env = Env.PROD
        private set

    /**
     * 命名多域名表：key 为域名别名，value 为完整 BaseUrl。
     * 用于一个 App 内访问多个后端服务的场景。
     */
    private val namedUrlMap = mutableMapOf<String, String>()

    /** 默认主域名别名 */
    const val NAME_DEFAULT = "default"

    /**
     * 动态切换运行环境。
     * @param env 目标环境
     */
    fun switchEnv(env: Env) {
        currentEnv = env
    }

    /**
     * 自定义/覆盖某个环境的主域名。
     */
    fun setMainUrl(env: Env, url: String) {
        mainUrlMap[env] = url
    }

    /**
     * 获取当前环境的默认主域名（即 Retrofit 的默认 baseUrl）。
     */
    fun getMainUrl(): String = mainUrlMap[currentEnv] ?: mainUrlMap.getValue(Env.PROD)

    /**
     * 注册一个命名域名。
     * @param name 域名别名（接口通过 Header 携带该别名即可切换到此域名）
     * @param url  完整域名
     */
    fun registerUrl(name: String, url: String) {
        namedUrlMap[name] = url
    }

    /**
     * 根据别名获取域名；找不到时回退到默认主域名。
     */
    fun getUrlByName(name: String?): String {
        if (name.isNullOrEmpty() || name == NAME_DEFAULT) return getMainUrl()
        return namedUrlMap[name] ?: getMainUrl()
    }
}
