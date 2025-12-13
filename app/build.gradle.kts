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
        versionCode = 251213
        versionName = "1.0.1"
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
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.fragment:fragment:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}