plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

// The NSRPlay key is read from the git-ignored .env file, never hardcoded in a
// source file. It is still a client-side key: for a public release, route the
// API through your own small proxy instead of shipping it in the APK.
val envFile = rootProject.file(".env")
val envValues: Map<String, String> = if (envFile.exists()) {
  envFile.readLines()
    .mapNotNull { line ->
      val trimmed = line.trim()
      if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("=")) return@mapNotNull null
      trimmed.substringBefore("=").trim() to trimmed.substringAfter("=").trim()
    }
    .toMap()
} else emptyMap()

val nsrPlayApiKey: String = envValues["NSRPLAY_API_KEY"].orEmpty()
val nsrPlayBaseUrl: String = envValues["NSRPLAY_BASE_URL"]
  ?.takeIf { it.isNotBlank() }
  ?: "https://nsrplay.space"
val vimeusApiKey: String = envValues["VIMEUS_API_KEY"].orEmpty()
val vimeusViewKey: String = envValues["VIMEUS_VIEW_KEY"].orEmpty()
val vimeusBaseUrl: String = envValues["VIMEUS_BASE_URL"]
  ?.takeIf { it.isNotBlank() }
  ?: "https://vimeus.com"
val unlimplayTmdbApiKey: String = envValues["UNLIMPLAY_TMDB_API_KEY"].orEmpty()
val unlimplayBaseUrl: String = envValues["UNLIMPLAY_BASE_URL"]
  ?.takeIf { it.isNotBlank() }
  ?: "https://unlimplay.com"

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.blooderstv.kjmnqx"
    minSdk = 24
    targetSdk = 36
    versionCode = 25
    versionName = "2.23"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField(
      "String",
      "NSRPLAY_API_KEY",
      "\"${nsrPlayApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    )
    buildConfigField("String", "NSRPLAY_BASE_URL", "\"$nsrPlayBaseUrl\"")
    buildConfigField(
      "String",
      "VIMEUS_API_KEY",
      "\"${vimeusApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    )
    buildConfigField(
      "String",
      "VIMEUS_VIEW_KEY",
      "\"${vimeusViewKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    )
    buildConfigField("String", "VIMEUS_BASE_URL", "\"$vimeusBaseUrl\"")
    buildConfigField(
      "String",
      "UNLIMPLAY_TMDB_API_KEY",
      "\"${unlimplayTmdbApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    )
    buildConfigField("String", "UNLIMPLAY_BASE_URL", "\"$unlimplayBaseUrl\"")
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.coil.compose)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.hls)
  implementation(libs.androidx.media3.datasource)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
