package com.example.animal.net.utils

/**
 * 上传 / 下载进度回调监听。
 *
 * @param bytesProcessed 已处理字节数
 * @param totalBytes     总字节数（未知时为 -1）
 * @param percent        进度百分比 0~100
 * @param done           是否完成
 */
fun interface ProgressListener {
    fun onProgress(bytesProcessed: Long, totalBytes: Long, percent: Int, done: Boolean)
}
