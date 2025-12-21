import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 定义读取 local.properties 的函数
fun getLocalProperty(key: String): String {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { properties.load(it) }
    }
    return properties.getProperty(key, "")
}

android {
    namespace = "cn.pylin.xycjd"
    compileSdk = 36

    defaultConfig {
        applicationId = "cn.pylin.xycjd"
        minSdk = 30
        targetSdk = 36
        versionCode = 251221
        versionName = "1.4.1"
        // 使用函数读取 API Key
        buildConfigField("String", "Hunyuan_KEY", "\"${getLocalProperty("Hunyuan_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = false
        buildConfig = true // 必须启用 buildConfig 功能
    }
    dependenciesInfo {
        includeInBundle = false
        includeInApk = false
    }
    buildToolsVersion = "36.1.0"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation("androidx.dynamicanimation:dynamicanimation:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.fragment:fragment:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
