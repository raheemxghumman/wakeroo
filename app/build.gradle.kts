import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val supportEmail: String = providers.gradleProperty("wakeroo.supportEmail").orElse("").get()
val uploadKey: Properties? = rootProject.file("keystore.properties").takeIf { it.exists() }
    ?.let { f -> Properties().apply { f.inputStream().use(::load) } }

android {
    namespace = "com.wakeroo.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wakeroo.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Shown in Settings › Send feedback and the privacy policy. Set it in gradle.properties.
        buildConfigField("String", "SUPPORT_EMAIL", "\"$supportEmail\"")
    }

    signingConfigs {
        // The Play upload key lives outside the repo; keystore.properties (gitignored) points to it.
        if (uploadKey != null) create("upload") {
            storeFile = file(uploadKey.getProperty("storeFile"))
            storePassword = uploadKey.getProperty("storePassword")
            keyAlias = uploadKey.getProperty("keyAlias")
            keyPassword = uploadKey.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            // Without the upload key, release builds are debug-signed so they can still be tested.
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// The bundle that goes to Google Play must carry a real support email and the upload key.
tasks.matching { it.name == "bundleRelease" }.configureEach {
    val email = supportEmail
    val signed = uploadKey != null
    doFirst {
        if (email.isBlank()) throw GradleException("Set wakeroo.supportEmail in gradle.properties before building for Play.")
        if (!signed) throw GradleException("Add keystore.properties with the upload key before building for Play.")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.labeling)
    implementation(libs.billing.ktx)
    implementation(libs.play.review.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
