# CleanYourPhone R8 / ProGuard rules for Release APK

# Keep Kotlinx Serialization generated serializers (UadRawEntry, ActionLogEntry)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.drliki.cleanyourphone.model.**$$serializer { *; }
-keepclassmembers class com.drliki.cleanyourphone.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.drliki.cleanyourphone.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Shizuku Binder & Provider classes
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Keep DADb classes
-keep class dadb.** { *; }
-dontwarn dadb.**
