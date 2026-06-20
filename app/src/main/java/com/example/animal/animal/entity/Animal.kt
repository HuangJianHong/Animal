package com.example.animal.animal.entity

import androidx.annotation.DrawableRes
import java.io.Serializable

/**
 * 小动物数据实体。
 *
 * 实现 [Serializable] 以便通过 Intent 在首页与聊天页之间完整传递，
 * 携带该动物的头像资源与专属 system 人设 Prompt。
 *
 * @param id           动物唯一 id
 * @param avatarResId  头像资源 id（drawable/mipmap）
 * @param name         动物名称
 * @param systemPrompt 专属 system 人设 prompt（决定 AI 的性格与说话风格）
 */
data class Animal(
    val id: Int,
    @DrawableRes val avatarResId: Int,
    val name: String,
    val systemPrompt: String
) : Serializable {

    companion object {
        /** Intent 传递时使用的 key */
        const val EXTRA_KEY = "extra_animal"

        private const val serialVersionUID = 1L
    }
}
