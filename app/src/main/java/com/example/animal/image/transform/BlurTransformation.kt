package com.example.animal.image.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.example.animal.image.config.ImageConfig
import java.security.MessageDigest

/**
 * 高斯模糊变换（纯 Kotlin Stack Blur 实现，不依赖已废弃的 RenderScript）。
 *
 * @param radius   模糊半径，越大越模糊
 * @param sampling 采样率，先缩小图片再模糊，越大越省内存、性能越好
 */
class BlurTransformation(
    private val radius: Int = ImageConfig.DEFAULT_BLUR_RADIUS,
    private val sampling: Int = ImageConfig.DEFAULT_BLUR_SAMPLING
) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height
        val scaledWidth = (width / sampling).coerceAtLeast(1)
        val scaledHeight = (height / sampling).coerceAtLeast(1)

        // 先按采样率缩小，降低模糊计算量
        val bitmap = pool.get(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(1f / sampling, 1f / sampling)
        canvas.drawBitmap(toTransform, 0f, 0f, null)

        return stackBlur(bitmap, radius)
    }

    /** Stack Blur 算法（经典快速模糊实现） */
    private fun stackBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return sentBitmap
        val bitmap = sentBitmap
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int; var gsum: Int; var bsum: Int; var x: Int; var y: Int; var i: Int
        var p: Int; var yp: Int; var yi: Int; var yw: Int
        val vmin = IntArray(maxOf(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yw = 0; yi = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int; var goutsum: Int; var boutsum: Int
        var rinsum: Int; var ginsum: Int; var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0; gsum = 0; rsum = 0
            boutsum = 0; goutsum = 0; routsum = 0
            binsum = 0; ginsum = 0; rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + minOf(wm, maxOf(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (y == 0) vmin[x] = minOf(x + radius + 1, wm)
                p = pix[yw + vmin[x]]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0; gsum = 0; rsum = 0
            boutsum = 0; goutsum = 0; routsum = 0
            binsum = 0; ginsum = 0; rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = maxOf(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }
                if (i < hm) yp += w
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (0xff000000.toInt()) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (x == 0) vmin[y] = minOf(y + r1, hm) * w
                p = x + vmin[y]
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("$ID-$radius-$sampling".toByteArray(Charsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean =
        other is BlurTransformation && other.radius == radius && other.sampling == sampling

    override fun hashCode(): Int = ID.hashCode() + radius * 1000 + sampling * 10

    companion object {
        private const val ID = "com.example.animal.image.transform.BlurTransformation"
    }
}
