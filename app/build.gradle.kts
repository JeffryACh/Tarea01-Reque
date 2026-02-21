plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.tarea01_reque"
    // Usamos 35 o 36 dependiendo de la versión estable del SDK que tengas descargada
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tarea01_reque"
        minSdk = 26  // Permite instalar la app en el 99% de los teléfonos modernos
        targetSdk = 36 // Optimizado para Android 16
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- LIBRERÍAS BASE DE JETPACK COMPOSE ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // --- NUEVAS LIBRERÍAS REQUERIDAS PARA LA TAREA 1 ---

    // 1. Preferences DataStore (Para la Misión Técnica 2: Guardar el número de contacto)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 2. ViewModel Compose (Para la Guía Técnica: Conectar SensorViewModel con la UI)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // 3. Corrutinas (Para manejar la cuenta regresiva de 10 segundos en el servicio)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // --- LIBRERÍAS DE PRUEBA ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}