package com.example.animal.net.utils

import android.content.Context
import com.example.animal.net.config.NetConfig
import com.example.animal.net.config.NetConstants
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Cookie 持久化管理。
 *
 * - 自动存储服务端 Set-Cookie 返回的 Cookie；
 * - 下次请求同 host 时自动携带；
 * - 内存缓存 + SharedPreferences 双层持久化，App 重启后依然有效。
 */
class PersistentCookieJar(
    context: Context = NetConfig.appContext
) : CookieJar {

    private val sp = context.getSharedPreferences(NetConstants.SP_COOKIE, Context.MODE_PRIVATE)

    /** 内存缓存：host -> (cookieName -> Cookie) */
    private val cache = ConcurrentHashMap<String, MutableMap<String, Cookie>>()

    init {
        // 启动时从磁盘恢复（简化：仅恢复 host 列表对应的原始字符串）
        loadFromDisk()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val hostMap = cache.getOrPut(host) { ConcurrentHashMap() }
        cookies.forEach { cookie ->
            // 过期 Cookie 移除
            if (cookie.expiresAt < System.currentTimeMillis()) {
                hostMap.remove(cookie.name)
            } else {
                hostMap[cookie.name] = cookie
            }
        }
        persist(host, hostMap.values)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val hostMap = cache[url.host] ?: return emptyList()
        val now = System.currentTimeMillis()
        // 过滤已过期 Cookie
        return hostMap.values.filter { it.expiresAt > now }
    }

    /** 清空所有 Cookie（如退出登录时调用） */
    fun clear() {
        cache.clear()
        sp.edit().clear().apply()
    }

    /** 将某 host 的 Cookie 持久化到磁盘 */
    private fun persist(host: String, cookies: Collection<Cookie>) {
        // 以 setCookie 字符串形式存储，恢复时再 parse
        val raw = cookies.joinToString(SEPARATOR) { it.toString() }
        sp.edit().putString(host, raw).apply()
    }

    /** 从磁盘恢复全部 Cookie */
    private fun loadFromDisk() {
        sp.all.forEach { (host, value) ->
            val raw = value as? String ?: return@forEach
            val httpUrl = HttpUrl.Builder().scheme("https").host(host).build()
            val hostMap = cache.getOrPut(host) { ConcurrentHashMap() }
            raw.split(SEPARATOR).forEach { cookieStr ->
                if (cookieStr.isNotEmpty()) {
                    Cookie.parse(httpUrl, cookieStr)?.let { hostMap[it.name] = it }
                }
            }
        }
    }

    companion object {
        private const val SEPARATOR = "\u00A6" // 不常见分隔符，避免与 cookie 内容冲突
    }
}
