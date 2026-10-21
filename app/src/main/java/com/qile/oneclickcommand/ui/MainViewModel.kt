package com.qile.oneclickcommand.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qile.oneclickcommand.data.AppDatabase
import com.qile.oneclickcommand.data.entity.ActionEntity
import com.qile.oneclickcommand.data.entity.CommandEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 指令列表 ViewModel，提供指令的 CRUD 及执行能力。
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getDatabase(app)

    val commands: Flow<List<CommandEntity>> = db.commandDao().getAllCommandsFlow()

    suspend fun getActions(commandId: Long): List<ActionEntity> {
        return db.actionDao().getActionsByCommandId(commandId).sortedBy { it.orderIndex }
    }

    fun deleteCommand(command: CommandEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            db.commandDao().deleteCommandById(command.id)
            onDone()
        }
    }
}
