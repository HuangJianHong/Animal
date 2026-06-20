package com.example.animal.net.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer

/**
 * 带上传进度回调的 RequestBody，用于大文件上传时实时回调进度。
 *
 * 用法：把原始 RequestBody 包一层即可。
 */
class ProgressRequestBody(
    private val delegate: RequestBody,
    private val listener: ProgressListener
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    /** 统计写出字节数的 Sink */
    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten = 0L
        private val total = contentLength()

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            val percent = if (total > 0) (bytesWritten * 100 / total).toInt() else 0
            listener.onProgress(bytesWritten, total, percent, bytesWritten >= total)
        }
    }
}
