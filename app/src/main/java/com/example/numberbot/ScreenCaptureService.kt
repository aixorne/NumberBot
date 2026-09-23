package com.example.numberbot

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "START"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val ACTION_STOP = "STOP"
        private const val CHANNEL_ID = "number_bot"
        private const val NOTIFICATION_ID = 1001
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private val busy = AtomicBoolean(false)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_START) {
            startForeground(
                NOTIFICATION_ID,
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Number Bot")
                    .setContentText("กำลังค้นหาเลขและแตะอัตโนมัติ")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setOngoing(true)
                    .build()
            )
            startProjection(
                intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED),
                intent.getParcelableExtra(EXTRA_DATA)
            )
        }

        return START_NOT_STICKY
    }

    private fun startProjection(resultCode: Int, data: Intent?) {
        if (data == null) return

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
            .defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(
            width, height, PixelFormat.RGBA_8888, 2
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            if (!NumberAccessibilityService.enabled) return@setOnImageAvailableListener
            if (!busy.compareAndSet(false, true)) {
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }

            val image = reader.acquireLatestImage()
            if (image == null) {
                busy.set(false)
                return@setOnImageAvailableListener
            }

            val bitmap = imageToBitmap(image, width, height)
            image.close()

            if (bitmap != null) {
                recognize(bitmap)
            } else {
                busy.set(false)
            }
        }, handler)

        virtualDisplay = projection?.createVirtualDisplay(
            "NumberBotCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )
    }

    private fun recognize(bitmap: Bitmap) {
        val target = NumberAccessibilityService.nextNumber
        val input = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(input)
            .addOnSuccessListener { result ->
                if (!NumberAccessibilityService.enabled) return@addOnSuccessListener

                var found = false

                for (block in result.textBlocks) {
                    for (line in block.lines) {
                        val cleaned = line.text.trim()
                            .replace(" ", "")
                            .replace("\n", "")

                        // Exact match prevents "1" from accidentally matching "10", etc.
                        if (cleaned == target.toString()) {
                            val box = line.boundingBox ?: continue
                            val x = box.centerX().toFloat()
                            val y = box.centerY().toFloat()

                            if (NumberAccessibilityService.tap(x, y, 1L)) {
                                NumberAccessibilityService.nextNumber =
                                    if (target >= 50) {
                                        NumberAccessibilityService.enabled = false
                                        1
                                    } else {
                                        target + 1
                                    }
                                found = true
                                break
                            }
                        }
                    }
                    if (found) break
                }
            }
            .addOnCompleteListener {
                bitmap.recycle()
                busy.set(false)

                if (NumberAccessibilityService.enabled) {
                    // Small delay prevents hammering OCR while the game redraws.
                    handler.postDelayed({}, 8L)
                }
            }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height).also {
            bitmap.recycle()
        }
    }

    override fun onDestroy() {
        NumberAccessibilityService.enabled = false
        imageReader?.close()
        virtualDisplay?.release()
        projection?.stop()
        recognizer.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Number Bot",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }
}
