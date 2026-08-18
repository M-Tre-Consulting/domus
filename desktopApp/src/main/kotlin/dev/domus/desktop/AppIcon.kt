package dev.domus.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlin.math.cos
import kotlin.math.sin

fun appIconPainter(): BitmapPainter = BitmapPainter(appIconBitmap(256))

// Mirrors the Android adaptive icon (ic_launcher_background/foreground.xml): a monogram of a
// house roofline, a lightning bolt (smart/power) and an open ring reading as a "D" (Domus),
// each in its own flat accent color, over a pastel color-blob backdrop. Coordinates are lifted
// straight from that 108x108 vector viewport so all platforms stay visually identical.
fun appIconBitmap(size: Int): ImageBitmap {
    val bmp = ImageBitmap(size, size)
    val canvas = Canvas(bmp)
    val s = size.toFloat()
    fun v(n: Float) = n / 108f * s

    fun fillPaint(c: Color, alpha: Float = 1f) = Paint().apply {
        isAntiAlias = true; color = c; this.alpha = alpha
    }

    // Squircle clip, so the pastel blobs behind it don't spill into hard square corners.
    canvas.save()
    val squircle = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                0f, 0f, s, s,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(v(24f), v(24f)),
            )
        )
    }
    canvas.clipPath(squircle)

    canvas.drawRect(androidx.compose.ui.geometry.Rect(0f, 0f, s, s), fillPaint(Color(0xFFF4F6FC)))
    canvas.drawCircle(Offset(v(28f), v(26f)), v(52f), fillPaint(Color(0xFFC3D7F8), 0.85f))
    canvas.drawCircle(Offset(v(88f), v(58f)), v(48f), fillPaint(Color(0xFFCDEAD1), 0.80f))
    canvas.drawCircle(Offset(v(40f), v(92f)), v(50f), fillPaint(Color(0xFFFCEAB6), 0.80f))
    canvas.restore()

    // Roofline: open stroke, left wall up to a peak, down to a right wall stub - a
    // recognizable house outline (roof + two wall stubs), not just a checkmark.
    val roofline = Path().apply {
        moveTo(v(32f), v(63f))
        lineTo(v(32f), v(46f))
        lineTo(v(45f), v(33f))
        lineTo(v(58f), v(46f))
        lineTo(v(58f), v(59f))
    }
    canvas.drawPath(roofline, strokePaint(Color(0xFF4E7FE0), v(7f)))

    // "D" ring: open circular arc (center 62,52 r 15), gap facing the house/bolt, same
    // stroke weight as the roofline so the two read as a balanced pair. Approximated as a
    // dense polyline so every platform renders the identical curve.
    val ring = Path()
    val ringCx = v(62f); val ringCy = v(52f); val ringR = v(15f)
    var first = true
    var deg = 225f
    while (deg <= 495f) {
        val rad = Math.toRadians(deg.toDouble())
        val x = ringCx + ringR * cos(rad).toFloat()
        val y = ringCy + ringR * sin(rad).toFloat()
        if (first) { ring.moveTo(x, y); first = false } else ring.lineTo(x, y)
        deg += 5f
    }
    canvas.drawPath(ring, strokePaint(Color(0xFF55A66B), v(7f)))

    // Bolt: solid fill, drawn last so it binds the composition together on top.
    val bolt = Path().apply {
        moveTo(v(43.83f), v(76.62f))
        lineTo(v(47.99f), v(61.07f))
        lineTo(v(39.11f), v(58.69f))
        lineTo(v(60.17f), v(33.38f))
        lineTo(v(56.01f), v(48.93f))
        lineTo(v(64.89f), v(51.31f))
        close()
    }
    canvas.drawPath(bolt, fillPaint(Color(0xFFFFC145)))

    return bmp
}

private fun strokePaint(c: Color, width: Float) = Paint().apply {
    isAntiAlias = true
    color = c
    style = PaintingStyle.Stroke
    strokeWidth = width
    strokeCap = StrokeCap.Round
    strokeJoin = StrokeJoin.Round
}
