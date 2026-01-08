plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "cn.pylin.xycjd"
    compileSdk = 36

    defaultConfig {
        applicationId = "cn.pylin.xycjd"
        minSdk = 30
        targetSdk = 36
        versionCode = 251229
        versionName = "1.5.5"
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
        buildConfig = true
        aidl = true
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
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.dynamicanimation)
    implementation(libs.viewpager2)
    implementation(libs.fragment)
    implementation(libs.okhttp)
}
