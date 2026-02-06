plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.eulcauink"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.eulcauink"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

// 放在 android { ... } 块的后面
androidComponents {
    onVariants { variant ->
        val name = "EulCauInk"
        val vName = android.defaultConfig.versionName ?: "dev"

        variant.outputs.forEach { output ->
            // 在新版 AGP 中，outputFileName 需要通过这种方式访问
            val mainOutput = output as? com.android.build.api.variant.impl.VariantOutputImpl
            mainOutput?.outputFileName?.set("${name}_v${vName}_${variant.name}.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.webkit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.webkit:webkit:1.9.0")
    implementation("com.google.code.gson:gson:2.11.0")
}