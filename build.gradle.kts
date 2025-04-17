// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenLocal()
    }
    dependencies {
        classpath("io.realm:realm-gradle-plugin:10.18.0")
//        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.2")
    }
}
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id ("io.realm.kotlin") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.dagger.hilt.android") version "2.56.1" apply false
    alias(libs.plugins.kotlin.compose) apply false
//    id("com.google.gms.google-services") version "4.4.0" apply false
}