package com.example.animal.image.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * 灰度滤镜变换：将彩色图片转换为黑白灰度图（饱和度置 0）。
 */
class GrayscaleTransformation : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height
        val config = toTransform.config ?: Bitmap.Config.ARGB_8888
        val bitmap = pool.get(width, height, config)

        val canvas = Canvas(bitmap)
        val saturation = ColorMatrix().apply { setSaturation(0f) }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(saturation) }
        canvas.drawBitmap(toTransform, 0f, 0f, paint)
        return bitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray(Charsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean = other is GrayscaleTransformation

    override fun hashCode(): Int = ID.hashCode()

    companion object {
        private const val ID = "com.example.animal.image.transform.GrayscaleTransformation"
    }
}
