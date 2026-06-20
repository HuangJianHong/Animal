package com.example.animal.net.utils

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

/**
 * 带下载进度回调的 ResponseBody，用于文件下载时实时回调进度。
 */
class ProgressResponseBody(
    private val delegate: ResponseBody,
    private val listener: ProgressListener
) : ResponseBody() {

    private val bufferedSource: BufferedSource by lazy { source(delegate.source()).buffer() }

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun source(): BufferedSource = bufferedSource

    private fun source(source: Source): Source = object : ForwardingSource(source) {
        private var totalBytesRead = 0L
        private val total = contentLength()

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            if (bytesRead != -1L) {
                totalBytesRead += bytesRead
            }
            val done = bytesRead == -1L
            val percent = if (total > 0) (totalBytesRead * 100 / total).toInt() else 0
            listener.onProgress(totalBytesRead, total, percent, done)
            return bytesRead
        }
    }
}
