package com.motoscope.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

data class PreparedCaptureImage(val width: Int, val height: Int, val orientationDegrees: Int)

/** Keeps the original JPEG untouched and writes a bounded, orientation-correct viewer derivative. */
object CaptureImageProcessor {
    private const val DISPLAY_MAX_EDGE = 1280
    private const val DECODE_MAX_EDGE = 2048

    fun prepareDisplayImage(original: File, destination: File): PreparedCaptureImage {
        require(original.isFile && original.canRead() && original.length() > 0L) { "Camera photo is missing" }
        val exif = ExifInterface(original)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(original.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Camera photo is not a readable image" }
        val sampleSize = sampleSize(bounds.outWidth, bounds.outHeight, DECODE_MAX_EDGE)
        val decoded = BitmapFactory.decodeFile(
            original.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: error("Could not decode camera photo")

        val matrix = orientationMatrix(orientation)
        val upright = if (matrix == null) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, matrix, true,
        ).also { if (it !== decoded) decoded.recycle() }
        val scale = minOf(1f, DISPLAY_MAX_EDGE.toFloat() / max(upright.width, upright.height))
        val output = if (scale < 1f) Bitmap.createScaledBitmap(
            upright,
            (upright.width * scale).toInt().coerceAtLeast(1),
            (upright.height * scale).toInt().coerceAtLeast(1),
            true,
        ).also { if (it !== upright) upright.recycle() } else upright

        try {
            FileOutputStream(destination).use { stream ->
                check(output.compress(Bitmap.CompressFormat.JPEG, 88, stream)) { "Could not prepare viewer photo" }
                stream.fd.sync()
            }
            val verify = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, verify)
            check(verify.outWidth > 0 && verify.outHeight > 0) { "Prepared viewer photo failed validation" }
            return PreparedCaptureImage(output.width, output.height, orientationDegrees(orientation))
        } finally {
            output.recycle()
        }
    }

    fun isReadableDisplayImage(file: File): Boolean {
        if (!file.isFile || !file.canRead() || file.length() <= 0L) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        return bounds.outWidth in 1..DISPLAY_MAX_EDGE && bounds.outHeight in 1..DISPLAY_MAX_EDGE
    }

    fun isReadableOriginalImage(file: File): Boolean {
        if (!file.isFile || !file.canRead() || file.length() <= 0L) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > maxEdge) sample *= 2
        return sample
    }

    private fun orientationDegrees(orientation: Int): Int = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_TRANSPOSE -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSVERSE -> 270
        else -> 0
    }

    private fun orientationMatrix(orientation: Int): Matrix? = when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { setScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { setRotate(180f) }
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { setScale(1f, -1f) }
        ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { setRotate(90f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { setRotate(90f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply { setRotate(270f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { setRotate(270f) }
        else -> null
    }
}
