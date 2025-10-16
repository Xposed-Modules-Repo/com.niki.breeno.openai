# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#-keep class com.niki914.net.** { *; }
#-keep class com.niki914.chat.** { *; }

# FreeReflection
-keep class me.weishu.reflection.** {*;}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static *** throwUninitializedProperty(...);
    public static *** throwUninitializedPropertyAccessException(...);
}

-dontwarn java.lang.reflect.AnnotatedType

# =================== 你的 App 入口 (例如 Xposed Hook) ===================
# 保持你的 Hook 入口类和其所有成员不被混淆
#-keep class com.niki.breeno.MainHook { *; }

# =================== 必要的框架和库规则 ===================

# Xposed Framework (保留核心接口即可)
#-keep interface de.robv.android.xposed.IXposedHookLoadPackage { *; }
#-keep interface de.robv.android.xposed.IXposedHookZygoteInit { *; }
#-keep interface de.robv.android.xposed.IXposedHookInitPackageResources { *; }

# Kotlin Coroutines
# 推荐由 AGP 和 R8 的默认规则处理，但如果遇到问题，以下是安全的
#-dontwarn kotlinx.coroutines.flow.**
#-dontwarn kotlinx.coroutines.internal.**
#-dontwarn kotlinx.coroutines.debug.**

# Kotlin 反射和元数据 (必要)
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation
-keep class kotlin.jvm.internal.DefaultConstructorMarker { *; }
# 如果协程出问题，可以加上这条作为保险
# -keep class * extends kotlin.coroutines.Continuation { *; }


# =================== 标准 Android 规则 (必要) ===================

# 保留 JNI 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留自定义 View 的构造函数
#-keep public class * extends android.view.View {
#    public <init>(android.content.Context);
#    public <init>(android.content.Context, android.util.AttributeSet);
#    public <init>(android.content.Context, android.util.AttributeSet, int);
#}

# 保留 Parcelable 的 CREATOR 字段
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# 保留 Serializable 的特殊方法和字段
-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
