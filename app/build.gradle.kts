import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.MyApp.Spoonful"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.MyApp.Spoonful"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Load Unsplash API key from properties file
        val unsplashPropsFile = rootProject.file("app/unsplash_key.properties")
        val unsplashProps = Properties()
        if (unsplashPropsFile.exists()) {
            unsplashProps.load(unsplashPropsFile.inputStream())
            val unsplashKey = unsplashProps["UNSPLASH_ACCESS_KEY"] as String? ?: ""
            buildConfigField("String", "UNSPLASH_ACCESS_KEY", unsplashKey.let { '"' + it + '"' })
        } else {
            buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.foundation:foundation")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.coil.compose)
    implementation(libs.navigation.compose)
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.google.accompanist:accompanist-flowlayout:0.30.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
}

apply(plugin = "com.google.gms.google-services")