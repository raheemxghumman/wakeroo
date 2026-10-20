package com.wakeroo.app.tile

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.wakeroo.app.R
import com.wakeroo.app.container
import com.wakeroo.app.data.model.Alarm
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * "Power nap" in Quick Settings: one tap sets a 20-minute alarm with your usual mission;
 * tap again to cancel it. The tile shows the minutes left. It goes through the same save
 * path as any alarm, so it's exact, survives reboots and works before unlock.
 */
class PowerNapTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { toggle() } else toggle()
    }

    private fun toggle() {
        val app = container
        app.appScope.launch {
            val nap = currentNap(this@PowerNapTileService)
            if (nap != null) {
                app.repository.get(nap.first)?.let { app.repository.delete(it) }
                clear(this@PowerNapTileService)
            } else {
                val at = ZonedDateTime.now().plusMinutes(NAP_MINUTES.toLong())
                val usual = app.repository.latest()
                val base = usual ?: Alarm(hour = 0, minute = 0)
                val id = app.repository.save(
                    base.copy(
                        id = 0, hour = at.hour, minute = at.minute, days = emptySet(), date = at.toLocalDate(),
                        label = "Power nap", enabled = true, skipDate = null, hardMode = false,
                        wakeCheckMinutes = 0, createdAt = System.currentTimeMillis(),
                    ),
                )
                prefs(this@PowerNapTileService).edit().putLong(KEY_ID, id).putLong(KEY_AT, at.toInstant().toEpochMilli()).apply()
            }
            // The panel may have stopped listening while we saved; ask for a redraw either way.
            refresh()
            requestRefresh(this@PowerNapTileService)
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val nap = currentNap(this)
        tile.state = if (nap != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_wakeroo)
        tile.label = "Power nap"
        val left = nap?.let { ((it.second - System.currentTimeMillis()) / 60_000).coerceAtLeast(0) + 1 }
        val subtitle = if (left != null) "$left min left" else "$NAP_MINUTES min"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        tile.contentDescription = "Power nap, $subtitle"
        tile.updateTile()
    }

    companion object {
        const val NAP_MINUTES = 20
        private const val KEY_ID = "nap_id"
        private const val KEY_AT = "nap_at"

        private fun prefs(context: Context) =
            context.createDeviceProtectedStorageContext().getSharedPreferences("power_nap", Context.MODE_PRIVATE)

        /** The nap alarm still to come, as (alarm id, ring time). */
        fun currentNap(context: Context): Pair<Long, Long>? {
            val p = prefs(context)
            val at = p.getLong(KEY_AT, 0L)
            return if (at > System.currentTimeMillis()) p.getLong(KEY_ID, 0L) to at else null
        }

        private fun clear(context: Context) = prefs(context).edit().clear().apply()

        /** Asks Quick Settings to redraw the tile, e.g. after the nap rings. */
        fun requestRefresh(context: Context) =
            runCatching { requestListeningState(context, ComponentName(context, PowerNapTileService::class.java)) }
    }
}
