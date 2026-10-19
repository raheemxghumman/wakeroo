package com.wakeroo.app.ui.scenes

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.Outfit
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** An animated alarm background. All drawn in code: no video files, nothing to download. */
enum class Scene(val id: String, val title: String, val blurb: String, val pro: Boolean, val suggestedTone: String?) {
    NightSky("night", "Night Sky", "Stars and a slow sunrise glow", false, "wakeroo_rise"),
    IntoSpace("space", "Into Space", "Sunrise over a turning planet", false, "warm_pads"),
    Minimal("minimal", "Minimal Black", "Pure black. Easy on OLED and eyes", false, "bright_pulse"),
    Aurora("aurora", "Aurora", "Northern lights drifting overhead", true, "dawn_chimes"),
    Ocean("ocean", "Ocean Dawn", "Waves rolling under a peach sky", true, "soft_marimba"),
    Kinetic("kinetic", "WAKE UP", "Loud type that won't let you doze", true, "klaxon"),
    Photo("photo", "Your photo", "Any picture, slowly drifting", true, null);

    companion object {
        fun byId(id: String?): Scene = entries.firstOrNull { it.id == id } ?: NightSky
    }
}

/** Where the custom scene photo lives: device-protected, so it shows even before the first unlock. */
fun customScenePhoto(context: Context): File =
    File(context.createDeviceProtectedStorageContext().filesDir, "scene").apply { mkdirs() }.resolve("custom.jpg")

/** Seconds since the scene appeared, advanced every frame; read it in draw blocks only. */
@Composable
fun rememberSceneClock(playing: Boolean = true): MutableFloatState {
    val time = remember { mutableFloatStateOf(0f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(playing, reduced) {
        if (!playing || reduced) return@LaunchedEffect
        val start = withFrameNanos { it } - (time.floatValue * 1e9).toLong()
        while (true) withFrameNanos { time.floatValue = (it - start) / 1e9f }
    }
    return time
}

/**
 * Draws [scene] full-bleed behind [content]. [intensity] (0..1) is how hard the alarm is
 * pushing: scenes pulse brighter and faster as it rises. [tilt] adds parallax depth.
 */
@Composable
fun SceneBackground(
    scene: Scene,
    modifier: Modifier = Modifier,
    tilt: Offset = Offset.Zero,
    intensity: Float = 0.5f,
    playing: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val clock = rememberSceneClock(playing)
    Box(modifier.fillMaxSize()) {
        when (scene) {
            Scene.NightSky -> NightSkyScene(clock, tilt, intensity)
            Scene.IntoSpace -> SpaceScene(clock, tilt)
            Scene.Minimal -> MinimalScene(clock, intensity)
            Scene.Aurora -> AuroraScene(clock, tilt)
            Scene.Ocean -> OceanScene(clock, tilt)
            Scene.Kinetic -> KineticScene(clock, intensity)
            Scene.Photo -> PhotoScene(clock)
        }
        content()
    }
}

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val depth: Float)

private fun stars(count: Int, seed: Int, maxY: Float = 1f) = Random(seed).let { rnd ->
    List(count) { Star(rnd.nextFloat(), rnd.nextFloat() * maxY, 0.5f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 6.28f, 0.3f + rnd.nextFloat()) }
}

private fun DrawScope.drawStars(list: List<Star>, t: Float, tilt: Offset, alpha: Float = 1f) {
    for (s in list) {
        val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(t * (0.8f + s.depth) + s.phase))
        drawCircle(
            Color.White.copy(alpha = alpha * tw * 0.85f), s.r * density * 0.9f,
            Offset(s.x * size.width + tilt.x * 26f * s.depth, s.y * size.height + tilt.y * 26f * s.depth),
        )
    }
}

@Composable
private fun NightSkyScene(clock: MutableFloatState, tilt: Offset, intensity: Float) {
    val list = remember { stars(80, 7, 0.8f) }
    Canvas(Modifier.fillMaxSize()) {
        val t = clock.floatValue
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0E1120), Color(0xFF1A1E33), Color(0xFF2B2550))))
        drawStars(list, t, tilt)
        val breathe = 0.75f + 0.25f * sin(t * (0.8f + intensity * 1.6f))
        val c = Offset(size.width * 0.5f - tilt.x * 18f, size.height * 0.36f - tilt.y * 18f)
        val r = size.minDimension * (0.58f + 0.05f * breathe)
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFFF7A59).copy(alpha = 0.5f * breathe), Color(0xFFFF7A59).copy(alpha = 0.1f), Color.Transparent), c, r),
            r, c,
        )
    }
}

