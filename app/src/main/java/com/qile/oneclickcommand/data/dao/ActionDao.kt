package com.qile.oneclickcommand.data.dao

import androidx.room.*
import com.qile.oneclickcommand.data.entity.ActionEntity

@Dao
interface ActionDao {
    @Query("SELECT * FROM actions WHERE commandId = :commandId ORDER BY orderIndex ASC")
    suspend fun getActionsByCommandId(commandId: Long): List<ActionEntity>

    @Insert
    suspend fun insertAction(action: ActionEntity): Long

    @Insert
    suspend fun insertActions(actions: List<ActionEntity>)

    @Update
    suspend fun updateAction(action: ActionEntity)

    @Delete
    suspend fun deleteAction(action: ActionEntity)

    @Query("DELETE FROM actions WHERE commandId = :commandId")
    suspend fun deleteActionsByCommandId(commandId: Long)
}
