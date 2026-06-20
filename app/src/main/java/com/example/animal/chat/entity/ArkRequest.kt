package com.example.animal.chat.entity

import com.example.animal.net.config.NetConfig
import com.google.gson.annotations.SerializedName

/**
 * 火山方舟 responses 接口完整请求体实体。
 *
 * 结构完全匹配官方 curl 示例：
 * ```json
 * {
 *   "model": "deepseek-v4-flash-260425",
 *   "stream": true,
 *   "tools": [ { "type": "web_search", "web_search": { "max_keyword": 3 } } ],
 *   "input": [ { "role": "user", "content": [ { "type": "input_text", "text": "..." } ] } ]
 * }
 * ```
 *
 * @param model  固定模型 ID（来自全局常量 [NetConfig.ARK_MODEL_ID]）
 * @param stream 固定 true，开启 SSE 分段流式返回
 * @param tools  固定内置 web_search 联网搜索工具
 * @param input  对话数组（人设 + 历史 + 最新消息）
 */
data class ArkRequest(
    @SerializedName("model") val model: String = NetConfig.ARK_MODEL_ID,
    @SerializedName("stream") val stream: Boolean = true,
    @SerializedName("tools") val tools: List<ArkToolItem> = listOf(ArkToolItem.default()),
    @SerializedName("input") val input: List<ArkInputItem>
)
