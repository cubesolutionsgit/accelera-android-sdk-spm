# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep DivKit classes
-keep class com.yandex.div.** { *; }
-dontwarn com.yandex.div.**

# Keep Accelera classes
-keep class ai.accelera.library.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ai.accelera.library.**$$serializer { *; }
-keepclassmembers class ai.accelera.library.** {
    *** Companion;
}
-keepclasseswithmembers class ai.accelera.library.** {
    kotlinx.serialization.KSerializer serializer(...);
}

