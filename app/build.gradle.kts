plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.flux"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.flux"
        minSdk = 29
        targetSdk = 36
        versionCode = 4
        versionName = "2.2"
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    signingConfigs {
        create("release") {
            keyAlias = project.findProperty("signingKeyAlias") as String? ?: "release"
            keyPassword = project.findProperty("signingKeyPassword") as String? ?: ""
            storeFile =
                file(project.findProperty("signingStoreFile") as String? ?: "releaseKey.jks")
            storePassword = project.findProperty("signingStorePassword") as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isDebuggable = false
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "assets/dexopt/baseline.prof"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
    buildToolsVersion = "35.0.0"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.documentfile)

    // Hilt
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)

    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    // coil
    implementation(libs.coil.compose)

    // Gson
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)

    // CommonMark, for markdown rendering and parsing
    implementation(libs.commonmark.ext.autolink)
    implementation(libs.commonmark.ext.footnotes)
    implementation(libs.commonmark.ext.ins)
    implementation(libs.commonmark.ext.task.list.items)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.heading.anchor)
    implementation(libs.commonmark.ext.image.attributes)
    implementation(libs.commonmark.ext.yaml.front.matter)
    implementation(libs.commonmark)
    implementation("com.vladsch.flexmark:flexmark-html2md-converter:0.64.8")

}
