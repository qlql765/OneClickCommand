package com.qile.oneclickcommand.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import com.qile.oneclickcommand.data.entity.CommandEntity
import com.qile.oneclickcommand.ui.ExecuteActivity

/**
 * 桌面快捷方式辅助类：将指令以 Shortcut 形式固定到桌面。
 * 点击快捷方式会启动 ExecuteActivity（透明、不残留最近任务）。
 */
object ShortcutHelper {

    private fun shortcutId(commandId: Long) = "command_$commandId"

    fun createShortcut(context: Context, command: CommandEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as? ShortcutManager
            if (shortcutManager == null) {
                Toast.makeText(context, "无法获取快捷方式服务", Toast.LENGTH_SHORT).show()
                return
            }
            if (!shortcutManager.isRequestPinShortcutSupported) {
                Toast.makeText(context, "当前桌面不支持固定快捷方式", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(context, ExecuteActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("executeCommandId", command.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val icon = generateShortcutIcon(context, command.iconColor)

            val shortcutInfo = ShortcutInfo.Builder(context, shortcutId(command.id))
                .setShortLabel(command.name)
                .setLongLabel("一键指令：${command.name}")
                .setIcon(Icon.createWithBitmap(icon))
                .setIntent(intent)
                .build()

            val callbackIntent = shortcutManager.createShortcutResultIntent(shortcutInfo)
            val successCallback = PendingIntent.getBroadcast(
                context, command.id.toInt(), callbackIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            shortcutManager.requestPinShortcut(shortcutInfo, successCallback.intentSender)
            Toast.makeText(context, "请在桌面确认添加快捷方式", Toast.LENGTH_SHORT).show()
        } else {
            createShortcutLegacy(context, command)
        }
    }

    fun removeShortcut(context: Context, commandId: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as? ShortcutManager
            shortcutManager?.removeDynamicShortcuts(listOf(shortcutId(commandId)))
        }
    }

    @Suppress("DEPRECATION")
    private fun createShortcutLegacy(context: Context, command: CommandEntity) {
        val launchIntent = Intent(context, ExecuteActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("executeCommandId", command.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val intent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
            putExtra(Intent.EXTRA_SHORTCUT_NAME, command.name)
            putExtra(Intent.EXTRA_SHORTCUT_ICON, generateShortcutIcon(context, command.iconColor))
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent)
            putExtra("duplicate", false)
        }
        context.sendBroadcast(intent)
        Toast.makeText(context, "快捷方式已发送到桌面", Toast.LENGTH_SHORT).show()
    }

    private fun generateShortcutIcon(context: Context, color: Int): Bitmap {
        val size = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val radius = size / 2f
        val bgPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }
        canvas.drawCircle(radius, radius, radius, bgPaint)

        val textPaint = Paint().apply {
            this.color = Color.WHITE
            isAntiAlias = true
            textSize = size * 0.5f
            textAlign = Paint.Align.CENTER
        }
        val y = radius - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText("⚡", radius, y, textPaint)

        return bitmap
    }
}
