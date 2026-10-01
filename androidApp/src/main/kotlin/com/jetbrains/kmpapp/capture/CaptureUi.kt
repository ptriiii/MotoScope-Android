package com.motoscope.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.StatFs
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.compose.foundation.layout.safeDrawingPadding
import com.motoscope.viewer.ImageSequenceViewerContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import android.util.LruCache
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.lifecycle.Observer

@Composable
internal fun CaptureCameraScreen(
    lifecycleOwner: LifecycleOwner,
    sequence: CaptureSequence,
    slot: Int,
    store: CaptureSessionStore,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSelectSlot: (Int) -> Unit,
    onReview: () -> Unit,
    onClearError: () -> Unit,
    onCaptureError: (String) -> Unit,
    onCaptureSaved: suspend (Int, File, File, PreparedCaptureImage) -> Unit,
) {
    val context = LocalContext.current
    val directory = remember(sequence.id) { store.sessionDirectory(sequence.id) }
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    val selectedPhoto = sequence.photos[slot]
    val previousPhoto = sequence.photos.getOrNull(slot - 1)
    val coroutineScope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var cameraBindError by remember(controller) { mutableStateOf<String?>(null) }
    var bindAttempt by remember(controller) { mutableIntStateOf(0) }
    val inFlight = saving || busy
    BackHandler(enabled = !inFlight, onBack = onBack)

    DisposableEffect(controller, lifecycleOwner, bindAttempt) {
        cameraBindError = null
        val binding = runCatching { controller.bindToLifecycle(lifecycleOwner) }
            .onFailure { failure ->
                Log.e("MotoScopeCapture", "CameraX could not bind the capture camera.", failure)
                cameraBindError = failure.message?.let { "Camera could not start: $it" }
                    ?: "Camera could not start. Check camera access and try again."
            }
        val cameraState = binding.getOrNull()?.let { controller.cameraInfo?.cameraState }
        val stateObserver = cameraState?.let { liveCameraState ->
            Observer<CameraState> { state ->
                if (state.error != null) {
                    cameraBindError = "Camera became unavailable. Check camera access and retry."
                }
            }.also { liveCameraState.observe(lifecycleOwner, it) }
        }
        onDispose {
            if (stateObserver != null) cameraState.removeObserver(stateObserver)
            controller.unbind()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, enabled = !inFlight) { Text("‹  Back") }
            Text("Photo ${slot + 1} of ${sequence.photos.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = onReview, enabled = sequence.isComplete && !inFlight) { Text("Review") }
        }
        Text("${sequence.completedCount} saved", color = Color(0xFF687084), modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PreviewView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        this.controller = controller
                    }
                },
            )
            Box(
                Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.82f)
                    .border(2.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                    .semantics { contentDescription = "Framing guide. Keep the complete motorcycle inside the outline." },
            )
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text("Keep the whole motorcycle inside the guide", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            cameraBindError?.let { message ->
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(message, color = Color.White, textAlign = TextAlign.Center)
                    Button(onClick = { cameraBindError = null; bindAttempt++ }, enabled = !inFlight) {
                        Text("Retry camera")
                    }
                }
            }
        }

        Text(
            "Keep a consistent height and distance. Move a small step in the same direction between photos.",
            color = Color(0xFF687084),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            if (previousPhoto != null) {
                val previousFile = store.fileFor(sequence, previousPhoto, display = true)
                LocalCaptureFrameImage(previousFile.absolutePath, Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Text("1 / ${sequence.photos.size}", fontSize = 10.sp, color = Color(0xFF687084)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onSelectSlot((slot - 1).coerceAtLeast(0)) },
                    enabled = slot > 0 && !inFlight,
                    modifier = Modifier.semantics { contentDescription = "Previous photo" },
                ) { Text("‹") }
                Spacer(Modifier.width(12.dp))
                FilledIconButton(
                    onClick = {
                        if (inFlight) return@FilledIconButton
                        val available = runCatching { StatFs(directory.absolutePath).availableBytes }.getOrDefault(0L)
                        if (available < 24L * 1024L * 1024L) {
                            onCaptureError("Free space is low. Free at least 24 MB before taking another photo.")
                            saving = false
                            return@FilledIconButton
                        }
                        saving = true
                        onClearError()
                        val id = UUID.randomUUID().toString()
                        val originalTemp = File(directory, "photo-${slot + 1}-$id.pending.jpg")
                        val displayTemp = File(directory, "display-${slot + 1}-$id.pending.jpg")
                        val options = ImageCapture.OutputFileOptions.Builder(originalTemp).build()
                        controller.takePicture(
                            options,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                    coroutineScope.launch {
                                        try {
                                            val prepared = withContext(Dispatchers.IO) {
                                                CaptureImageProcessor.prepareDisplayImage(originalTemp, displayTemp)
                                            }
                                            onCaptureSaved(slot, originalTemp, displayTemp, prepared)
                                        } catch (failure: CancellationException) {
                                            originalTemp.delete()
                                            displayTemp.delete()
                                            throw failure
                                        } catch (failure: Exception) {
                                            originalTemp.delete()
                                            displayTemp.delete()
                                            onCaptureError(failure.message ?: "Photo could not be prepared. Try again.")
                                        } finally {
                                            saving = false
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e(
                                        "MotoScopeCapture",
                                        "CameraX still capture failed (error=${exception.imageCaptureError}).",
                                        exception,
                                    )
                                    originalTemp.delete()
                                    displayTemp.delete()
                                    saving = false
                                    onCaptureError(exception.message ?: "Camera capture failed. Try again.")
                                }
                            },
                        )
                    },
                    enabled = !inFlight,
                    modifier = Modifier.size(72.dp).semantics {
                        contentDescription = if (selectedPhoto == null) "Take photo ${slot + 1} of ${sequence.photos.size}" else "Retake photo ${slot + 1} of ${sequence.photos.size}"
                    },
                ) {
                    if (inFlight) CircularProgressIndicator(Modifier.size(28.dp), color = Color.White)
                    else Box(
                        Modifier.size(28.dp).border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(12.dp).background(Color.White, CircleShape))
                    }
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { onSelectSlot((slot + 1).coerceAtMost(sequence.photos.lastIndex)) },
                    enabled = slot < sequence.photos.lastIndex && !inFlight,
                    modifier = Modifier.semantics { contentDescription = "Next photo" },
                ) { Text("›") }
            }
            if (selectedPhoto != null) {
                LocalCaptureFrameImage(
                    store.fileFor(sequence, selectedPhoto, display = true).absolutePath,
                    Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(Modifier.size(52.dp))
            }
        }
        // Keep this slot allocated in every state. Changing the Column's height while a capture is
        // in flight resizes PreviewView; CameraX then rebinds its use cases and can abort the shot.
        val statusText = error ?: if (inFlight) "Saving photo securely on this device…" else null
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            statusText?.let {
                Text(
                    text = it,
                    color = if (error != null) Color(0xFFB3261E) else Color(0xFF687084),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun CaptureContactSheet(
    sequence: CaptureSequence,
    store: CaptureSessionStore,
    onSelectSlot: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CaptureSequence.SLOT_COUNT) { slot ->
            val photo = sequence.photos[slot]
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelectSlot(slot) }
                    .semantics { contentDescription = if (photo == null) "Photo ${slot + 1} of ${sequence.photos.size}, empty. Tap to capture." else "Photo ${slot + 1} of ${sequence.photos.size}. Tap to retake." },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (photo != null) {
                        LocalCaptureFrameImage(
                            store.fileFor(sequence, photo, display = true).absolutePath,
                            Modifier.fillMaxWidth().aspectRatio(1.35f).background(Color(0xFFF4F5F7)),
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().aspectRatio(1.35f).background(Color(0xFFF4F5F7)), contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 24.sp, color = Color(0xFF687084))
                        }
                    }
                    Text("${slot + 1}", modifier = Modifier.padding(vertical = 6.dp), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun LocalCaptureViewer(sequence: CaptureSequence, store: CaptureSessionStore, modifier: Modifier = Modifier) {
    val paths = remember(sequence.id, sequence.photos) {
        sequence.photos.map { photo -> photo?.let { store.fileFor(sequence, it, display = true).absolutePath } }
    }
    val canvasAspectRatio = remember(sequence.id, sequence.photos) {
        sequence.photos.firstNotNullOfOrNull { photo ->
            photo?.let { it.width.toFloat() / it.height.toFloat() }
        } ?: (2048f / 1332f)
    }
    ImageSequenceViewerContent(
        frameCount = sequence.photos.size,
        contentDescription = "Personal motorcycle photo sequence",
        initialFrameIndex = 0,
        modifier = modifier,
        canvasAspectRatio = canvasAspectRatio,
    ) { index ->
        paths[index]?.let { LocalCaptureFrameImage(it, Modifier.fillMaxSize()) }
    }
}

@Composable
private fun LocalCaptureFrameImage(path: String, modifier: Modifier = Modifier) {
    var bitmap by remember(path) { mutableStateOf(CaptureBitmapCache.get(path)) }
    LaunchedEffect(path) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) { CaptureBitmapCache.getOrDecode(path) }
        }
    }
    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    } ?: Box(modifier = modifier.background(Color(0xFFF4F5F7)), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}

private object CaptureBitmapCache {
    private val bitmaps = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun get(path: String): Bitmap? = synchronized(bitmaps) { bitmaps.get(path) }

    fun getOrDecode(path: String): Bitmap? {
        get(path)?.let { return it }
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        synchronized(bitmaps) {
            bitmaps.get(path)?.let { bitmap.recycle(); return it }
            bitmaps.put(path, bitmap)
        }
        return bitmap
    }
}
