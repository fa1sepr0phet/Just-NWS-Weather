package com.nwsweather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import android.util.AtomicFile
import android.view.View
import android.widget.RemoteViews
import androidx.work.*
import com.nwsweather.MainActivity
import com.nwsweather.data.local.AppDatabase
import com.nwsweather.data.local.SettingsManager
import com.nwsweather.myapp.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.currentCoroutineContext

class ForecastGraphWidgetReceiver : AppWidgetProvider() {
    override fun onEnabled(context: Context) { ForecastGraphWorker.schedule(context) }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = graphViews(context)
        manager.updateAppWidget(ids, views)
        ForecastGraphWorker.schedule(context)
        ForecastGraphWorker.enqueue(context)
    }
    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("forecast_graph_periodic")
        WorkManager.getInstance(context).cancelUniqueWork("forecast_graph_refresh")
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.nwsweather.REFRESH_GRAPH") ForecastGraphWorker.enqueue(context, force = true)
    }
}

private fun graphIds(context: Context): IntArray = AppWidgetManager.getInstance(context)
    .getAppWidgetIds(ComponentName(context, ForecastGraphWidgetReceiver::class.java))

private fun graphViews(context: Context): RemoteViews = RemoteViews(context.packageName, R.layout.forecast_graph_widget).apply {
    val openApp = PendingIntent.getActivity(context, 201, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    setOnClickPendingIntent(R.id.graph_image, openApp)
    setOnClickPendingIntent(R.id.graph_title, openApp)
    val refresh = Intent(context, ForecastGraphWidgetReceiver::class.java).setAction("com.nwsweather.REFRESH_GRAPH")
    setOnClickPendingIntent(R.id.graph_refresh, PendingIntent.getBroadcast(context, 202, refresh, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
}

class ForecastGraphWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        refreshMutex.withLock { updateGraph() }
    }

    private suspend fun updateGraph(): Result {
        val context = applicationContext
        if (graphIds(context).isEmpty()) return Result.success()
        val dao = AppDatabase.getInstance(context).weatherSnapshotDao()
        val snapshot = dao.getLatest()
        if (snapshot == null) {
            AppWidgetManager.getInstance(context).updateAppWidget(graphIds(context), graphViews(context).apply {
                setTextViewText(R.id.graph_status, "Open the app and select a location.")
            })
            return Result.success()
        }
        val unit = SettingsManager(context).unit.value
        val page = forecastGraphPage(snapshot.latitude, snapshot.longitude, unit)
        val key = MessageDigest.getInstance("SHA-256").digest(page.toString().toByteArray()).joinToString("") { "%02x".format(it) }
        val directory = File(context.cacheDir, "forecast_graphs").apply { mkdirs() }
        val file = File(directory, "$key.png")
        var failed = false
        try {
            val age = System.currentTimeMillis() - file.lastModified()
            if (!file.exists() || age !in 0 until TimeUnit.MINUTES.toMillis(30) || inputData.getBoolean("force", false)) {
                val html = fetch(page.toString(), 1_000_000).toString(Charsets.UTF_8)
                val imageUrl = extractForecastGraphImage(html, page) ?: error("NWS graph image not found")
                val image = fetch(imageUrl.toString(), 2_000_000)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(image, 0, image.size, bounds)
                check(bounds.outWidth in 1..1600 && bounds.outHeight in 1..1600) { "Invalid graph image" }
                currentCoroutineContext().ensureActive()
                val atomic = AtomicFile(file)
                val output = atomic.startWrite()
                try { output.write(image); atomic.finishWrite(output) }
                catch (e: Exception) { atomic.failWrite(output); throw e }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { failed = true }
        currentCoroutineContext().ensureActive()
        // A location/unit change while downloading must not paint the previous location's graph.
        val selected = dao.getLatest()
        if (selected?.latitude != snapshot.latitude || selected.longitude != snapshot.longitude || SettingsManager(context).unit.value != unit) {
            return Result.success()
        }
        val age = System.currentTimeMillis() - file.lastModified()
        val bitmap = if (file.exists() && age in 0..TimeUnit.HOURS.toMillis(24)) BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth > 1000 || bounds.outHeight > 1000) inSampleSize = 2
        }) else null
        val views = graphViews(context).apply {
            setTextViewText(R.id.graph_title, "${snapshot.locationName} \u00B7 NWS graph")
            setTextViewText(R.id.graph_window, "48-hour forecast starting +59h \u00B7 Tap graph to enlarge")
            val open = Intent(Intent.ACTION_VIEW, page.toString().toUri())
            setOnClickPendingIntent(R.id.graph_image, PendingIntent.getActivity(context, 203, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            setViewVisibility(R.id.graph_image, if (bitmap != null) View.VISIBLE else View.GONE)
            if (bitmap != null) {
                setImageViewBitmap(R.id.graph_image, bitmap)
                val time = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(file.lastModified()))
                setTextViewText(R.id.graph_status, "${if (failed) "Refresh failed; cached" else "Downloaded"} $time")
            } else setTextViewText(R.id.graph_status, "Graph unavailable. Tap Refresh to retry.")
        }
        AppWidgetManager.getInstance(context).updateAppWidget(graphIds(context), views)
        return if (failed) Result.retry() else Result.success()
    }

    private fun fetch(url: String, limit: Long): ByteArray {
        val request = Request.Builder().url(url).header("User-Agent", "(Just NWS Weather, falsepr0phet@protonmail.com)").build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Graph HTTP ${response.code}" }
            val body = response.body ?: error("Empty graph response")
            check(body.contentLength() <= limit) { "Graph response too large" }
            val source = body.source()
            source.request(limit + 1)
            check(source.buffer.size <= limit) { "Graph response too large" }
            source.readByteArray()
        }
    }

    companion object {
        private val refreshMutex = Mutex()
        private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
        fun clearCache(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("forecast_graph_refresh")
            File(context.cacheDir, "forecast_graphs").deleteRecursively()
        }
        fun schedule(context: Context) {
            if (graphIds(context).isEmpty()) return
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("forecast_graph_periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ForecastGraphWorker>(30, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
        }
        fun enqueue(context: Context, force: Boolean = false) {
            if (graphIds(context).isEmpty()) return
            WorkManager.getInstance(context).enqueueUniqueWork("forecast_graph_refresh", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ForecastGraphWorker>().setInputData(workDataOf("force" to force))
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES).build())
        }
    }
}
