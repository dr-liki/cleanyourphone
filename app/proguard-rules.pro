# CleanYourPhone v2 — R8 / ProGuard rules

# Keep all models and serializers
-keep class com.drliki.cleanyourphone.model.** { *; }
-keepclassmembers class com.drliki.cleanyourphone.model.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Shizuku
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# DADb
-keep class dadb.** { *; }
-dontwarn dadb.**

# JUnit Platform (transitive via dadb) — not available on Android, safe to suppress
-dontwarn org.junit.**
-dontwarn java.util.spi.ToolProvider
-dontwarn org.junit.platform.console.ConsoleLauncherToolProvider
