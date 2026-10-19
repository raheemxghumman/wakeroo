package com.wakeroo.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms")
    suspend fun all(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun byId(id: Long): AlarmEntity?

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(alarm: AlarmEntity): Long

    @Delete
    suspend fun delete(alarm: AlarmEntity)
}

@Dao
interface RingEventDao {
    @Query("SELECT * FROM ring_events ORDER BY firedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<RingEventEntity>>

    @Query("SELECT * FROM ring_events ORDER BY firedAt DESC")
    fun observeAll(): Flow<List<RingEventEntity>>

    @Insert
    suspend fun insert(event: RingEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<RingEventEntity>)
}

@Dao
interface MorningDao {
    @Query("SELECT * FROM mood_log ORDER BY date DESC")
    fun observeMoods(): Flow<List<MoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMood(mood: MoodEntity)

    @Query("SELECT * FROM routine_item ORDER BY position, id")
    fun observeRoutine(): Flow<List<RoutineItemEntity>>

    @Query("SELECT COUNT(*) FROM routine_item")
    suspend fun routineCount(): Int

    @Upsert
    suspend fun upsertRoutine(item: RoutineItemEntity): Long

    @Delete
    suspend fun deleteRoutine(item: RoutineItemEntity)

    @Query("DELETE FROM routine_log WHERE itemId = :itemId")
    suspend fun clearRoutineLog(itemId: Long)

    @Query("SELECT * FROM routine_log")
    fun observeRoutineLog(): Flow<List<RoutineLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun tick(log: RoutineLogEntity)

    @Query("DELETE FROM routine_log WHERE date = :date AND itemId = :itemId")
    suspend fun untick(date: String, itemId: Long)

    @Query("SELECT * FROM saved_quote ORDER BY savedAt DESC")
    fun observeSavedQuotes(): Flow<List<SavedQuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQuote(quote: SavedQuoteEntity)

    @Query("DELETE FROM saved_quote WHERE quoteId = :id")
    suspend fun unsaveQuote(id: String)
}

@Database(
    entities = [
        AlarmEntity::class, RingEventEntity::class, MoodEntity::class, RoutineItemEntity::class,
        RoutineLogEntity::class, SavedQuoteEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class WakerooDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
    abstract fun ringEvents(): RingEventDao
    abstract fun morning(): MorningDao

    companion object {
        fun create(context: Context): WakerooDatabase =
            Room.databaseBuilder(context, WakerooDatabase::class.java, "wakeroo.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** v2: sound mode, bundled tones, Harder Mode and scenes on alarms; the Morning tab tables. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN tone TEXT")
                db.execSQL("ALTER TABLE alarms ADD COLUMN soundMode TEXT NOT NULL DEFAULT 'Single'")
                db.execSQL("ALTER TABLE alarms ADD COLUMN hardMode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN scene TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS mood_log (date TEXT NOT NULL, mood INTEGER NOT NULL, loggedAt INTEGER NOT NULL, PRIMARY KEY(date))")
                db.execSQL("CREATE TABLE IF NOT EXISTS routine_item (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS routine_log (date TEXT NOT NULL, itemId INTEGER NOT NULL, doneAt INTEGER NOT NULL, PRIMARY KEY(date, itemId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS saved_quote (quoteId TEXT NOT NULL, savedAt INTEGER NOT NULL, PRIMARY KEY(quoteId))")
            }
        }
    }
}
