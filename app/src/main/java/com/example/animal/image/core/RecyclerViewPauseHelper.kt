package com.example.animal.image.core

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * RecyclerView 列表滑动优化工具。
 *
 * 滑动（拖拽 / 惯性滚动）时暂停图片加载，停止时恢复加载，
 * 减少滑动过程中的解码与内存抖动，提升滑动帧率。
 */
object RecyclerViewPauseHelper {

    /**
     * 给 RecyclerView 绑定「滑动暂停加载」逻辑。
     */
    fun attach(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                super.onScrollStateChanged(rv, newState)
                val glide = Glide.with(rv.context)
                when (newState) {
                    // 停止滚动：恢复加载
                    RecyclerView.SCROLL_STATE_IDLE -> glide.resumeRequests()
                    // 拖拽 / 惯性滚动：暂停加载
                    RecyclerView.SCROLL_STATE_DRAGGING,
                    RecyclerView.SCROLL_STATE_SETTLING -> glide.pauseRequests()
                }
            }
        })
    }
}
