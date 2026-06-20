package com.example.animal.net.utils

import android.util.Base64
import com.example.animal.net.config.NetConstants
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 参数加密工具：支持 AES（对称）与 RSA（非对称）。
 *
 * - AES：用于请求参数整体加密，性能高，密钥需安全保管；
 * - RSA：用于加密 AES 密钥或敏感小数据，使用服务端下发的公钥加密。
 */
object EncryptUtils {

    private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

    /**
     * AES 加密，返回 Base64 字符串。
     */
    fun aesEncrypt(
        content: String,
        key: String = NetConstants.AES_KEY,
        iv: String = NetConstants.AES_IV
    ): String {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val ivSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(content.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * AES 解密，入参为 Base64 字符串。
     */
    fun aesDecrypt(
        base64Content: String,
        key: String = NetConstants.AES_KEY,
        iv: String = NetConstants.AES_IV
    ): String {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val ivSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decoded = Base64.decode(base64Content, Base64.NO_WRAP)
        return String(cipher.doFinal(decoded), Charsets.UTF_8)
    }

    /**
     * RSA 公钥加密，返回 Base64 字符串。
     * @param publicKeyBase64 服务端下发的公钥（Base64 X509 格式）
     */
    fun rsaEncrypt(content: String, publicKeyBase64: String): String {
        val keyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encrypted = cipher.doFinal(content.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }
}
