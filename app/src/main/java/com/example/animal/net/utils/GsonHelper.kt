package com.example.animal.net.utils

import com.example.animal.net.config.NetConstants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.Date

/**
 * 全局 Gson 配置。
 *
 * 自定义适配：
 * 1. 空字符串("")自动转为 null，避免业务侧拿到无意义空串；
 * 2. Date 类型按统一格式序列化/反序列化；
 * 3. 默认忽略未知字段（Gson 原生行为），同时对类型不匹配做容错。
 */
object GsonHelper {

    /** 空字符串转 null 的 String 适配器 */
    private val emptyStringToNullAdapter = object : TypeAdapter<String?>() {
        override fun write(out: JsonWriter, value: String?) {
            if (value == null) out.nullValue() else out.value(value)
        }

        override fun read(reader: JsonReader): String? {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            val value = reader.nextString()
            return value.ifEmpty { null }
        }
    }

    /** Date 适配器：按统一格式解析 */
    private val dateAdapter = object : TypeAdapter<Date?>() {
        override fun write(out: JsonWriter, value: Date?) {
            if (value == null) out.nullValue() else out.value(TimeUtils.format(value))
        }

        override fun read(reader: JsonReader): Date? {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            return when (reader.peek()) {
                // 兼容时间戳数字
                JsonToken.NUMBER -> Date(reader.nextLong())
                else -> {
                    val str = reader.nextString()
                    if (str.isEmpty()) null else Date(TimeUtils.parse(str))
                }
            }
        }
    }

    /** 全局唯一 Gson 实例 */
    val gson: Gson = GsonBuilder()
        .setDateFormat(NetConstants.DATE_FORMAT_DEFAULT)
        .registerTypeAdapter(String::class.java, emptyStringToNullAdapter)
        .registerTypeAdapter(Date::class.java, dateAdapter)
        // 序列化时不输出 null，按需可放开
        .disableHtmlEscaping()
        .create()
}
