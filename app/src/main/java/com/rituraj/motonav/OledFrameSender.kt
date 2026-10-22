
package com.rituraj.motonav

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object OledFrameSender {

    private const val WIDTH = 128
    private const val HEIGHT = 64
    private const val FRAME_SIZE = WIDTH * HEIGHT / 8

    private val client = OkHttpClient()

    // Format: 8 pixels per byte, most-significant bit first.
    // White = 1, black = 0.
    fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        require(bitmap.width == WIDTH && bitmap.height == HEIGHT) {
            "Bitmap must be exactly 128x64"
        }

        val output = ByteArray(FRAME_SIZE)

        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val pixel = bitmap.getPixel(x, y)

                val red = android.graphics.Color.red(pixel)
                val green = android.graphics.Color.green(pixel)
                val blue = android.graphics.Color.blue(pixel)

                val brightness =
                    (299 * red + 587 * green + 114 * blue) / 1000

                // White pixels are ON.
                if (brightness >= 128) {
                    val index = y * (WIDTH / 8) + x / 8
                    val mask = 0x80 shr (x % 8)

                    output[index] =
                        (output[index].toInt() or mask).toByte()
                }
            }
        }

        return output
    }

    suspend fun sendFrame(
        bitmap: Bitmap,
        espIpAddress: String
    ): String = withContext(Dispatchers.IO) {

        val bytes = bitmapToBytes(bitmap)

        check(bytes.size == FRAME_SIZE)

        val request = Request.Builder()
            .url("http://$espIpAddress/frame")
            .post(
                bytes.toRequestBody(
                    "application/octet-stream".toMediaType()
                )
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("ESP8266 HTTP error: ${response.code}")
            }

            response.body?.string() ?: "OK"
        }
    }
}
