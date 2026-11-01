package com.example.campuscomplaint

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class StatusService : Service() {
    companion object {
        const val ACTION_STATUS_CHANGED = "com.example.campuscomplaint.STATUS_CHANGED"
        const val EXTRA_TITLE = "title"
        const val EXTRA_STATUS = "status"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Complaint"
        val status = intent?.getStringExtra(EXTRA_STATUS) ?: ""
        Log.d("StatusService", "$title -> $status")
        showNotification(title, status)
        sendBroadcast(Intent(ACTION_STATUS_CHANGED).setPackage(packageName))
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun showNotification(title: String, status: String) {
        val channelId = "status_updates"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(NotificationChannel(channelId, "Complaint updates", NotificationManager.IMPORTANCE_DEFAULT))
        val pi = PendingIntent.getActivity(this, 0, Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Complaint $status").setContentText(title)
            .setContentIntent(pi).setAutoCancel(true).build()
        val allowed = Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) nm.notify(System.currentTimeMillis().toInt(), n)
    }
}