@Composable
private fun SpaceScene(clock: MutableFloatState, tilt: Offset) {
    val list = remember { stars(110, 11) }
    Canvas(Modifier.fillMaxSize()) {
        val t = clock.floatValue
        drawRect(Color(0xFF05060F))
        // Stars drift slowly sideways: the camera orbits.
        for (s in list) {
            val x = ((s.x + t * 0.004f * s.depth) % 1f) * size.width + tilt.x * 30f * s.depth
            val tw = 0.5f + 0.5f * sin(t * s.depth + s.phase)
            drawCircle(Color.White.copy(alpha = 0.3f + 0.6f * tw), s.r * density * 0.8f, Offset(x, s.y * size.height * 0.75f + tilt.y * 30f * s.depth))
        }
        val planetR = size.height * 0.62f
        val pc = Offset(size.width * 0.5f + tilt.x * 10f, size.height * 0.68f + planetR + tilt.y * 10f)
        val rimY = pc.y - planetR
        // The sun just over the planet's edge, its glow wrapping the atmosphere.
        val rise = 0.5f + 0.5f * sin(t * 0.15f)
        val sun = Offset(size.width * 0.5f, rimY - size.height * (0.01f + 0.035f * rise))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE2A8), Color(0xFFFFC857).copy(alpha = 0.5f), Color.Transparent), sun, size.width * 0.75f), size.width * 0.75f, sun)
        drawCircle(Color(0xFFFFF4DC), size.width * 0.035f, sun)
        drawCircle(
            Brush.radialGradient(listOf(Color.Transparent, Color(0xFFFF7A59).copy(alpha = 0.55f), Color(0xFFB9A7FF).copy(alpha = 0.25f), Color.Transparent), pc, planetR * 1.08f),
            planetR * 1.08f, pc,
        )
        drawCircle(Brush.verticalGradient(listOf(Color(0xFF2B2550), Color(0xFF0E1120)), rimY, rimY + planetR * 0.6f), planetR, pc)
        // Cloud bands sliding across the surface sell the rotation.
        val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(pc, planetR)) }
        clipPath(clip) {
            for (k in 0 until 6) {
                val y = rimY + planetR * (0.05f + k * 0.06f)
                val drift = ((t * 0.02f * (1 + k * 0.3f) + k * 0.17f) % 1f)
                val w = size.width * (0.5f + 0.3f * (k % 2))
                val x = -w + drift * (size.width + w * 2)
                drawRoundRect(
                    Color(0xFFB9A7FF).copy(alpha = 0.06f + 0.03f * (k % 3)), Offset(x, y), Size(w, planetR * 0.02f + k * 2f),
                    androidx.compose.ui.geometry.CornerRadius(20f),
                )
            }
        }
        drawCircle(Color(0xFFFFC857).copy(alpha = 0.7f), planetR, pc, style = Stroke(2f * density))
    }
}

@Composable
private fun MinimalScene(clock: MutableFloatState, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val t = clock.floatValue
        drawRect(Color.Black)
        val c = Offset(size.width / 2, size.height * 0.38f)
        val r = size.minDimension * 0.42f
        drawCircle(Color.White.copy(alpha = 0.06f), r, c, style = Stroke(1.5f * density))
        val sweep = 60f + 40f * sin(t * 0.7f)
        val start = (t * (40f + intensity * 80f)) % 360f
        drawArc(
            Color(0xFFFF7A59), start, sweep, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2),
            style = Stroke(3f * density, cap = StrokeCap.Round),
        )
        val a = Math.toRadians((start + sweep).toDouble())
        drawCircle(Color(0xFFFFC857), 5f * density, Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()))
    }
}

