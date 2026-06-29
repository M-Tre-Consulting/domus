import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
}

compose.desktop {
    application {
        mainClass = "dev.domus.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
            packageName = "Domus"
            packageVersion = (project.findProperty("packageVersion") as String?) ?: "0.8.0"

            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}

// Generates icon files (PNG + ICO) for native distributions.
// Drawing logic lives in buildSrc/src/main/kotlin/GenerateIconsTask.kt (full JDK access there).
// Runs automatically before any packaging task; also: ./gradlew :desktopApp:generateIcons
val generateIcons = tasks.register<GenerateIconsTask>("generateIcons") {
    outputDir.set(project.file("icons"))
}

afterEvaluate {
    listOf("packageMsi", "packageDeb", "packageRpm", "packageAppImage", "packageDmg",
           "createDistributable", "createReleaseDistributable").forEach { name ->
        tasks.findByName(name)?.dependsOn(generateIcons)
    }
}

kotlin {
    jvmToolchain(21)
}
