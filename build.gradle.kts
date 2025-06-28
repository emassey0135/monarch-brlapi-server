buildscript {
  repositories {
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
  }
  dependencies {
    classpath("com.android.tools.build:gradle:8.10.1")
    classpath("org.mozilla.rust-android-gradle:plugin:0.9.6")
    classpath(kotlin("gradle-plugin", version = "2.1.21"))
  }
}
allprojects {
  repositories {
    mavenCentral()
    google()
  }
}
