package com.nwsweather.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.nwsweather.MainActivity

class NotificationHelper(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        val alertsChannel = NotificationChannel(
            CHANNEL_ID,
            "Weather Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for severe weather alerts from the NWS"
        }
        notificationManager.createNotificationChannel(alertsChannel)

        val statusChannel = NotificationChannel(
            STATUS_BAR_CHANNEL_ID,
            "Status Bar Temperature",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Displays the current temperature in the status bar"
            setShowBadge(false)
            setSound(null, null) // Keep it silent
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(statusChannel)
    }

    fun showWeatherAlert(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Ensure sound/vibe for real alerts
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun updateStatusBarTemperature(
        temp: Int,
        locationName: String,
        forecast: String,
        isDaytime: Boolean
    ) {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val bitmap = createTemperatureBitmap(temp)
        val icon = IconCompat.createWithBitmap(bitmap)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, STATUS_BAR_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle("$temp\u00B0 in $locationName")
            .setContentText(forecast)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setSilent(true) // Silent to prevent vibration/sound on every update
            .setShowWhen(false)
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(STATUS_BAR_NOTIFICATION_ID, notification)
    }

    fun cancelStatusBarTemperature() {
        notificationManager.cancel(STATUS_BAR_NOTIFICATION_ID)
    }

    private fun createTemperatureBitmap(temp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (24f * density).toInt()

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw the temperature at the largest readable size for the small status icon canvas.
        val paint = Paint().apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        val text = "$temp\u00B0"
        var textSize = 20f * density
        paint.textSize = textSize

        val maxWidth = sizePx - (2f * density)
        while (paint.measureText(text) > maxWidth && textSize > 8f * density) {
            textSize -= 0.5f * density
            paint.textSize = textSize
        }

        val xPos = sizePx / 2f
        val yPos = (sizePx / 2f) - ((paint.ascent() + paint.descent()) / 2f)
        canvas.drawText(text, xPos, yPos, paint)

        return bitmap
    }

    companion object {
        private const val CHANNEL_ID = "weather_alerts"
        private const val STATUS_BAR_CHANNEL_ID = "status_bar_temp_v6"
        private const val NOTIFICATION_ID = 1001
        private const val STATUS_BAR_NOTIFICATION_ID = 1002
    }
}
