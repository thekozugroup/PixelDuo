plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "org.pixelduo"
    compileSdk = 36
    defaultConfig {
        applicationId = "org.pixelduo"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    val key = rootProject.file("signing/pixelduo.jks")
    signingConfigs {
        if (key.exists()) create("localRelease") {
            storeFile = key
            storePassword = System.getenv("PIXELDUO_STORE_PASSWORD") ?: rootProject.file("signing/password.txt").readText().trim()
            keyAlias = "pixelduo"
            keyPassword = storePassword
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (key.exists()) signingConfig = signingConfigs.getByName("localRelease")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.5.0-alpha10")
    testImplementation("junit:junit:4.13.2")
}
