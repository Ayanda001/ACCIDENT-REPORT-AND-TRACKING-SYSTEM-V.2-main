package com.example.arts.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Photos are shrunk and stored inside the Firestore document itself as a
 * data URL ("data:image/jpeg;base64,..."), the same format the website
 * uses, so a photo taken on the phone shows on the website and back.
 * Firestore documents are limited to 1 MB, so the size is kept small.
 */
object ImageStorageHelper {

    /**
     * @param maxDimension longest side, in pixels, to downscale to
     * @param quality JPEG quality, 0-100
     * @return a data URL, or null if the image could not be read
     */
    fun toDataUrl(context: Context, uri: Uri, maxDimension: Int = 900, quality: Int = 70): String? {
        val original = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val scale = minOf(1f, maxDimension.toFloat() / maxOf(original.width, original.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
        } else {
            original
        }

        val bytes = ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
        if (scaled !== original) original.recycle()
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decodeDataUrl(dataUrl: String?): Bitmap? {
        if (dataUrl.isNullOrBlank() || !dataUrl.startsWith("data:image")) return null
        return try {
            val bytes = Base64.decode(dataUrl.substringAfter("base64,"), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
