import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(key: String): String? =
    System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: localProps.getProperty(key)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.laxy.ecgrate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.laxy.ecgrate"
        minSdk = 24
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    val keystoreFile = secret("KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    val keystorePassword = secret("KEYSTORE_PASSWORD")
    val signKeyAlias = secret("KEY_ALIAS")
    val signKeyPassword = secret("KEY_PASSWORD")
    val releaseSigning = if (keystoreFile != null && keystorePassword != null && signKeyAlias != null && signKeyPassword != null) {
        signingConfigs.create("release") {
            storeFile = keystoreFile
            storePassword = keystorePassword
            keyAlias = signKeyAlias
            keyPassword = signKeyPassword
        }
    } else null

    buildTypes {
        debug {
            isMinifyEnabled = false
            releaseSigning?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
            releaseSigning?.let { signingConfig = it }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
