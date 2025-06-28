plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.compose") version "2.1.21"
  kotlin("plugin.serialization") version "2.1.21"
  id("org.mozilla.rust-android-gradle.rust-android")
}
android {
  compileSdk = 36
  namespace = "dev.emassey0135.monarchBrlapiServer"
  ndkVersion = "29.0.13599879"
  buildFeatures {
    aidl = true
    compose = true
    viewBinding = true
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  defaultConfig {
    applicationId = "dev.emassey0135.monarchBrlapiServer"
    minSdk = 33
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }
  kotlinOptions {
    jvmTarget = "21"
  }
}
cargo {
  module = "../rust"
  libname = "monarch_brlapi_server"
  profile = "release"
  targets = listOf("arm64")
}
dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
  implementation(kotlin("stdlib"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
  val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
  implementation(composeBom)
  implementation("androidx.compose.material3:material3")
  implementation("androidx.activity:activity-compose:1.10.1")
}
afterEvaluate {
  tasks.named("javaPreCompileDebug") {
    dependsOn("cargoBuild")
  }
  tasks.named("javaPreCompileRelease") {
    dependsOn("cargoBuild")
  }
}
