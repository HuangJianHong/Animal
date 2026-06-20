package com.example.animal.chat.entity

import com.google.gson.annotations.SerializedName

/**
 * tools 联网搜索工具实体（固定 web_search 配置，写死无需动态修改）。
 *
 * @param type      工具类型，固定 "web_search"
 * @param webSearch 联网搜索配置（max_keyword=3）
 */
data class ArkToolItem(
    @SerializedName("type") val type: String = "web_search",
    @SerializedName("web_search") val webSearch: WebSearchConfig = WebSearchConfig()
) {
    /**
     * web_search 工具的固定配置。
     * @param maxKeyword 最大联网搜索关键词数，固定 3
     */
    data class WebSearchConfig(
        @SerializedName("max_keyword") val maxKeyword: Int = 3
    )

    companion object {
        /** 默认联网搜索工具实例 */
        fun default(): ArkToolItem = ArkToolItem()
    }
}
