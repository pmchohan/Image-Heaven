plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.mad_final_prep"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mad_final_prep"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")
    // Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx:21.0.0")
    // Firebase ui
    implementation ("com.firebaseui:firebase-ui-database:8.0.2")
    // Glide
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    // CameraX
    implementation ("androidx.camera:camera-core:1.3.4")
    implementation ("androidx.camera:camera-camera2:1.3.4")
    implementation ("androidx.camera:camera-lifecycle:1.3.4")
    implementation ("androidx.camera:camera-view:1.3.4")
    // Guava
    implementation ("com.google.guava:guava:32.0.1-android")
    // Material Design
    implementation("com.google.android.material:material:1.9.0")
    // Picasso
    implementation("com.squareup.picasso:picasso:2.8")
}