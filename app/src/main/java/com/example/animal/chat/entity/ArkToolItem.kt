package com.example.animal.chat.entity

import com.google.gson.annotations.SerializedName

/**
 * tools 联网搜索工具实体（固定 web_search 配置）。
 *
 * 联调结论：火山方舟 responses 接口的 web_search 工具只接受 `{"type":"web_search"}`，
 * 若额外携带 `web_search`/`max_keyword` 等嵌套字段，服务端会返回 400：
 * `InvalidParameter: json: unknown field "web_search"`。
 * 因此这里只保留 type 字段。
 *
 * @param type 工具类型，固定 "web_search"
 */
data class ArkToolItem(
    @SerializedName("type") val type: String = "web_search"
) {
    companion object {
        /** 默认联网搜索工具实例 */
        fun default(): ArkToolItem = ArkToolItem()
    }
}
