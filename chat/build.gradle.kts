plugins {
    autowire(libs.plugins.android.library)
    autowire(libs.plugins.kotlin.android)
}

android {
    namespace = "com.niki914.chat"
    compileSdk = property.project.android.compileSdk

    defaultConfig {
        minSdk = property.project.android.minSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(project(":core"))
    implementation(com.squareup.okhttp3.okhttp)
    implementation(com.google.code.gson.gson)

    implementation(androidx.annotation.annotation)
    implementation(org.jetbrains.kotlinx.kotlinx.coroutines.core)

    implementation(com.google.android.material.material)
    implementation(kotlin("reflect"))
}