package com.example.animal.image.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 相册 / 媒体图片读取权限适配工具。
 *
 * 适配差异：
 * - Android 13 (API 33)+：使用细分媒体权限 READ_MEDIA_IMAGES；
 * - Android 12 及以下：使用 READ_EXTERNAL_STORAGE；
 * - Android 10 (Q) 分区存储下，加载自身应用产生的 Uri 通常无需权限。
 */
object ImagePermissionHelper {

    /** 获取当前系统加载相册图片所需的权限名 */
    fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /** 是否已授予图片读取权限 */
    fun hasImagePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, requiredPermission()) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * 加载相册图片前的统一权限检查。
     * 有权限则执行 [onGranted]，无权限则回调 [onDenied] 提示业务去申请。
     */
    fun checkBeforeLoad(
        context: Context,
        onGranted: () -> Unit,
        onDenied: (permission: String) -> Unit
    ) {
        if (hasImagePermission(context)) {
            onGranted()
        } else {
            onDenied(requiredPermission())
        }
    }
}
