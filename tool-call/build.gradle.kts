plugins {
    autowire(libs.plugins.android.library)
    autowire(libs.plugins.kotlin.android)
}

android {
    namespace = "com.niki914.tool_call"
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
    implementation(project(":chat"))

    implementation(org.jetbrains.kotlinx.kotlinx.serialization.json)
    implementation("org.apache.commons:commons-text:1.12.0") // 字符串算法

    implementation(com.github.niki914.zephyr.log)
    implementation(com.github.niki914.zephyr.provider)
    implementation(com.github.niki914.zephyr.tools)
    implementation("com.github.niki914:cmd-android:0.8.2")

    implementation(com.google.code.gson.gson)
    implementation(kotlin("reflect"))

    implementation(com.google.android.material.material)
}