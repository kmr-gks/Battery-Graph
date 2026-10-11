package com.gukos.battery_graph.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gukos.battery_graph.BatteryGraphApplication
import com.gukos.battery_graph.MainActivity
import com.gukos.battery_graph.R
import com.gukos.battery_graph.util.BatteryReceiver
import com.gukos.battery_graph.util.BatterySnapshot
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 高精度記録用の常駐サービス。
 * ACTION_BATTERY_CHANGED は Manifest で受信できないため、プロセスを生かしたまま動的登録で受け取り、
 * 残量か充電状態が変わるたびに記録する。通知には現在の残量を表示する。
 */
class BatteryMonitorService : Service() {

    // ストレージが満杯などで DB への書き込みが失敗しても、サービス（プロセス）を落とさない。
    // 落ちると START_STICKY で再起動し、また失敗するのを繰り返してしまう
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, e -> Log.w(TAG, "record failed", e) }
    )
    private var receiver: BatteryReceiver? = null
    private var lastNotified: Pair<Int, Boolean>? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as BatteryGraphApplication
        if (!app.monitorSettings.highPrecisionEnabled.value) {
            stopSelf()
            return START_NOT_STICKY
        }

        // startForegroundService から一定時間内に呼ぶ必要があるため、最初に通知を出す
        val initial = app.batteryStatus.getSnapshot()
        if (!startInForeground(initial)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (receiver == null) {
            val newReceiver = BatteryReceiver(::onBatteryChanged)
            // sticky broadcast なので登録直後に現在値が届き、そのまま記録される
            ContextCompat.registerReceiver(
                this,
                newReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiver = newReceiver
        }
        return START_STICKY
    }

    override fun onDestroy() {
        receiver?.let { unregisterReceiver(it) }
        receiver = null
        scope.cancel()
        super.onDestroy()
    }

    private fun onBatteryChanged(snapshot: BatterySnapshot) {
        val repository = (application as BatteryGraphApplication).repository
        scope.launch { repository.recordIfChanged(snapshot) }

        // 電圧・温度だけの変化では通知を更新しない
        if (lastNotified != snapshot.level to snapshot.isCharging) {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(snapshot))
        }
    }

    private fun startInForeground(snapshot: BatterySnapshot?): Boolean {
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(snapshot), type)
            true
        } catch (e: Exception) {
            // Android 12 以降、バックグラウンドからの起動は許可されない場合がある
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                Log.w(TAG, "foreground service start not allowed", e)
                false
            } else {
                throw e
            }
        }
    }

    private fun buildNotification(snapshot: BatterySnapshot?): android.app.Notification {
        lastNotified = snapshot?.let { it.level to it.isCharging }

        val title = when {
            snapshot == null -> getString(R.string.monitor_notification_title_unknown)
            snapshot.isCharging -> getString(R.string.monitor_notification_title_charging, snapshot.level)
            else -> getString(R.string.monitor_notification_title, snapshot.level)
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_battery)
            .setContentTitle(title)
            .setContentText(getString(R.string.monitor_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.monitor_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "BatteryMonitorService"
        private const val CHANNEL_ID = "battery_monitor"
        private const val NOTIFICATION_ID = 1

        /** 設定が ON のときだけ起動する。フォアグラウンドか起動完了時など、起動が許可される場面で呼ぶ */
        fun startIfEnabled(context: Context) {
            val app = context.applicationContext as BatteryGraphApplication
            if (!app.monitorSettings.highPrecisionEnabled.value) return
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, BatteryMonitorService::class.java)
                )
            } catch (e: IllegalStateException) {
                // バックグラウンドからの起動制限（Android 8 以降）。次に画面を開いたときに再試行される
                Log.w(TAG, "could not start service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryMonitorService::class.java))
        }
    }
}
