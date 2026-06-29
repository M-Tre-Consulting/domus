import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

abstract class GenerateIconsTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        // BufferedImage/Graphics2D work in headless environments — no display needed.
        System.setProperty("java.awt.headless", "true")

        val dir = outputDir.get().asFile
        dir.mkdirs()

        // PNG (512×512) for Linux packages
        ImageIO.write(makeBitmap(512), "PNG", File(dir, "icon.png"))

        // ICO for Windows: single 256×256 image stored as an embedded PNG (Vista+ format).
        val pngBytes = ByteArrayOutputStream()
            .also { ImageIO.write(makeBitmap(256), "PNG", it) }
            .toByteArray()

        FileOutputStream(File(dir, "icon.ico")).use { out ->
            fun Int.le2(): ByteArray = byteArrayOf(and(0xFF).toByte(), shr(8).and(0xFF).toByte())
            fun Int.le4(): ByteArray = byteArrayOf(
                and(0xFF).toByte(), shr(8).and(0xFF).toByte(),
                shr(16).and(0xFF).toByte(), shr(24).and(0xFF).toByte(),
            )
            // ICONDIR header
            out.write(0.le2())              // reserved
            out.write(1.le2())              // type = 1 (icon)
            out.write(1.le2())              // count = 1 image
            // ICONDIRENTRY
            out.write(0); out.write(0)      // width=0 → 256, height=0 → 256
            out.write(0); out.write(0)      // colorCount=0, reserved=0
            out.write(1.le2())              // planes = 1
            out.write(32.le2())             // bitCount = 32
            out.write(pngBytes.size.le4())  // image data size
            out.write(22.le4())             // image offset = 6 (header) + 16 (entry) = 22
            // Embedded PNG data
            out.write(pngBytes)
        }
    }

    private fun drawIcon(g: java.awt.Graphics2D, size: Int) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val s = size.toDouble()

        // Circular background
        g.color = Color(0x1565C0)
        g.fillOval(0, 0, size, size)

        // House body
        g.color = Color.WHITE
        g.fillRect((s * 0.20).toInt(), (s * 0.46).toInt(), (s * 0.60).toInt(), (s * 0.36).toInt())

        // Roof triangle
        val rx = intArrayOf((s * 0.50).toInt(), (s * 0.10).toInt(), (s * 0.90).toInt())
        val ry = intArrayOf((s * 0.15).toInt(), (s * 0.48).toInt(), (s * 0.48).toInt())
        g.fillPolygon(rx, ry, 3)

        // Door cutout in background color
        g.color = Color(0x1565C0)
        val dw = (s * 0.18).toInt()
        val dh = (s * 0.24).toInt()
        val dx = (s / 2 - dw / 2).toInt()
        val dy = (s * 0.82 - dh).toInt()
        g.fillRoundRect(dx, dy, dw, dh, (s * 0.08).toInt(), (s * 0.08).toInt())
    }

    private fun makeBitmap(size: Int): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        drawIcon(g, size)
        g.dispose()
        return img
    }
}
