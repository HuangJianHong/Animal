import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Glide 注解处理器需要 kapt
    alias(libs.plugins.kotlin.kapt)
}

// 读取 Release 签名配置：敏感信息放在 keystore.properties（不纳入版本控制），仅存本地。
// 文件不存在或未填密码时，自动跳过正式签名（仍可编译出未签名 release 用于验证混淆）。
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists() &&
        !keystoreProperties.getProperty("storePassword").isNullOrEmpty()

android {
    namespace = "com.example.animal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.animal"
        // 业务要求：最低兼容 minSdk 21
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release 正式签名（animalks）：从 keystore.properties 读取，缺失时不设置
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(
                    keystoreProperties.getProperty("storeFile", "animalks")
                )
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 开启代码混淆 + 压缩（R8）。资源压缩默认关闭，避免误删资源；
            // 如需进一步减包可改 isShrinkResources = true 并自行验证。
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 仅当本地存在 keystore.properties 且填写了密码时，启用正式签名
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            // 调试包不混淆，沿用默认调试签名
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        // 网络框架需要读取 BuildConfig.DEBUG 做环境差异化逻辑
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // ---------------- 网络框架核心依赖 ----------------
    // Retrofit2 + Gson 转换器
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    // OkHttp4 + 日志拦截器
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    // Gson
    implementation(libs.gson)
    // Kotlin 协程（IO 线程请求 + 主线程回调）
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    // Lifecycle / ViewModel（viewModelScope 安全请求 + 生命周期绑定）
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    // -------------------------------------------------

    // ---------------- Glide 图片加载框架 ----------------
    implementation(libs.glide)
    // 注解处理器：生成 GlideApp，使 @GlideModule 生效
    kapt(libs.glide.compiler)
    // OkHttp 集成：用于图片加载进度回调
    implementation(libs.glide.okhttp.integration)
    // SVG 矢量图解析
    implementation(libs.androidsvg)
    // RecyclerView：列表滑动暂停加载
    implementation(libs.androidx.recyclerview)
    // ---------------------------------------------------

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