private const val AURORA_SHADER = """
uniform float2 res;
uniform float t;
uniform float2 tilt;

float band(float2 uv, float y, float speed, float seed) {
    float wave = 0.07 * sin(uv.x * 2.6 + t * speed + seed) + 0.035 * sin(uv.x * 6.3 - t * speed * 1.7 + seed * 2.0);
    float d = uv.y - (y + wave);
    // A bright lower edge with light rays climbing upward from it.
    float edge = exp(-d * d * 900.0);
    float rays = (d < 0.0) ? exp(d * 5.5) * (0.55 + 0.45 * sin(uv.x * 38.0 + 2.5 * sin(uv.x * 9.0 + t * 0.35 + seed) + t * 0.9 + seed * 5.0)) : 0.0;
    return edge * 0.9 + rays * 0.55;
}

half4 main(float2 fc) {
    float2 uv = fc / res + tilt * 0.02;
    float3 sky = mix(float3(0.01, 0.02, 0.06), float3(0.06, 0.05, 0.16), uv.y);
    float3 col = sky;
    col += float3(0.20, 0.95, 0.62) * band(uv, 0.34, 0.35, 0.0);
    col += float3(0.48, 0.42, 1.00) * band(uv, 0.24, 0.27, 1.9) * 0.85;
    col += float3(1.00, 0.42, 0.55) * band(uv, 0.44, 0.22, 3.7) * 0.45;
    float s = fract(sin(dot(floor(fc / 3.0), float2(12.9898, 78.233))) * 43758.5453);
    if (s > 0.9985) col += float3(0.8) * (0.5 + 0.5 * sin(t * 2.0 + s * 50.0));
    return half4(col, 1.0);
}
"""

@Composable
private fun AuroraScene(clock: MutableFloatState, tilt: Offset) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AuroraShader(clock, tilt) else AuroraFallback(clock, tilt)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AuroraShader(clock: MutableFloatState, tilt: Offset) {
    val shader = remember { RuntimeShader(AURORA_SHADER) }
    val brush = remember { ShaderBrush(shader) }
    Canvas(Modifier.fillMaxSize()) {
        shader.setFloatUniform("res", size.width, size.height)
        shader.setFloatUniform("t", clock.floatValue)
        shader.setFloatUniform("tilt", tilt.x, tilt.y)
        drawRect(brush)
    }
}

/** Below Android 13: soft ribbons of gradient that sway instead of the shader. */
@Composable
private fun AuroraFallback(clock: MutableFloatState, tilt: Offset) {
    val list = remember { stars(60, 5, 0.7f) }
    Canvas(Modifier.fillMaxSize()) {
        val t = clock.floatValue
        drawRect(Brush.verticalGradient(listOf(Color(0xFF03050F), Color(0xFF0F0C29))))
        drawStars(list, t, tilt, 0.6f)
        val colors = listOf(Color(0xFF33F29E), Color(0xFF7A6BFF), Color(0xFFFF6B8C))
        colors.forEachIndexed { i, c ->
            val path = Path()
            val baseY = size.height * (0.34f - i * 0.08f)
            path.moveTo(0f, baseY)
            var x = 0f
            while (x <= size.width) {
                val y = baseY + sin(x / size.width * 5f + t * (0.4f + i * 0.1f) + i) * size.height * 0.05f
                path.lineTo(x, y); x += 12f
            }
            path.lineTo(size.width, baseY - size.height * 0.3f)
            path.lineTo(0f, baseY - size.height * 0.3f)
            path.close()
            drawPath(path, Brush.verticalGradient(listOf(Color.Transparent, c.copy(alpha = 0.35f)), baseY - size.height * 0.3f, baseY + size.height * 0.05f))
        }
    }
}

@Composable
private fun OceanScene(clock: MutableFloatState, tilt: Offset) {
    Canvas(Modifier.fillMaxSize()) {
        val t = clock.floatValue
        val horizon = size.height * 0.52f
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2B2550), Color(0xFF6B4A78), Color(0xFFFF9466), Color(0xFFFFC857)), 0f, horizon))
        val sun = Offset(size.width * 0.5f - tilt.x * 12f, horizon - size.width * 0.06f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1C9), Color(0xFFFFC857).copy(alpha = 0.4f), Color.Transparent), sun, size.width * 0.5f), size.width * 0.5f, sun)
        drawCircle(Color(0xFFFFF4DC), size.width * 0.12f, sun)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF3B3D7A), Color(0xFF0E1120)), horizon, size.height), Offset(0f, horizon), Size(size.width, size.height - horizon))
        // The sun's path on the water, glittering.
        for (k in 0 until 24) {
            val y = horizon + (size.height - horizon) * (k / 24f) * 0.8f
            val w = size.width * (0.06f + 0.4f * k / 24f) * (0.6f + 0.4f * sin(t * 3f + k))
            drawRect(Color(0xFFFFE2A8).copy(alpha = 0.35f * (1 - k / 24f)), Offset(sun.x - w / 2, y), Size(w, 2.5f * density))
        }
        // Four wave layers, nearer ones bigger, faster and more affected by tilt.
        for (layer in 0 until 4) {
            val depth = layer / 3f
            val baseY = horizon + (size.height - horizon) * (0.18f + depth * 0.62f)
            val amp = size.height * (0.006f + depth * 0.02f)
            val len = size.width * (0.35f + depth * 0.5f)
            val speed = 0.4f + depth * 0.9f
            val path = Path().apply {
                moveTo(0f, size.height)
                var x = 0f
                while (x <= size.width + 10) {
                    lineTo(x, baseY + sin((x + tilt.x * 40f * depth) / len * 2 * PI.toFloat() + t * speed) * amp + tilt.y * 20f * depth)
                    x += 10f
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, Brush.verticalGradient(listOf(Color(0xFF4A4A8F).copy(alpha = 0.5f + depth * 0.3f), Color(0xFF0E1120)), baseY - amp, size.height))
        }
    }
}

