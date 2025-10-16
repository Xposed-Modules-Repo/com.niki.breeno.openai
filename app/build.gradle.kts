plugins {
    autowire(libs.plugins.android.application)
    autowire(libs.plugins.kotlin.android)
    autowire(libs.plugins.kotlin.ksp)
    autowire(libs.plugins.android.compose)
    autowire(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.niki914.breeno"
    compileSdk = property.project.android.compileSdk

    defaultConfig {
        applicationId = property.project.app.packageName
        minSdk = property.project.android.minSdk
        targetSdk = property.project.android.targetSdk
        versionName = property.project.app.versionName
        versionCode = property.project.app.versionCode
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        freeCompilerArgs = listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    lint { checkReleaseBuilds = false }

    // TODO Please visit https://highcapable.github.io/YukiHookAPI/en/api/special-features/host-inject
    // TODO 请参考 https://highcapable.github.io/YukiHookAPI/zh-cn/api/special-features/host-inject
    // androidResources.additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x64")
}

dependencies {
    implementation(project(":hooker"))
    implementation(project(":chat"))
    implementation(project(":tool-call"))
    implementation(project(":core"))

    implementation(com.google.dagger.hilt.android)
    implementation("androidx.navigation:navigation-compose:2.7.7") // 请使用最新版本
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // 请使用最新版本
    ksp(com.google.dagger.hilt.android.compiler)

    compileOnly(de.robv.android.xposed.api)
    ksp(com.highcapable.yukihookapi.ksp.xposed)
    implementation(com.highcapable.yukihookapi.api)
    implementation(com.highcapable.kavaref.kavaref.core)
    implementation(com.highcapable.kavaref.kavaref.extension)

    implementation(com.google.android.material.material)

    implementation(androidx.annotation.annotation)

    implementation("androidx.activity:activity-compose:1.4.0")

    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-alpha16")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    debugImplementation(androidx.compose.ui.ui.tooling)
    implementation(androidx.compose.ui.ui.tooling.preview.android)

    implementation(org.jetbrains.kotlinx.kotlinx.serialization.json)

    implementation(com.github.niki914.zephyr.tools)
    implementation(com.github.niki914.zephyr.provider)
    implementation(com.github.niki914.zephyr.log)

//    implementation("com.squareup.leakcanary:leakcanary-android:2.7")
    implementation(kotlin("reflect"))

    testImplementation(junit.junit)
    androidTestImplementation(androidx.test.ext.junit)
    androidTestImplementation(androidx.test.espresso.espresso.core)
}