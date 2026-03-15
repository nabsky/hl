plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.zorindisplays.hilo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zorindisplays.hilo"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "260318"
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
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.text)
    implementation(libs.androidx.foundation)
    implementation(libs.activity.compose)
    implementation(libs.emoji2)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.konfetti.compose)
    implementation(libs.androidx.compose.material3)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core-ktx:1.13.1")

    debugImplementation(libs.ui.tooling)
}

configurations.all {
    resolutionStrategy {
        force("androidx.emoji2:emoji2:1.3.0")
    }
}
