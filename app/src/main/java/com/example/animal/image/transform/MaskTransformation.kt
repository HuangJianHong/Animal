package com.example.animal.image.transform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.example.animal.image.config.ImageConfig
import java.security.MessageDigest

/**
 * 蒙版遮罩变换：用指定的形状 Drawable 作为蒙版裁切图片，
 * 可实现异形头像、心形/星形图片等效果。
 *
 * @param context 上下文
 * @param maskId  蒙版资源（形状由该 Drawable 的不透明区域决定）
 */
class MaskTransformation(
    private val context: Context,
    @DrawableRes private val maskId: Int
) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height
        val result = pool.get(width, height, Bitmap.Config.ARGB_8888)

        val mask = ContextCompat.getDrawable(context, maskId)
            ?: return toTransform

        val canvas = Canvas(result)
        // 先画蒙版形状
        mask.setBounds(0, 0, width, height)
        mask.draw(canvas)
        // 再以 SRC_IN 模式画原图，仅保留蒙版区域
        canvas.drawBitmap(toTransform, Rect(0, 0, width, height), Rect(0, 0, width, height), MASK_PAINT)
        return result
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("$ID-$maskId".toByteArray(Charsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean = other is MaskTransformation && other.maskId == maskId

    override fun hashCode(): Int = ID.hashCode() + maskId

    companion object {
        private const val ID = "com.example.animal.image.transform.MaskTransformation"

        /** SRC_IN：只保留下层（蒙版）与上层（图片）重叠且蒙版不透明的部分 */
        private val MASK_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }

        /** 默认圆角半径（dp 转 px 由调用方处理），此处保留与 Config 关联 */
        val DEFAULT_RADIUS = ImageConfig.DEFAULT_CORNER_RADIUS_DP
    }
}
