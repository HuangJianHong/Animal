plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Glide 注解处理器需要 kapt
    alias(libs.plugins.kotlin.kapt)
}

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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
