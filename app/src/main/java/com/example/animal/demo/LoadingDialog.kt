package com.example.animal.demo

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.animal.net.core.ILoadingView

/**
 * 通用加载弹窗默认实现（纯代码构建，无需 XML）。
 *
 * 实现 [ILoadingView]，可直接传给 launchHttp 实现请求自动 show/dismiss，支持自定义文案。
 */
class LoadingDialog(context: Context) : ILoadingView {

    private val textView: TextView
    private val dialog: Dialog

    init {
        val dp = context.resources.displayMetrics.density
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((24 * dp).toInt(), (24 * dp).toInt(), (24 * dp).toInt(), (24 * dp).toInt())
            background = ColorDrawable(Color.parseColor("#CC000000")).apply {
                // 圆角通过简单背景模拟，业务可替换为自定义 drawable
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(ProgressBar(context))
            textView = TextView(context).apply {
                setTextColor(Color.WHITE)
                setPadding(0, (12 * dp).toInt(), 0, 0)
                text = "加载中..."
            }
            addView(textView)
        }

        dialog = Dialog(context).apply {
            setContentView(container)
            setCancelable(true)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun show(message: String?) {
        textView.text = message ?: "加载中..."
        if (!dialog.isShowing) dialog.show()
    }

    override fun dismiss() {
        if (dialog.isShowing) dialog.dismiss()
    }
}
