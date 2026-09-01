plugins {
    id("com.android.application")
}

android {
    namespace = "reader.aigd"
    compileSdk = 37

    defaultConfig {
        applicationId = "reader.aigd"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "3"
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



}

dependencies {

    implementation("com.startapp:inapp-sdk:5.3.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.activity.ktx)
    implementation(libs.constraintlayout)
}