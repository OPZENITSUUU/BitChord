package com.music.bitchord.data.updates

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.annotation.Keep
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.music.bitchord.MainActivity
import com.music.bitchord.R
import com.music.bitchord.data.AppUpdateChecker
import java.util.concurrent.TimeUnit

/** Schedules a network-aware daily update check that survives app process death and reboot. */
object UpdateNotificationScheduler {
    const val WORK_NAME = "void_music_daily_update_check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<UpdateNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(2, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

/** Checks only release metadata; it never fetches the APK or starts the installer. */
@Keep
class UpdateNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return when (AppUpdateChecker.check(applicationContext, force = true)) {
            AppUpdateChecker.CheckResult.UPDATE_AVAILABLE -> {
                val update = AppUpdateChecker.available.value
                    ?: AppUpdateChecker.restorePending(applicationContext)
                    ?: return Result.retry()
                postNotificationIfNeeded(update.version)
                Result.success()
            }
            AppUpdateChecker.CheckResult.FAILED -> Result.retry()
            AppUpdateChecker.CheckResult.UP_TO_DATE,
            AppUpdateChecker.CheckResult.NO_COMPATIBLE_APK,
            AppUpdateChecker.CheckResult.THROTTLED -> Result.success()
        }
    }

    private fun postNotificationIfNeeded(version: String) {
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val prefs = applicationContext.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE)
        if (!shouldNotifyVersion(version, prefs.getString(KEY_NOTIFIED_VERSION, null))) return
        ensureChannel()
        val openApp = Intent(applicationContext, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_UPDATE_FROM_NOTIFICATION
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            CONTENT_REQUEST_CODE,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(applicationContext.getString(R.string.update_notification_title, version))
            .setContentText(applicationContext.getString(R.string.update_notification_body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(applicationContext.getString(R.string.update_notification_body)),
            )
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        runCatching { notificationManager.notify(NOTIFICATION_ID, notification) }
            .onSuccess {
                prefs.edit().putString(KEY_NOTIFIED_VERSION, version).apply()
            }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.update_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = applicationContext.getString(R.string.update_notification_channel_description)
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "app_update_alerts"
        private const val NOTIFICATION_PREFS = "app_update_notifications"
        private const val KEY_NOTIFIED_VERSION = "notified_update_version"
        private const val NOTIFICATION_ID = 91027
        private const val CONTENT_REQUEST_CODE = 91028

        internal fun shouldNotifyVersion(version: String, lastNotifiedVersion: String?): Boolean =
            version.isNotBlank() && version != lastNotifiedVersion
    }
}
