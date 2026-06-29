package dev.domus.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.BitmapPainter

fun appIconPainter(): BitmapPainter = BitmapPainter(appIconBitmap(256))

fun appIconBitmap(size: Int): ImageBitmap {
    val bmp = ImageBitmap(size, size)
    val canvas = Canvas(bmp)
    val s = size.toFloat()

    fun paint(c: Color) = Paint().apply { color = c; isAntiAlias = true }

    val blue = Color(0xFF1565C0)
    val white = Color.White

    // Circular background
    canvas.drawCircle(Offset(s / 2, s / 2), s / 2, paint(blue))

    // House body
    canvas.drawRect(s * 0.20f, s * 0.46f, s * 0.80f, s * 0.82f, paint(white))

    // Roof triangle
    val roof = Path()
    roof.moveTo(s * 0.50f, s * 0.15f)
    roof.lineTo(s * 0.10f, s * 0.48f)
    roof.lineTo(s * 0.90f, s * 0.48f)
    roof.close()
    canvas.drawPath(roof, paint(white))

    // Door cutout in background color
    val dw = s * 0.18f
    val dh = s * 0.24f
    val dx = s / 2 - dw / 2
    val dy = s * 0.82f - dh
    canvas.drawRoundRect(dx, dy, dx + dw, dy + dh, s * 0.04f, s * 0.04f, paint(blue))

    return bmp
}
