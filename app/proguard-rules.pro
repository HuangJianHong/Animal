# ============================================================
# Animal 项目 Release 混淆规则（R8 / ProGuard）
# 目标：开启代码压缩混淆的同时，保证网络框架（Retrofit + OkHttp + Gson）、
#       图片框架（Glide）、协程、数据实体在 Release 下正常运行。
# ============================================================

# ---------- 通用：保留必要属性 ----------
# Signature/泛型（Gson 解析泛型需要）、注解、内部类、异常表
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, Exceptions
# 保留源码行号，便于线上崩溃定位（同时隐藏真实源码文件名）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- 本项目：BuildConfig（NetConfig 读取 BuildConfig.DEBUG） ----------
-keep class com.example.animal.BuildConfig { *; }

# ============================================================
# 数据实体（Gson 反射 + Intent 序列化），必须完整保留字段与结构
# 覆盖：animal/entity、chat/entity、net/entity 等所有 entity 包
# ============================================================
-keep class com.example.animal.**.entity.** { *; }

# 所有带 @SerializedName 的字段（即便不在 entity 包），避免被优化移除/改名
-keepclassmembers,allowobfuscation class com.example.animal.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 枚举（Gson 序列化 + 状态机，如 ChatMessage.Status、NetConfig.EncryptType）
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Serializable（Animal 通过 Intent 在页面间传递）
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================
# Gson
# ============================================================
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-dontwarn sun.misc.**
# 自定义 TypeAdapter（GsonHelper 中的匿名适配器）
-keep class * extends com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# ============================================================
# Retrofit
# ============================================================
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
# 保留接口方法上的 Retrofit 注解（@GET/@POST/@Streaming 等）
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# 本项目所有 Retrofit Api 接口（含 ArkApiService、DemoApiService、BaseApi）
-keep interface com.example.animal.**.api.** { *; }
-keep interface com.example.animal.chat.model.ArkApiService { *; }
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn javax.annotation.**

# ============================================================
# OkHttp / Okio
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================================
# Glide（含 @GlideModule 注解处理器生成的实现 + 本项目自定义图片框架）
# ============================================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.integration.okhttp3.OkHttpGlideModule { *; }
# 注解处理器生成的 GlideModule 实现类（通配包名，兼容生成位置）
-keep class **.GeneratedAppGlideModuleImpl { *; }
-dontwarn com.bumptech.glide.**
# 本项目图片框架（自定义 GlideModule / 组件 / 转换 / 监听器）整体保留
-keep class com.example.animal.image.** { *; }

# AndroidSVG（SVG 解析）
-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**

# ============================================================
# Kotlin 协程
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ============================================================
# AndroidX / ViewBinding（一般无需额外规则，保留 binding 入口以防万一）
# ============================================================
-keep class com.example.animal.databinding.** { *; }
