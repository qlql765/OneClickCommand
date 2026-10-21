package com.qile.oneclickcommand.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commands")
data class CommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconColor: Int = 0xFF6200EE.toInt(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
