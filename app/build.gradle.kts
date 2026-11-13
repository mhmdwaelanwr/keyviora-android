plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mhmdwaelanwr.keyviora"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mhmdwaelanwr.keyviora"
        minSdk = 28
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0-beta"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
