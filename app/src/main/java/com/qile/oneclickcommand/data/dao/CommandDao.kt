package com.qile.oneclickcommand.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.qile.oneclickcommand.data.entity.CommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {
    @Query("SELECT * FROM commands ORDER BY createdAt DESC")
    fun getAllCommands(): LiveData<List<CommandEntity>>

    @Query("SELECT * FROM commands ORDER BY createdAt DESC")
    fun getAllCommandsFlow(): Flow<List<CommandEntity>>

    @Query("SELECT * FROM commands ORDER BY createdAt DESC")
    suspend fun getAllCommandsOnce(): List<CommandEntity>

    @Query("SELECT * FROM commands WHERE id = :id")
    suspend fun getCommandById(id: Long): CommandEntity?

    @Insert
    suspend fun insertCommand(command: CommandEntity): Long

    @Update
    suspend fun updateCommand(command: CommandEntity)

    @Delete
    suspend fun deleteCommand(command: CommandEntity)

    @Query("DELETE FROM commands WHERE id = :id")
    suspend fun deleteCommandById(id: Long)
}
