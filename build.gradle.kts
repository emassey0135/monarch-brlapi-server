buildscript {
  repositories {
    mavenCentral()
    google()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:8.10.1")
    classpath(kotlin("gradle-plugin", version = "2.1.21"))
  }
}
allprojects {
  repositories {
    mavenCentral()
    google()
  }
}
