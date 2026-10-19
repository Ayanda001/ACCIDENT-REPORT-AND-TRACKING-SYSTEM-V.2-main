package com.example.arts.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrHelper {

    private const val SCHEME_PREFIX = "arts://incident/"

    /** The content encoded into an incident's QR code. */
    fun buildIncidentPayload(incidentId: String): String = "$SCHEME_PREFIX$incidentId"

    /**
     * Extracts an incident id from scanned QR content, whether it's one of
     * this app's own codes (arts://incident/42) or just a bare id typed
     * onto a printed tag.
     */
    fun parseIncidentId(scannedContent: String): String? {
        val trimmed = scannedContent.trim()
        return when {
            trimmed.startsWith(SCHEME_PREFIX) -> trimmed.removePrefix(SCHEME_PREFIX).takeIf { it.isNotBlank() }
            else -> trimmed.takeIf { it.isNotBlank() && !it.contains("/") }
        }
    }

    /** Renders [content] as a black-on-white QR code bitmap, [size] pixels square. */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
