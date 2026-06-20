package com.example.animal.net.core

import com.example.animal.net.config.NetConstants
import java.util.concurrent.ConcurrentHashMap

/**
 * 请求防重复管理。
 *
 * 同一个 key（通常为接口标识 + 参数）在 [NetConstants.DUPLICATE_REQUEST_INTERVAL_MS]
 * 时间窗口内重复发起时会被拦截，避免重复提交（如连点提交按钮）。
 */
object DuplicateGuard {

    /** key -> 上次请求时间戳 */
    private val lastRequestTime = ConcurrentHashMap<String, Long>()

    /**
     * 尝试占用一次请求资格。
     * @return true 表示允许发起；false 表示属于重复请求，应被拦截。
     */
    fun tryAcquire(key: String): Boolean {
        val now = System.currentTimeMillis()
        val last = lastRequestTime[key] ?: 0L
        if (now - last < NetConstants.DUPLICATE_REQUEST_INTERVAL_MS) {
            return false
        }
        lastRequestTime[key] = now
        return true
    }

    /** 请求结束后释放（允许立即再次发起，按需调用） */
    fun release(key: String) {
        lastRequestTime.remove(key)
    }
}
