package com.qile.oneclickcommand.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.qile.oneclickcommand.data.dao.ActionDao
import com.qile.oneclickcommand.data.dao.CommandDao
import com.qile.oneclickcommand.data.entity.ActionEntity
import com.qile.oneclickcommand.data.entity.CommandEntity

@Database(entities = [CommandEntity::class, ActionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao
    abstract fun actionDao(): ActionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "one_click_command_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
