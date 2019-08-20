package com.improveit.face

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.microsoft.projectoxford.face.contract.Face

val BASE_URL_CONF = "https://westeurope.api.cognitive.microsoft.com"


fun drawFaceRectangleOnBitmap(mBitmap: Bitmap, facesDetected: Array<Face>?, name: String): Bitmap {

    val bitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    //Rectangle
    val paint = Paint()
    paint.isAntiAlias = true
    paint.style = Paint.Style.STROKE
    paint.color = Color.WHITE
    paint.strokeWidth = 12f

    if (facesDetected != null) {
        for (face in facesDetected) {
            val faceRectangle = face.faceRectangle
            canvas.drawRect(
                faceRectangle.left.toFloat(),
                faceRectangle.top.toFloat(),
                (faceRectangle.left + faceRectangle.width).toFloat(),
                (faceRectangle.top + faceRectangle.height).toFloat(),
                paint
            )
            drawTextOnCanvas(
                canvas,
                100,
                (faceRectangle.left + faceRectangle.width) / 2 + 100,
                faceRectangle.top + faceRectangle.height + 50,
                Color.WHITE,
                name
            )

        }
    }
    return bitmap
}

private fun drawTextOnCanvas(canvas: Canvas, textSize: Int, x: Int, y: Int, color: Int, name: String) {
    val paint = Paint()
    paint.isAntiAlias = true
    paint.style = Paint.Style.FILL
    paint.color = color
    paint.textSize = textSize.toFloat()

    val textWidth = paint.measureText(name)

    canvas.drawText(name, x - textWidth / 2, (y - textSize / 2).toFloat(), paint)
}

