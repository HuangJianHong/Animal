package com.example.animal.net.utils

import com.example.animal.net.config.NetConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时间转换工具。
 *
 * 用于网络层时间戳 <-> 字符串 互转，以及统一的时间格式化。
 * SimpleDateFormat 非线程安全，这里用 ThreadLocal 保证并发安全。
 */
object TimeUtils {

    private val formatterHolder = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat(NetConstants.DATE_FORMAT_DEFAULT, Locale.getDefault())
    }

    private fun formatter(pattern: String?): SimpleDateFormat {
        val sdf = formatterHolder.get()!!
        if (!pattern.isNullOrEmpty() && pattern != sdf.toPattern()) {
            sdf.applyPattern(pattern)
        }
        return sdf
    }

    /** 时间戳(ms) -> 字符串 */
    fun format(timeMillis: Long, pattern: String = NetConstants.DATE_FORMAT_DEFAULT): String =
        formatter(pattern).format(Date(timeMillis))

    /** Date -> 字符串 */
    fun format(date: Date, pattern: String = NetConstants.DATE_FORMAT_DEFAULT): String =
        formatter(pattern).format(date)

    /** 字符串 -> 时间戳(ms)，解析失败返回 0 */
    fun parse(timeStr: String, pattern: String = NetConstants.DATE_FORMAT_DEFAULT): Long =
        runCatching { formatter(pattern).parse(timeStr)?.time ?: 0L }.getOrDefault(0L)

    /** 当前时间戳（毫秒），用于请求签名 / 防重放 */
    fun nowMillis(): Long = System.currentTimeMillis()
}
