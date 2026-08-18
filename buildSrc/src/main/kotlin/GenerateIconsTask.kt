import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin

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

    // Mirrors the Android adaptive icon (ic_launcher_background/foreground.xml) and the desktop
    // window icon (AppIcon.kt): a monogram of a house roofline, a lightning bolt (smart/power)
    // and an open ring reading as a "D" (Domus), each in its own flat accent color, over a
    // pastel color-blob backdrop. Coordinates are lifted straight from that 108x108 viewport.
    private fun drawIcon(g: java.awt.Graphics2D, size: Int) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val s = size.toFloat()
        fun v(n: Float) = n / 108f * s

        // Squircle clip, so the pastel blobs behind it don't spill into hard square corners.
        val squircle = RoundRectangle2D.Float(0f, 0f, s, s, v(48f), v(48f))
        val oldClip = g.clip
        g.clip(squircle)

        g.paint = Color(0xF4F6FC)
        g.fill(squircle.bounds2D)
        g.paint = withAlpha(Color(0xC3D7F8), 0.85f)
        g.fill(circle(v(28f), v(26f), v(52f)))
        g.paint = withAlpha(Color(0xCDEAD1), 0.80f)
        g.fill(circle(v(88f), v(58f), v(48f)))
        g.paint = withAlpha(Color(0xFCEAB6), 0.80f)
        g.fill(circle(v(40f), v(92f), v(50f)))
        g.clip = oldClip

        // Roofline: open stroke, left wall up to a peak, down to a right wall stub - a
        // recognizable house outline (roof + two wall stubs), not just a checkmark.
        val roofline = Path2D.Float()
        roofline.moveTo(v(32f), v(63f))
        roofline.lineTo(v(32f), v(46f))
        roofline.lineTo(v(45f), v(33f))
        roofline.lineTo(v(58f), v(46f))
        roofline.lineTo(v(58f), v(59f))
        g.paint = Color(0x4E7FE0)
        g.stroke = BasicStroke(v(7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(roofline)

        // "D" ring: open circular arc (center 62,52 r 15), gap facing the house/bolt, same
        // stroke weight as the roofline so the two read as a balanced pair. Approximated as a
        // dense polyline so every platform renders the identical curve.
        val ring = Path2D.Float()
        val ringCx = v(62f); val ringCy = v(52f); val ringR = v(15f)
        var first = true
        var deg = 225.0
        while (deg <= 495.0) {
            val rad = Math.toRadians(deg)
            val x = ringCx + ringR * cos(rad).toFloat()
            val y = ringCy + ringR * sin(rad).toFloat()
            if (first) { ring.moveTo(x, y); first = false } else ring.lineTo(x, y)
            deg += 5.0
        }
        g.paint = Color(0x55A66B)
        g.stroke = BasicStroke(v(7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(ring)

        // Bolt: solid fill, drawn last so it binds the composition together on top.
        val bolt = Path2D.Float()
        bolt.moveTo(v(43.83f), v(76.62f))
        bolt.lineTo(v(47.99f), v(61.07f))
        bolt.lineTo(v(39.11f), v(58.69f))
        bolt.lineTo(v(60.17f), v(33.38f))
        bolt.lineTo(v(56.01f), v(48.93f))
        bolt.lineTo(v(64.89f), v(51.31f))
        bolt.closePath()
        g.paint = Color(0xFFC145)
        g.fill(bolt)
    }

    private fun circle(cx: Float, cy: Float, r: Float) = Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2)

    private fun withAlpha(c: Color, alpha: Float) =
        Color(c.red, c.green, c.blue, (alpha * 255).toInt())

    private fun makeBitmap(size: Int): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        drawIcon(g, size)
        g.dispose()
        return img
    }
}
