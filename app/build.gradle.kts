plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kyoten.kymusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kyoten.kymusic"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ SOPORTE PARA 16 KB - Solo arquitectura 64-bit
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true      // ✅ Activa R8 (ofuscación y reducción)
            isShrinkResources = true    // ✅ Reduce recursos no usados
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        // ✅ Ayuda con la compatibilidad de 16 KB
        resources {
            excludes += "**/lib/**"
        }
    }
}

dependencies {
    // ========== SPLASH ==========
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ========== EXOPLAYER (Media3) - Actualizado ==========
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")

    // ========== ANUNCIOS ==========
    implementation("com.google.android.gms:play-services-ads:23.5.0")

    // ========== COMPOSE BOM - Actualizado ==========
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))

    // ========== COMPOSE ==========
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation("androidx.compose.ui:ui-geometry")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation(libs.androidx.appcompat)
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ========== NAVEGACIÓN ==========
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // ========== LIFECYCLE ==========
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")

    // ========== ARCHIVOS ==========
    implementation("androidx.documentfile:documentfile:1.0.1")

    // ========== MEDIA ==========
    implementation("androidx.media:media:1.7.0")

    // ========== CORE KTX ==========
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")

    // ========== COIL ==========
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ========== VISUALIZADOR ==========
    //implementation("com.github.lincollincol:amplituda:2.2.3") // ✅ VERSIÓN CON SOPORTE 16 KB

    // ========== TESTS ==========
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}