plugins {
    id("com.android.application")
}

android {
    namespace = "com.littleorbit.bigorbit"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.littleorbit.bigorbit"
        minSdk = 29
        targetSdk = 36
        versionCode = 3
        versionName = "1.3.0"
        manifestPlaceholders["appLabel"] = "Big Orbit"
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        buildConfigField("String", "API_BASE_URL", "\"https://lil-orb.pax-kun.com/api\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "Big Orbit Debug"
        }
        create("smoke") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".smoke"
            versionNameSuffix = "-qa"
            manifestPlaceholders["appLabel"] = "Big Orbit QA"
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:18180/api\"")
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.work:work-runtime:2.10.3")
    implementation("com.google.android.material:material:1.14.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
