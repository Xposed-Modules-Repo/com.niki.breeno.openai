plugins {
    autowire(libs.plugins.android.library)
    autowire(libs.plugins.kotlin.android)
    autowire(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.niki914.hooker"
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
    compileOnly(de.robv.android.xposed.api)
    ksp(com.highcapable.yukihookapi.ksp.xposed)
    implementation(com.highcapable.yukihookapi.api)
    implementation(com.highcapable.kavaref.kavaref.core)
    implementation(com.highcapable.kavaref.kavaref.extension)
    implementation(project(":core"))

    implementation(org.jetbrains.kotlinx.kotlinx.coroutines.core)
}