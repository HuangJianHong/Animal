package com.example.animal.net.utils

import com.example.animal.net.config.NetConfig
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * HTTPS / SSL 适配工具。
 *
 * 双适配：
 * - 测试环境（[NetConfig.trustAllCerts] = true）：信任全部证书，忽略 SSL 异常，方便联调；
 * - 生产环境：使用自定义证书（Cer 文件）做校验，杜绝中间人攻击。
 */
object SslHelper {

    /** SSLSocketFactory + TrustManager 配对结果 */
    data class SSLParams(
        val sslSocketFactory: SSLSocketFactory,
        val trustManager: X509TrustManager
    )

    /** 信任所有证书的 TrustManager（仅测试环境用） */
    private val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    /**
     * 获取 SSL 配置。
     * @param certificates 生产环境自定义证书输入流（可传多个），为空则使用系统默认证书校验。
     */
    fun getSSLParams(vararg certificates: InputStream): SSLParams {
        val trustManager: X509TrustManager = if (NetConfig.trustAllCerts) {
            // 测试环境：信任全部
            trustAllManager
        } else if (certificates.isNotEmpty()) {
            // 生产环境：使用自定义证书
            buildTrustManager(certificates)
        } else {
            // 生产环境：系统默认证书校验
            systemDefaultTrustManager()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), java.security.SecureRandom())
        return SSLParams(sslContext.socketFactory, trustManager)
    }

    /** 主机名校验器：测试环境放行全部，生产环境走默认严格校验 */
    fun hostnameVerifier(): HostnameVerifier =
        if (NetConfig.trustAllCerts) HostnameVerifier { _, _ -> true }
        else javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()

    /** 用自定义证书构建 TrustManager */
    private fun buildTrustManager(certificates: Array<out InputStream>): X509TrustManager {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null) }
        certificates.forEachIndexed { index, inputStream ->
            inputStream.use {
                val certificate = certificateFactory.generateCertificate(it)
                keyStore.setCertificateEntry(index.toString(), certificate)
            }
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)
        return tmf.trustManagers.first { it is X509TrustManager } as X509TrustManager
    }

    /** 系统默认 TrustManager */
    private fun systemDefaultTrustManager(): X509TrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        return tmf.trustManagers.first { it is X509TrustManager } as X509TrustManager
    }
}
