plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("io.realm.kotlin") version "2.3.0"
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
//    id("com.google.firebase.crashlytics")
//    id("com.google.gms.google-services")
}

android {
    namespace = "yurazhovnir.healthgraphs"
    compileSdk = 35

    defaultConfig {
        applicationId = "yurazhovnir.healthgraphs"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/INDEX.LIST")
    }
    flavorDimensions.add("default")
    productFlavors {
        create("live") {
            applicationId = "app.yurazhovnir"
            versionCode = 1
            versionName = "1.0.0"
            resValue("string", "app_name", "yurazhovnir")
            buildConfigField(
                "String",
                "SERVER_URL",
                "\"https://dev.yurazhovnir.app/\""
            )
            dimension = "default"
        }

        create("dev") {
            applicationId = "yurazhovnir.healthgraphs"
            versionCode = 1
            versionName = "1.0.0"
            resValue("string", "app_name", "Health Graphs")
            buildConfigField(
                "String",
                "SERVER_URL",
                "\"https://dev.yurazhovnir.app/\""
            )
            dimension = "default"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    viewBinding {
        enable = true
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    implementation("com.squareup.retrofit2:converter-moshi:2.4.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    implementation("io.realm.kotlin:library-base:2.3.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.19")
    implementation("com.jakewharton.rxrelay2:rxrelay:2.1.0")
    implementation("com.airbnb.android:lottie:3.5.0")
    implementation("org.greenrobot:eventbus:3.3.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.6.0")
    implementation("com.google.dagger:hilt-android:2.56.1")
    implementation("androidx.hilt:hilt-navigation-fragment:1.0.0")
    ksp("com.google.dagger:hilt-compiler:2.56.1")
    implementation("com.prolificinteractive:material-calendarview:1.4.3")
    implementation("com.kizitonwose.calendar:view:2.6.0")
    implementation("com.kizitonwose.calendar:compose:2.6.0")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha11")

//    implementation(platform("com.google.firebase:firebase-bom:31.0.2"))
//    implementation("com.google.firebase:firebase-analytics")
//    implementation("com.google.firebase:firebase-crashlytics")
//    implementation("com.google.firebase:firebase-database")

    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
    implementation("com.makeramen:roundedimageview:2.3.0")

}