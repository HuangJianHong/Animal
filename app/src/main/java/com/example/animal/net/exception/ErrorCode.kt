package com.example.animal.net.exception

/**
 * 全局错误码统一管理
 *
 * 包含：HTTP 标准状态码 + 业务自定义本地错误码。
 * 业务侧应通过本类常量判断错误类型，禁止硬编码数字。
 */
object ErrorCode {

    /** 业务成功码（后端约定，按需修改） */
    const val SUCCESS = 200

    // ============================ HTTP 标准错误码 ============================
    /** 请求参数错误 */
    const val BAD_REQUEST = 400

    /** token 过期 / 未登录 / 鉴权失败 */
    const val UNAUTHORIZED = 401

    /** 无权限 */
    const val FORBIDDEN = 403

    /** 资源不存在 */
    const val NOT_FOUND = 404

    /** 接口限流（请求过于频繁） */
    const val TOO_MANY_REQUESTS = 429

    /** 服务器内部错误 */
    const val SERVER_ERROR = 500

    // ============================ 本地自定义错误码（负数区分） ============================
    /** 无网络 */
    const val NO_NETWORK = -1001

    /** 请求超时 */
    const val TIMEOUT = -1002

    /** 数据解析异常 */
    const val PARSE_ERROR = -1003

    /** 网络连接异常（host 不可达 / 连接失败） */
    const val NETWORK_ERROR = -1004

    /** SSL 证书异常 */
    const val SSL_ERROR = -1005

    /** 未知错误 */
    const val UNKNOWN = -1000

    /** 重复请求被拦截 */
    const val DUPLICATE_REQUEST = -1006

    /**
     * 根据错误码返回默认友好提示文案。
     */
    fun getMessage(code: Int): String = when (code) {
        BAD_REQUEST -> "请求参数有误，请稍后重试"
        UNAUTHORIZED -> "鉴权失败，请检查 API Key"
        FORBIDDEN -> "暂无访问权限"
        NOT_FOUND -> "请求的资源不存在"
        TOO_MANY_REQUESTS -> "请求过于频繁，请稍后再试"
        SERVER_ERROR -> "服务器开小差了，请稍后重试"
        NO_NETWORK -> "网络未连接，请检查网络设置"
        TIMEOUT -> "请求超时，请稍后重试"
        PARSE_ERROR -> "数据解析失败"
        NETWORK_ERROR -> "网络连接失败，请稍后重试"
        SSL_ERROR -> "证书校验失败"
        DUPLICATE_REQUEST -> "请勿重复操作"
        else -> "未知错误"
    }
}
