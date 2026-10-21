package com.qile.oneclickcommand.executor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.qile.oneclickcommand.R
import com.qile.oneclickcommand.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 前台服务：按序启动指定指令中的所有 App，完成后自动停止。
 * 使用前台服务确保在 Android 16 后台启动限制下仍能可靠执行。
 */
class CommandExecuteService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val channelId = "command_execution_channel"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val commandId = intent?.getLongExtra("commandId", 0) ?: 0
        val commandName = intent?.getStringExtra("commandName") ?: "一键指令"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("正在执行：$commandName")
            .setContentText("正在依次打开应用…")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setSilent(true)
            .build()
        startForeground(notificationId, notification)

        if (commandId > 0) {
            executeCommand(commandId, commandName)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun executeCommand(commandId: Long, commandName: String) {
        val db = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            val command = db.commandDao().getCommandById(commandId)
            if (command == null) {
                handler.post { stopSelf() }
                return@launch
            }
            val actions = db.actionDao().getActionsByCommandId(commandId).sortedBy { it.orderIndex }

            if (actions.isEmpty()) {
                handler.post { stopSelf() }
                return@launch
            }

            for ((index, action) in actions.withIndex()) {
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(action.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                    }
                    // 若 intent 为 null（应用已卸载），静默跳过
                } catch (e: Exception) {
                    // 单个 App 启动失败，继续下一个
                }

                if (index < actions.size - 1) {
                    try {
                        Thread.sleep(action.delayMs)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }

            // 全部执行完毕，停止服务
            handler.post { stopSelf() }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "指令执行",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "执行一键指令时的通知"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context, commandId: Long, commandName: String) {
            val intent = Intent(context, CommandExecuteService::class.java).apply {
                putExtra("commandId", commandId)
                putExtra("commandName", commandName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
