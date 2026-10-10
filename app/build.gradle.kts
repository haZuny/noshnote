plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

val releaseSigningValues = mapOf(
    "NOSHNOTE_KEYSTORE_PATH" to providers.environmentVariable("NOSHNOTE_KEYSTORE_PATH").orNull,
    "NOSHNOTE_KEYSTORE_PASSWORD" to providers.environmentVariable("NOSHNOTE_KEYSTORE_PASSWORD").orNull,
    "NOSHNOTE_KEY_ALIAS" to providers.environmentVariable("NOSHNOTE_KEY_ALIAS").orNull,
    "NOSHNOTE_KEY_PASSWORD" to providers.environmentVariable("NOSHNOTE_KEY_PASSWORD").orNull,
)
val hasReleaseSigning = releaseSigningValues.values.any { it != null }
if (hasReleaseSigning) {
    require(releaseSigningValues.values.all { !it.isNullOrEmpty() }) {
        "릴리스 서명 환경변수 네 항목을 모두 설정해야 합니다."
    }
}

android {
    namespace = "com.hazuny.noshnote"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hazuny.noshnote"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseSigningValues.getValue("NOSHNOTE_KEYSTORE_PATH")!!)
                storePassword = releaseSigningValues.getValue("NOSHNOTE_KEYSTORE_PASSWORD")
                keyAlias = releaseSigningValues.getValue("NOSHNOTE_KEY_ALIAS")
                keyPassword = releaseSigningValues.getValue("NOSHNOTE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // PR 검증에는 키가 필요 없고, 배포 작업에서만 실제 서명 정보를 전달한다.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition.korean)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
