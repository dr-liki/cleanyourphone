import java.util.Properties
import java.io.FileInputStream
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "com.drliki.cleanyourphone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.drliki.cleanyourphone"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "2.6.0"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val envStoreFile = System.getenv("SIGNING_STORE_FILE")
            val propStoreFile = keystoreProperties.getProperty("storeFile")
            val repoStoreFile = rootProject.file("keystore/release.jks")
            val resolvedFile = when {
                !envStoreFile.isNullOrBlank() && File(envStoreFile).exists() -> File(envStoreFile)
                !propStoreFile.isNullOrBlank() && rootProject.file(propStoreFile).exists() -> rootProject.file(propStoreFile)
                repoStoreFile.exists() -> repoStoreFile
                else -> null
            }
            if (resolvedFile != null && resolvedFile.exists()) {
                storeFile = resolvedFile
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                    ?: keystoreProperties.getProperty("storePassword")
                    ?: "cleanyourphone2026"
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                    ?: keystoreProperties.getProperty("keyAlias")
                    ?: "cleanyourphone"
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                    ?: keystoreProperties.getProperty("keyPassword")
                    ?: "cleanyourphone2026"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE.md",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/*.DSA",
                "META-INF/*.SF",
                "META-INF/*.RSA"
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    implementation("dev.mobile:dadb:1.2.8")

    // Pure Kotlin ADB engine with TLS 1.3 & SPAKE2 wireless pairing
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.bouncycastle:bctls-jdk18on:1.78.1")
    implementation("com.squareup.okio:okio:3.9.0")

    // QR code generation for Android 11+ Wireless Debugging pairing
    implementation("com.google.zxing:core:3.5.3")
}
