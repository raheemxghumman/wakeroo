package com.wakeroo.app.ui.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.wakeroo.app.ui.theme.LocalReducedMotion

/**
 * How far the phone is tilted from where it was held when the screen opened, in -1..1 on
 * each axis. Drives the parallax and 3D card effects. Zero when motion is reduced.
 */
@Composable
fun rememberDeviceTilt(): State<Offset> {
    val state = remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    val reduced = LocalReducedMotion.current
    DisposableEffect(reduced) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (reduced || sensor == null) {
            state.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        val rotation = FloatArray(9)
        val angles = FloatArray(3)
        var baseline: FloatArray? = null
        var smooth = Offset.Zero
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, angles)
                val base = baseline ?: angles.copyOf().also { baseline = it }
                val pitch = (angles[1] - base[1]).coerceIn(-0.5f, 0.5f) / 0.5f
                val roll = (angles[2] - base[2]).coerceIn(-0.5f, 0.5f) / 0.5f
                smooth = Offset(smooth.x + (roll - smooth.x) * 0.12f, smooth.y + (pitch - smooth.y) * 0.12f)
                state.value = smooth
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return state
}
