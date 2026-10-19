package com.wakeroo.app.data

import android.content.Context
import androidx.core.os.UserManagerCompat
import com.wakeroo.app.alarm.AlarmScheduler
import com.wakeroo.app.data.db.RingEventEntity
import com.wakeroo.app.data.db.WakerooDatabase
import com.wakeroo.app.data.db.toEntity
import com.wakeroo.app.data.db.toModel
import com.wakeroo.app.data.model.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Single entry point for alarm changes. Every write ends in [sync], which mirrors alarms to
 * direct-boot storage and reschedules them, so the system state can never drift from the data.
 */
class AlarmRepository(
    private val context: Context,
    private val db: Lazy<WakerooDatabase>,
    private val store: DirectBootStore,
    private val scheduler: AlarmScheduler,
) {
    private val lock = Mutex()

    val alarms: Flow<List<Alarm>> get() = db.value.alarms().observeAll().map { list -> list.map { it.toModel() } }

    val ringEvents: Flow<List<RingEventEntity>> get() = db.value.ringEvents().observeRecent()

    suspend fun get(id: Long): Alarm? = db.value.alarms().byId(id)?.toModel()

    suspend fun save(alarm: Alarm): Long = lock.withLock {
        val id = db.value.alarms().upsert(alarm.toEntity()).let { if (it == -1L) alarm.id else it }
        syncLocked()
        id
    }

    suspend fun delete(alarm: Alarm) = lock.withLock {
        db.value.alarms().delete(alarm.toEntity())
        scheduler.cancelAll(alarm.id)
        syncLocked()
    }

    suspend fun setEnabled(alarm: Alarm, enabled: Boolean) = save(alarm.copy(enabled = enabled, skipDate = null))

    /** Skips the next occurrence of a repeating alarm, or turns a one-time alarm off. */
    suspend fun skipNext(alarm: Alarm, nextDate: LocalDate) =
        if (alarm.isRepeating) save(alarm.copy(skipDate = nextDate)) else save(alarm.copy(enabled = false))

    suspend fun sync() = lock.withLock { syncLocked() }

    /** Called when a ring ends. One-time alarms switch themselves off; repeating ones reschedule. */
    suspend fun onRingFinished(alarmId: Long) = lock.withLock {
        if (!isUnlocked()) {
            val alarm = store.alarm(alarmId) ?: return@withLock
            if (!alarm.isRepeating) {
                store.pendingDisables = store.pendingDisables + alarmId
                scheduler.rescheduleAll(store.readAlarms().map { if (it.id == alarmId) it.copy(enabled = false) else it })
            } else {
                scheduler.rescheduleAll(store.readAlarms())
            }
            return@withLock
        }
        val alarm = db.value.alarms().byId(alarmId)?.toModel() ?: return@withLock
        if (!alarm.isRepeating) db.value.alarms().upsert(alarm.copy(enabled = false).toEntity())
        syncLocked()
    }

    suspend fun log(event: PendingEvent) {
        if (isUnlocked()) db.value.ringEvents().insert(event.toEntity()) else store.addPendingEvent(event)
    }

    /** After the first unlock: apply work that happened while storage was locked. */
    suspend fun reconcileAfterUnlock() = lock.withLock {
        val events = store.pendingEvents()
        if (events.isNotEmpty()) {
            db.value.ringEvents().insertAll(events.map { it.toEntity() })
            store.clearPendingEvents()
        }
        val disables = store.pendingDisables
        if (disables.isNotEmpty()) {
            disables.forEach { id -> db.value.alarms().byId(id)?.let { db.value.alarms().upsert(it.copy(enabled = false)) } }
            store.pendingDisables = emptySet()
        }
        syncLocked()
    }

    private suspend fun syncLocked() {
        scheduler.rescheduleAll(db.value.alarms().all().map { it.toModel() })
    }

    private fun isUnlocked() = UserManagerCompat.isUserUnlocked(context)
}

private fun PendingEvent.toEntity() = RingEventEntity(
    alarmId = alarmId, label = label, scheduledAt = scheduledAt, firedAt = firedAt, endedAt = endedAt,
    outcome = outcome, snoozes = snoozes, secondsToWake = secondsToWake, wakeCheckPassed = wakeCheckPassed,
    isTest = isTest,
)
