package com.example.animal.image.module

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.io.IOException
import java.io.InputStream

/**
 * SVG 解码器：把 InputStream 解析为 androidsvg 的 [SVG] 对象。
 * 配合 [SvgDrawableTranscoder] 把 SVG 转为可显示的 PictureDrawable。
 */
class SvgDecoder : ResourceDecoder<InputStream, SVG> {

    override fun handles(source: InputStream, options: Options): Boolean = true

    override fun decode(
        source: InputStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<SVG>? {
        return try {
            val svg = SVG.getFromInputStream(source)
            SimpleResource(svg)
        } catch (e: SVGParseException) {
            throw IOException("无法解析 SVG", e)
        }
    }
}
