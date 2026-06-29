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

// Generates icon files for native distributions using Java2D.
// Runs automatically before any packaging task; also callable via ./gradlew :desktopApp:generateIcons
val generateIcons by tasks.registering {
    val iconsDir = project.file("icons")
    outputs.dir(iconsDir)
    doLast {
        fun drawIcon(g: java.awt.Graphics2D, size: Int) {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            val s = size.toDouble()

            // Background circle
            g.color = java.awt.Color(0x1565C0)
            g.fillOval(0, 0, size, size)

            // House body
            g.color = java.awt.Color.WHITE
            g.fillRect((s * 0.20).toInt(), (s * 0.46).toInt(), (s * 0.60).toInt(), (s * 0.36).toInt())

            // Roof triangle
            val rx = intArrayOf((s * 0.50).toInt(), (s * 0.10).toInt(), (s * 0.90).toInt())
            val ry = intArrayOf((s * 0.15).toInt(), (s * 0.48).toInt(), (s * 0.48).toInt())
            g.fillPolygon(rx, ry, 3)

            // Door cutout
            g.color = java.awt.Color(0x1565C0)
            val dw = (s * 0.18).toInt()
            val dh = (s * 0.24).toInt()
            val dx = (s / 2 - dw / 2).toInt()
            val dy = (s * 0.82 - dh).toInt()
            g.fillRoundRect(dx, dy, dw, dh, (s * 0.08).toInt(), (s * 0.08).toInt())
        }

        fun makeBitmap(size: Int): java.awt.image.BufferedImage {
            val img = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            drawIcon(g, size)
            g.dispose()
            return img
        }

        iconsDir.mkdirs()

        // PNG for Linux (512×512)
        javax.imageio.ImageIO.write(makeBitmap(512), "PNG", File(iconsDir, "icon.png"))

        // ICO for Windows — single 256×256 image stored as embedded PNG (Vista+ format)
        val pngBytes = java.io.ByteArrayOutputStream()
            .also { javax.imageio.ImageIO.write(makeBitmap(256), "PNG", it) }
            .toByteArray()
        java.io.FileOutputStream(File(iconsDir, "icon.ico")).use { out ->
            fun Int.le2() = byteArrayOf(and(0xFF).toByte(), shr(8).and(0xFF).toByte())
            fun Int.le4() = byteArrayOf(
                and(0xFF).toByte(), shr(8).and(0xFF).toByte(),
                shr(16).and(0xFF).toByte(), shr(24).and(0xFF).toByte()
            )
            out.write(0.le2())              // reserved
            out.write(1.le2())              // type = 1 (icon)
            out.write(1.le2())              // count = 1 image
            out.write(0); out.write(0)      // width=0 → 256, height=0 → 256
            out.write(0); out.write(0)      // colorCount=0, reserved=0
            out.write(1.le2())              // planes = 1
            out.write(32.le2())             // bitCount = 32
            out.write(pngBytes.size.le4())  // image data size
            out.write(22.le4())             // image offset = 6 (header) + 16 (one entry) = 22
            out.write(pngBytes)
        }
    }
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
