import java.util.Properties
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  id("com.google.gms.google-services")
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

android {
    namespace = "com.ahad.foodapp"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ahad.foodapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        
        val mapboxAccessToken = localProps.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""
        resValue("string", "mapbox_access_token", mapboxAccessToken)
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = localProps.getProperty("KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD") ?: "password"
            keyAlias = localProps.getProperty("KEYSTORE_ALIAS") ?: System.getenv("KEYSTORE_ALIAS") ?: "release"
            keyPassword = localProps.getProperty("KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD") ?: "password"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
      resValues = true
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation("androidx.compose.material:material-icons-extended")
  implementation("io.coil-kt:coil-compose:2.5.0")
  implementation("com.airbnb.android:lottie-compose:6.4.0")
  // DataStore for Preferences
  implementation("androidx.datastore:datastore-preferences:1.0.0")
  implementation(libs.kotlinx.serialization.json)
  implementation("androidx.navigation:navigation-compose:2.7.7")

  // Firebase & Google Auth
  implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
  implementation("com.google.firebase:firebase-auth")
  implementation("com.google.firebase:firebase-firestore")
  implementation("com.google.firebase:firebase-database")
  implementation("com.google.android.gms:play-services-auth:21.0.0")
  implementation("com.google.android.gms:play-services-location:21.2.0")
  
  // Credential Manager for 1-Tap Auth
  implementation("androidx.credentials:credentials:1.3.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
  
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("io.appium:java-client:9.2.2")
  testImplementation("org.seleniumhq.selenium:selenium-java:4.20.0")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Mapbox Maps SDK & Compose Extension
  implementation("com.mapbox.maps:android:11.24.3")
  implementation("com.mapbox.extension:maps-compose:11.24.3")

  // Network
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

  // NFC EMV Library
  implementation("com.github.devnied.emvnfccard:library:3.0.1")

}
