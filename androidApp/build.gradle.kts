import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

// Release signing comes from an existing JKS keystore, supplied via Gradle properties
// (CI passes these via -P flags backed by secrets). Builds without them still work
// locally — assembleRelease just produces an unsigned APK.
val releaseStoreFile = (project.findProperty("domus.signing.storeFile") as String?)
    ?.let { file(it) }
val releaseStorePassword = project.findProperty("domus.signing.storePassword") as String?
val releaseKeyAlias = project.findProperty("domus.signing.keyAlias") as String?
val releaseKeyPassword = project.findProperty("domus.signing.keyPassword") as String?
val hasReleaseSigning = releaseStoreFile != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

configure<ApplicationExtension> {
    namespace = "dev.domus.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.domus.android"
        minSdk = 31
        targetSdk = 37
        versionCode = 5
        versionName = "0.5.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        resources.excludes.add("META-INF/*")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.client.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
