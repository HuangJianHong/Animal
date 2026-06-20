package com.example.animal.image.core

import android.widget.ImageView
import com.example.animal.R
import com.example.animal.image.config.ImageConfig

/**
 * 防重复加载：同一个 ImageView 在极短时间内重复加载相同 url 时拦截，减少无效网络请求。
 *
 * 通过给 ImageView 打 tag 记录「上一次 url + 时间」实现，不引入额外内存持有。
 */
object DuplicateLoadGuard {

    /**
     * 判断本次加载是否应执行。
     * @return true 允许加载；false 属于短时间重复加载，拦截。
     */
    fun shouldLoad(view: ImageView, key: String?): Boolean {
        if (key.isNullOrEmpty()) return true
        val lastUrl = view.getTag(R.id.image_tag_last_url) as? String
        val lastTime = view.getTag(R.id.image_tag_last_time) as? Long ?: 0L
        val now = System.currentTimeMillis()
        val isDuplicate = key == lastUrl && (now - lastTime) < ImageConfig.DUPLICATE_LOAD_INTERVAL_MS
        if (!isDuplicate) {
            view.setTag(R.id.image_tag_last_url, key)
            view.setTag(R.id.image_tag_last_time, now)
        }
        return !isDuplicate
    }
}
