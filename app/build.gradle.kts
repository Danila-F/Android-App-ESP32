plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ciVersionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val ciKeystorePath = providers.gradleProperty("ANDROID_KEYSTORE_PATH").orNull
val ciKeystorePassword = providers.gradleProperty("ANDROID_KEYSTORE_PASSWORD").orNull
val ciKeyAlias = providers.gradleProperty("ANDROID_KEY_ALIAS").orNull
val ciKeyPassword = providers.gradleProperty("ANDROID_KEY_PASSWORD").orNull
val ciSigningEnabled = listOf(ciKeystorePath, ciKeystorePassword, ciKeyAlias, ciKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.danilaf.esp32controller"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.danilaf.esp32controller"
        minSdk = 26
        targetSdk = 36
        versionCode = ciVersionCode
        versionName = "1.0.$ciVersionCode"
    }

    signingConfigs {
        create("ci") {
            if (ciSigningEnabled) {
                storeFile = file(ciKeystorePath!!)
                storePassword = ciKeystorePassword
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (ciSigningEnabled) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
        release {
            isMinifyEnabled = false
            if (ciSigningEnabled) {
                signingConfig = signingConfigs.getByName("ci")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
