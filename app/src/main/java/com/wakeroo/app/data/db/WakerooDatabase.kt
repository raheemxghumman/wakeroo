package com.wakeroo.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms")
    suspend fun all(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun byId(id: Long): AlarmEntity?

    @Upsert
    suspend fun upsert(alarm: AlarmEntity): Long

    @Delete
    suspend fun delete(alarm: AlarmEntity)
}

@Dao
interface RingEventDao {
    @Query("SELECT * FROM ring_events ORDER BY firedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<RingEventEntity>>

    @Insert
    suspend fun insert(event: RingEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<RingEventEntity>)
}

@Database(entities = [AlarmEntity::class, RingEventEntity::class], version = 1, exportSchema = true)
abstract class WakerooDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
    abstract fun ringEvents(): RingEventDao

    companion object {
        fun create(context: Context): WakerooDatabase =
            Room.databaseBuilder(context, WakerooDatabase::class.java, "wakeroo.db").build()
    }
}
