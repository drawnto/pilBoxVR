plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}


android {
    packagingOptions.jniLibs.useLegacyPackaging = true;

    namespace = "de.software_lab.pilbox"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "de.software_lab.pilbox"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }

    sourceSets {
        this.getByName("main") {
            jniLibs.srcDir("src/main/jniLibs")
        }

    }

}



dependencies {
    implementation(files("lib/core.jar"))
}