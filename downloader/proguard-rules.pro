# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class com.avd.ui.main.home.browser.BrowserFragment$WebViewJavaScriptInterface {
   public *;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn org.immutables.value.Value$Default
-dontwarn org.immutables.value.Value$Immutable
-dontwarn org.immutables.value.Value$Style$BuilderVisibility
-dontwarn org.immutables.value.Value$Style$ImplementationVisibility
-dontwarn org.immutables.value.Value$Style

-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.liulishuo.okdownload.** { *; }
-keep class io.lindstrom.** { *; }
-keep class org.immutables.** { *; }
-keep class org.immutables.** { *; }
-keep class com.video.avd.* { *; }
-keep class com.avd.* { *; }
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.firebase.analytics.** { *; }
-keep class com.avd.** { *; }

-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }

-keep class ** extends java.lang.reflect.Field { *; }

-keep class com.video.avd.** { *; }
-keep class nh.** { *; }
-keep class a0.h.** { *; }

-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

-keepattributes Signature
-keepattributes *Annotation*