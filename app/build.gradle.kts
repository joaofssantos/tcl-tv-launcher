plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.joaofsantos.tcllauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.joaofsantos.tcllauncher"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }
