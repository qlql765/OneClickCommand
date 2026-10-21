package com.qile.oneclickcommand.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import com.qile.oneclickcommand.data.AppDatabase
import com.qile.oneclickcommand.executor.CommandExecuteService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 透明 Activity：点击桌面快捷方式后启动。
 * - 按指令顺序通过 CommandExecuteService 依次打开各 App。
 * - 全部启动完成后，该 Activity 自动 finish 且因 excludeFromRecents 不残留后台。
 * - launchMode=singleTask + taskAffinity="" 确保不与主界面任务栈混淆。
 */
class ExecuteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 透明主题，无需 setContentView
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val commandId = intent.getLongExtra("executeCommandId", 0)
        if (commandId <= 0) {
            finishAndRemoveTask()
            return
        }

        val db = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            val command = db.commandDao().getCommandById(commandId)
            if (command == null) {
                runOnUiThread { finishAndRemoveTask() }
                return@launch
            }
            val actions = db.actionDao().getActionsByCommandId(commandId)

            // 启动前台服务执行
            CommandExecuteService.start(this@ExecuteActivity, commandId, command.name)

            // 计算总耗时，确保所有 App 启动后再关闭自身
            val totalDelay = actions.sumOf { it.delayMs } + 1500L
            Handler(Looper.getMainLooper()).postDelayed({
                finishAndRemoveTask()
            }, totalDelay)
        }
    }
}