private val KINETIC_WORDS = listOf("WAKE UP", "RISE", "GET UP", "GO", "TODAY")

/** Bold type slams in on the beat, shakes and fades, word after word, in the background. */
@Composable
private fun KineticScene(clock: MutableFloatState, intensity: Float) {
    val beat = 0.9f - intensity * 0.35f
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.floatValue
            val k = (t / beat)
            val p = k - k.toInt()
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1B0F24), Color(0xFF0E1120))))
            // A flash and a coral stripe on every beat.
            drawRect(Color(0xFFFF7A59).copy(alpha = max(0f, 0.25f - p * 0.6f)))
            val stripeY = size.height * ((k.toInt() * 0.37f) % 1f)
            drawRect(Color(0xFFFFC857).copy(alpha = 0.18f * (1 - p)), Offset(0f, stripeY), Size(size.width, size.height * 0.08f))
        }
        for (row in 0 until 4) KineticWord(clock, row, beat)
    }
}

@Composable
private fun BoxScope.KineticWord(clock: MutableFloatState, row: Int, beat: Float) {
    val y = listOf(-0.62f, -0.2f, 0.25f, 0.68f)[row]
    // One word per row, swapping every few beats; the word itself never recomposes per frame.
    Text(
        KINETIC_WORDS[row % KINETIC_WORDS.size],
        style = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 96.sp, letterSpacing = (-0.04).em, textAlign = TextAlign.Center),
        color = Color.White,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .align(androidx.compose.ui.BiasAlignment(0f, y))
            .graphicsLayer {
                val t = clock.floatValue
                val k = t / beat + row * 0.5f
                val p = k - k.toInt()
                val slam = 1f + 0.35f * max(0f, 1f - p * 5f)
                scaleX = slam; scaleY = slam
                translationX = sin(k * 13f) * 6f * max(0f, 1f - p * 4f) + sin(t * 0.35f + row * 1.3f) * size.width * 0.12f
                rotationZ = (if (row % 2 == 0) -4f else 3f)
                alpha = 0.08f + 0.16f * max(0f, 1f - p * 2f)
            },
    )
}

@Composable
private fun PhotoScene(clock: MutableFloatState) {
    val context = LocalContext.current
    val image: ImageBitmap? = remember {
        runCatching {
            val file = customScenePhoto(context)
            if (!file.exists()) null else {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (image == null) {
        NightSkyScene(clock, Offset.Zero, 0.5f)
        return
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = clock.floatValue
        // Ken Burns: a slow zoom and drift that loops every 24 seconds.
        val cycle = (t % 24f) / 24f
        val ease = 0.5f - 0.5f * cos(cycle * 2 * PI.toFloat())
        val zoom = 1.04f + 0.1f * ease
        val scale = max(size.width / image.width, size.height / image.height) * zoom
        val w = image.width * scale
        val h = image.height * scale
        val dx = (size.width - w) / 2 + (ease - 0.5f) * size.width * 0.04f
        val dy = (size.height - h) / 2
        clipRect {
            drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(image.width, image.height), dstOffset = IntOffset(dx.toInt(), dy.toInt()), dstSize = IntSize(w.toInt(), h.toInt()))
        }
        // A dark scrim so the clock and buttons stay readable on any photo.
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.7f))))
    }
}
