package com.qile.oneclickcommand.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "actions",
    foreignKeys = [
        ForeignKey(
            entity = CommandEntity::class,
            parentColumns = ["id"],
            childColumns = ["commandId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("commandId")]
)
data class ActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val commandId: Long,
    val orderIndex: Int,
    val packageName: String,
    val appName: String,
    val delayMs: Long = 600
)
