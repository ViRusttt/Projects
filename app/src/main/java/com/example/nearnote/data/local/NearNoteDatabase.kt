package com.example.nearnote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
abstract class NearNoteDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: NearNoteDatabase? = null

        fun getInstance(context: Context): NearNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    NearNoteDatabase::class.java,
                    "nearnote_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
