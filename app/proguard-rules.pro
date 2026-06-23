# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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


# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# AppLovin SDK
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**

# Google Mobile Ads SDK (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.video.avd.utils.CookieUtils { *; }
# Keep your native libraries intact
-keep class com.video.avd.* { *; }
-keep class com.avd.* { *; }
# Ignore annotation used for build tooling.
#-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation


-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int w(...);
    public static int v(...);
    public static int i(...);
}

-keep class wseemann.media.* { *; }


-keep class cn.pedant.SweetAlert.** { *; }

# Add this global rule
    -keepattributes Signature

    # This rule will properly ProGuard all the model classes in
    # the package com.yourcompany.models.
    # Modify this rule to fit the structure of your app.
    -keepclassmembers class com.yourcompany.models.** {
      *;
    }
    -keepattributes Annotation
    # in order to provide the most meaningful crash reports, add the following line:
    -keepattributes SourceFile,LineNumberTable
    # If you are using custom exceptions, add this line so that custom exception types are skipped during obfuscation:
    -keep public class * extends java.lang.Exception

#    -printmapping mapping.txt
#
#
#    -keep class com.crashlytics.* { ; }
#    -dontwarn com.crashlytics.**

-keep class com.shockwave.**

-keep class androidx.room.** { *; }
-keep @androidx.room.Dao public interface *
-keepclassmembers class * {
    @androidx.room.* <fields>;
}

-keep class com.video.avd.data.local.** { *; }

-keepattributes *Annotation*

-keepclassmembers class * {
    public void set*(***);
    public *** get*();
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class com.video.avd.ui.videos.model.Video { *; }
-keepnames class com.video.avd.ui.videos.model.Video
-keepclassmembers class com.video.avd.ui.videos.model.Video {
    *;
}
-keep class com.video.avd.ui.songs.model.Audio { *; }
-keepnames class com.video.avd.ui.songs.model.Audio
-keepclassmembers class com.video.avd.ui.songs.model.Audio {
    *;
}
-keep class com.video.avd.ui.albumb.model.Albums { *; }
-keepnames class com.video.avd.ui.albumb.model.Albums
-keepclassmembers class com.video.avd.ui.albumb.model.Albums {
    *;
}
-keep class com.video.avd.ui.artist.model.Artist { *; }
-keepnames class com.video.avd.ui.artist.model.Artist
-keepclassmembers class com.video.avd.ui.artist.model.Artist {
    *;
}
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.fragment.app.Fragment{}

-keepclassmembers class * implements android.os.Parcelable {
   public static ** CREATOR;
     public void writeToParcel(android.os.Parcel, int);
        public int describeContents();
}

-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type


-keep class com.video.avd.ui.video_downloader.download_feature.** {*;}
-keep class com.video.avd.ui.app_vault.** {*;}



# Keep Google Play Services classes
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep classes that are accessed via reflection
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

# Keep the names of classes needed by the Google Cast SDK
-keepclassmembers class * extends com.google.android.gms.cast.framework.CastOptions {
    <methods>;
}

-keep class com.google.android.gms.cast.framework.** { *; }
-keep class com.google.android.gms.cast.** { *; }
-keep class androidx.work.** { *; }

# Keep Parcelable Creator fields
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# Open Subtitles Android
-keep class com.masterwok.opensubtitlesandroid.** {*;}


-keep class android.adservices.* { *; }
-keep class com.unity3d.services.* { *; }

-dontwarn android.adservices.AdServicesState
-dontwarn android.adservices.topics.GetTopicsRequest$Builder
-dontwarn android.adservices.topics.GetTopicsRequest
-dontwarn android.adservices.topics.GetTopicsResponse
-dontwarn android.adservices.topics.Topic
-dontwarn android.adservices.topics.TopicsManager


# Facebook Annotations
-dontwarn com.facebook.infer.annotation.Nullsafe$Mode
-dontwarn com.facebook.infer.annotation.Nullsafe

# Firebase Messaging
-dontwarn com.google.firebase.messaging.TopicOperation$TopicOperations

# Google Protocol Buffers
-dontwarn com.google.protobuf.java_com_google_android_gmscore_sdk_target_granule__proguard_group_gtm_N1281923064GeneratedExtensionRegistryLite$Loader

# BouncyCastle
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider

# Conscrypt
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier

# OpenJSSE
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

-dontwarn org.immutables.value.Value$Default
-dontwarn org.immutables.value.Value$Immutable
-dontwarn org.immutables.value.Value$Style$BuilderVisibility
-dontwarn org.immutables.value.Value$Style$ImplementationVisibility
-dontwarn org.immutables.value.Value$Style

-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.example.ammar.youtubedldynamic.** { *; }

-keep class com.liulishuo.okdownload.** { *; }
-keep class io.lindstrom.** { *; }
-keep class org.immutables.** { *; }

-keep public class com.google.android.gms.** { public protected *; }

-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.firebase.analytics.** { *; }
-keepclassmembers class kotlin.Metadata {
   public <methods>;
}
-keep class kotlin.reflect.jvm.internal.** { *; }


# Keep classes in the dynamic feature module
-keep class com.example.ammar.youtubedldynamic.** { *; }

# Preserve YouTubeDL library classes if required
-keep class com.yausername.youtubedl_android.** { *; }
-keepclassmembers class com.yausername.youtubedl_android.** { *; }



# Preserve all classes in the YouTubeDL library
-keep class com.yausername.youtubedl_android.** { *; }
-keepclassmembers class com.yausername.youtubedl_android.** {
    *;
}

-keepclassmembers class kotlin.Metadata {
   public <methods>;
}
-keep class kotlin.reflect.jvm.internal.** { *; }


# Keep all class members that are accessed via reflection
-keepclassmembers class * {
    public *;
}

-keep class * { *; }


