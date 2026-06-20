package com.example.animal.net.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.animal.net.config.NetConfig

/**
 * 网络状态检测工具。
 *
 * 适配 Android 6.0(API 23) 以上的 NetworkCapabilities 新 API，
 * 同时向下兼容旧设备（minSdk 21）。
 */
object NetworkUtils {

    /**
     * 是否已连接网络（Wi-Fi / 蜂窝 / 以太网任一可用）。
     */
    @Suppress("DEPRECATION")
    fun isConnected(context: Context = NetConfig.appContext): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } else {
            // 旧版本兼容
            cm.activeNetworkInfo?.isConnected == true
        }
    }
}
