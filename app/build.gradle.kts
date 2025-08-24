import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.0"
}

// Load API keys from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// Function to safely get property with fallback
fun getLocalProperty(key: String, fallback: String = ""): String {
    return localProperties.getProperty(key) ?: fallback
}

android {
    namespace = "com.vishruth.key1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vishruth.key1"
        minSdk = 24
        targetSdk = 35
        versionCode = 11
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Add API keys to BuildConfig (secured)
        buildConfigField("String", "GEMINI_PRIMARY_API_KEY", "\"${getLocalProperty("GEMINI_PRIMARY_API_KEY", "")}\"")
        buildConfigField("String", "GEMINI_BACKUP_API_KEY_1", "\"${getLocalProperty("GEMINI_BACKUP_API_KEY_1", "")}\"")
        buildConfigField("String", "GEMINI_BACKUP_API_KEY_2", "\"${getLocalProperty("GEMINI_BACKUP_API_KEY_2", "")}\"")
        buildConfigField("String", "GPT5_API_KEY", "\"${getLocalProperty("GPT5_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Lifecycle and ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    
    // Google Generative AI SDK for Gemini (Updated to new unified SDK for Gemini 2.0 support)
    // Note: Migrated from deprecated 'com.google.ai.client.generativeai:generativeai' 
    // to new unified SDK as recommended by Google
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // JSON parsing and serialization
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // SharedPreferences for settings
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // Permission handling
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    
    // Responsive design support
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2")
    implementation("androidx.compose.foundation:foundation:1.5.4")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}