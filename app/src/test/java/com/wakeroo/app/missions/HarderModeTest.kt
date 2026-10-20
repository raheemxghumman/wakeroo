package com.wakeroo.app.missions

import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HarderModeTest {
    private val protected = Alarm(hour = 6, minute = 30, hardMode = true, missions = listOf(MissionConfig(MissionType.Math), MissionConfig(MissionType.Scan)))

    @Test fun ordinaryAlarmsAreNeverGated() {
        assertFalse(HarderMode.weakens(protected.copy(hardMode = false), protected.copy(hardMode = false, enabled = false)))
    }

    @Test fun switchingOffOrDroppingProtectionIsGated() {
        assertTrue(HarderMode.weakens(protected, protected.copy(enabled = false)))
        assertTrue(HarderMode.weakens(protected, protected.copy(hardMode = false)))
        assertTrue(HarderMode.weakens(protected, protected.copy(missions = protected.missions.take(1))))
    }

    @Test fun smallMovesAndEarlierAreFine() {
        assertFalse(HarderMode.weakens(protected, protected.copy(hour = 7, minute = 30)))
        assertFalse(HarderMode.weakens(protected, protected.copy(hour = 8, minute = 30)))
        assertFalse(HarderMode.weakens(protected, protected.copy(hour = 4, minute = 0)))
        assertFalse(HarderMode.weakens(protected, protected.copy(label = "Gym")))
    }

    @Test fun movingMuchLaterIsGated() {
        assertTrue(HarderMode.weakens(protected, protected.copy(hour = 8, minute = 31)))
        assertTrue(HarderMode.weakens(protected, protected.copy(hour = 11, minute = 0)))
    }
}
