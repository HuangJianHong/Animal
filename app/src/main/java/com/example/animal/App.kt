package com.example.animal

import android.app.Application
import android.content.ComponentCallbacks2
import com.example.animal.image.cache.ImageCacheManager
import com.example.animal.net.config.BaseUrlManager
import com.example.animal.net.config.NetConfig

/**
 * 全局 Application，负责在启动时初始化网络框架。
 *
 * 注意：Glide 图片框架通过 @GlideModule(AppGlideModuleImpl) 自动初始化，无需手动调用；
 * 这里只需转发内存回调，实现「退后台/低内存自动释放缓存」。
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. 初始化网络框架（自动读取 App 版本、设置渠道与环境）
        NetConfig.init(
            context = this,
            env = if (BuildConfig.DEBUG) BaseUrlManager.Env.TEST else BaseUrlManager.Env.PROD,
            channel = "official"
        )

        // 2. 注册多域名（演示：文件服务独立域名）
        BaseUrlManager.registerUrl("file", "https://file.example.com/")

        // 3. 设置登录失效全局回调（401 时框架自动触发，这里可跳转登录页）
        NetConfig.onLoginExpired = {
            // 例如：startActivity(LoginActivity) 并清空任务栈
            // 此处仅占位，业务按需实现
        }

        // 4. 按需调整全局开关（示例：测试环境信任所有证书已默认开启）
        // NetConfig.enableEncrypt = true
        // NetConfig.updateTimeout(connect = 15, read = 20, write = 20)
    }

    /**
     * 系统内存紧张回调：转发给 Glide，按等级裁剪图片缓存。
     * TRIM_MEMORY_UI_HIDDEN 即应用退到后台，会释放部分内存缓存。
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        ImageCacheManager.trimMemory(this, level)
    }

    /** 低内存回调：主动清理内存缓存 */
    override fun onLowMemory() {
        super.onLowMemory()
        ImageCacheManager.trimMemory(this, ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}
