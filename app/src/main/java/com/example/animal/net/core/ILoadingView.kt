package com.example.animal.net.core

/**
 * 加载弹窗抽象接口。
 *
 * 框架不绑定具体 UI 实现，业务可提供自定义 Loading（Dialog/全局蒙层等）。
 * 配合 [launchHttp] 的 loading 参数实现请求自动展示/隐藏 Loading。
 */
interface ILoadingView {
    /** 显示 Loading，可自定义文案 */
    fun show(message: String?)

    /** 隐藏 Loading */
    fun dismiss()
}
