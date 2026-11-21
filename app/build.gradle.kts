import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.ghsssss.ithomeadkill"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "io.github.ghsssss.ithomeadkill"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1-dev"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.5-beta03")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.5-beta03")
    testImplementation("junit:junit:4.13.2")
    // The Xposed API is supplied by LSPosed, never packaged in the module.
    compileOnly("de.robv.android.xposed:api:82")
}
