package com.wakeroo.app.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.wakeroo.app.missions.ImageHash
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/**
 * A live back-camera preview. [onFrame] runs on a background thread for every analysed frame;
 * call [ImageProxy.close] when done (or let [FrameHandler] do it).
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    torch: Boolean = false,
    onFrame: (ImageProxy) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val frameHandler by rememberUpdatedState(onFrame)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraRef = remember { arrayOfNulls<Camera>(1) }

    DisposableEffect(lifecycle) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor) { image -> frameHandler(image) } }
            runCatching {
                provider?.unbindAll()
                cameraRef[0] = provider?.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                cameraRef[0]?.cameraControl?.enableTorch(torch)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            executor.shutdown()
        }
    }
    DisposableEffect(torch) {
        cameraRef[0]?.cameraControl?.enableTorch(torch)
        onDispose { }
    }
    AndroidView({ previewView }, modifier)
}

/** On-device barcode reading (bundled model: works offline on the very first ring). */
class BarcodeReader {
    private val scanner = BarcodeScanning.getClient()

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun read(image: ImageProxy, onResult: (String?) -> Unit) {
        val media = image.image ?: return image.close().also { onResult(null) }
        scanner.process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes -> onResult(codes.firstNotNullOfOrNull { it.rawValue }) }
            .addOnFailureListener { onResult(null) }
            .addOnCompleteListener { image.close() }
    }

    fun close() = scanner.close()
}

/** A frame's fingerprint: what ML Kit sees in it, plus a perceptual hash of its layout. */
data class SceneFingerprint(val labels: List<String>, val hash: Long)

class SceneReader {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.6f).build())

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    suspend fun read(image: ImageProxy): SceneFingerprint? {
        val media = image.image ?: return null.also { image.close() }
        val hash = hashOf(image)
        val labels = suspendCancellableCoroutine { cont ->
            labeler.process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
                .addOnSuccessListener { list -> cont.resume(list.map { it.text }) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }
        image.close()
        return SceneFingerprint(labels, hash)
    }

    fun close() = labeler.close()

    companion object {
        /** dHash straight from the luminance plane; no bitmap conversion needed. */
        fun hashOf(image: ImageProxy): Long {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            return ImageHash.dHash({ x, y -> buffer.get(y * rowStride + x * pixelStride).toInt() and 0xFF }, image.width, image.height)
        }

        fun matches(reference: SceneFingerprint, live: SceneFingerprint, sensitivity: Int): Boolean {
            // sensitivity 1 (lenient) .. 3 (strict)
            val maxDistance = when (sensitivity) { 1 -> 22; 2 -> 18; else -> 14 }
            val labelOk = reference.labels.isEmpty() || live.labels.any { it in reference.labels }
            return ImageHash.distance(reference.hash, live.hash) <= maxDistance && labelOk
        }
    }
}

/** Saves a small preview of the reference spot for the alarm editor. */
fun saveThumbnail(context: Context, bitmap: Bitmap, name: String): String {
    val file = java.io.File(context.filesDir, "spots").apply { mkdirs() }.resolve("$name.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
    return file.absolutePath
}

/** On-device object labels with their confidence, for the item-hunt mission. */
class LabelReader {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.35f).build())

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    suspend fun read(image: ImageProxy): List<Pair<String, Float>> {
        val media = image.image ?: return emptyList<Pair<String, Float>>().also { image.close() }
        val labels = suspendCancellableCoroutine { cont ->
            labeler.process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
                .addOnSuccessListener { list -> cont.resume(list.map { it.text to it.confidence }) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }
        image.close()
        return labels
    }

    fun close() = labeler.close()
}
