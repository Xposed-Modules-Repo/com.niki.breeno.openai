pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        maven { url = uri("https://jitpack.io") }

        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("com.highcapable.sweetdependency") version "1.0.4"
    id("com.highcapable.sweetproperty") version "1.0.5"
}
sweetProperty {
    rootProject { all { isEnable = false } }
}
rootProject.name = "breeno-openai-pri"
include(":app")
include(":core")
include(":chat")
include(":hooker")
include(":tool-call")
