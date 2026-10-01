package com.motoscope.capture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.motoscope.App
import com.motoscope.MotoScopeHomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class CaptureRoute { Home, Guidance, Camera, Review, SpinPreview, SavedViewer }

private data class PendingCameraRequest(
    val createDraft: Boolean,
    val slot: Int?,
    val returnToReview: Boolean,
)

@Composable
internal fun MotoScopeAndroidRoot(lifecycleOwner: LifecycleOwner, activity: Activity) {
    val context = LocalContext.current
    val store = remember(context) { CaptureSessionStore(File(context.noBackupFilesDir, "motoscope-capture")) }
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var storeState by remember { mutableStateOf(CaptureStoreState()) }
    var route by remember { mutableStateOf(CaptureRoute.Home) }
    var selectedSlot by remember { mutableIntStateOf(0) }
    var returnToReview by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var permissionError by remember { mutableStateOf<String?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }
    var pendingRequest by remember { mutableStateOf<PendingCameraRequest?>(null) }

    LaunchedEffect(store) {
        try {
            storeState = withContext(Dispatchers.IO) {
                store.cleanupInterruptedWrites()
                store.load()
            }
            storeState.recoverableIssue?.let {
                error = it
                route = CaptureRoute.Guidance
            }
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            error = failure.message ?: "Saved capture data could not be loaded."
            route = CaptureRoute.Guidance
        } finally {
            loaded = true
        }
    }

    fun openCamera(request: PendingCameraRequest) {
        permissionError = null
        permissionDenied = false
        scope.launch {
            busy = true
            error = null
            try {
                val draft = withContext(Dispatchers.IO) {
                    if (request.createDraft) store.createDraft() else store.load().draft
                        ?: error("There is no saved draft to resume")
                }
                storeState = withContext(Dispatchers.IO) { store.load() }
                selectedSlot = request.slot ?: draft.currentSlot
                returnToReview = request.returnToReview
                route = if (!request.createDraft && draft.isComplete && request.slot == null) {
                    CaptureRoute.Review
                } else {
                    CaptureRoute.Camera
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                error = failure.message ?: "Could not open the capture session."
                route = CaptureRoute.Guidance
            } finally {
                busy = false
            }
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val request = pendingRequest
        pendingRequest = null
        if (granted && request != null) openCamera(request)
        else {
            permissionDenied = true
            permissionError = if (activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                "Camera permission is needed to take the ${CaptureSequence.SLOT_COUNT} photos. You can try again when ready."
            } else {
                "Camera access is off. Allow Camera in Android App settings to capture photos."
            }
            route = CaptureRoute.Guidance
        }
    }

    fun requestCamera(createDraft: Boolean, slot: Int? = null, fromReview: Boolean = false) {
        val request = PendingCameraRequest(createDraft, slot, fromReview)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera(request)
        } else {
            pendingRequest = request
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    App {
        when (route) {
            CaptureRoute.Home -> MotoScopeHomeScreen(
                captureActionLabel = when {
                    storeState.draft != null -> "Resume capture"
                    storeState.finalized != null -> "View your capture"
                    else -> "Capture your motorcycle"
                },
                onCaptureAction = {
                    error = null
                    if (storeState.draft != null) route = CaptureRoute.Guidance
                    else if (storeState.finalized != null) route = CaptureRoute.SavedViewer
                    else route = CaptureRoute.Guidance
                },
            )

            CaptureRoute.Guidance -> CaptureGuidanceScreen(
                draft = storeState.draft,
                hasFinal = storeState.finalized != null,
                busy = busy,
                error = error ?: permissionError,
                permissionDenied = permissionDenied,
                onBack = { route = CaptureRoute.Home },
                onStart = { requestCamera(createDraft = storeState.draft == null) },
                onViewFinal = { route = CaptureRoute.SavedViewer },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                    )
                },
                onDiscard = { sequence ->
                    scope.launch {
                        busy = true
                        try {
                            storeState = withContext(Dispatchers.IO) { store.discardDraft(sequence.id) }
                            error = null
                        } catch (failure: CancellationException) {
                            throw failure
                        } catch (failure: Exception) {
                            error = failure.message ?: "Could not discard this draft."
                        } finally { busy = false }
                        if (error == null) requestCamera(createDraft = true)
                    }
                },
            )

            CaptureRoute.Camera -> {
                val draft = storeState.draft
                if (draft == null) {
                    CaptureMessageScreen("The draft is unavailable.", onBack = { route = CaptureRoute.Home })
                } else {
                    CaptureCameraScreen(
                        lifecycleOwner = lifecycleOwner,
                        sequence = draft,
                        slot = selectedSlot.coerceIn(0, CaptureSequence.SLOT_COUNT - 1),
                        store = store,
                        busy = busy,
                        error = error,
                        onBack = { route = if (returnToReview) CaptureRoute.Review else CaptureRoute.Guidance },
                        onSelectSlot = { selectedSlot = it.coerceIn(0, CaptureSequence.SLOT_COUNT - 1) },
                        onReview = { route = CaptureRoute.Review },
                        onClearError = { error = null },
                        onCaptureError = { error = it },
                        onCaptureSaved = { slot, original, display, prepared ->
                            busy = true
                            error = null
                            try {
                                val committed = withContext(Dispatchers.IO) {
                                    store.acceptCapturedPhoto(
                                        sequence = draft,
                                        slot = slot,
                                        originalTemp = original,
                                        displayTemp = display,
                                        width = prepared.width,
                                        height = prepared.height,
                                        orientationDegrees = prepared.orientationDegrees,
                                    )
                                }
                                storeState = storeState.copy(draft = committed)
                                selectedSlot = committed.currentSlot
                                if (returnToReview || committed.isComplete) route = CaptureRoute.Review
                            } catch (failure: CancellationException) {
                                throw failure
                            } catch (failure: Exception) {
                                original.delete()
                                display.delete()
                                error = failure.message ?: "Photo could not be saved. Try again."
                            } finally { busy = false }
                        },
                    )
                }
            }

            CaptureRoute.Review -> {
                val draft = storeState.draft
                if (draft == null) {
                    CaptureMessageScreen("The draft is unavailable.", onBack = { route = CaptureRoute.Home })
                } else {
                    CaptureReviewScreen(
                        sequence = draft,
                        store = store,
                        busy = busy,
                        error = error,
                        onBack = { route = CaptureRoute.Guidance },
                        onSelectSlot = { slot -> requestCamera(createDraft = false, slot = slot, fromReview = true) },
                        onPreview = { route = CaptureRoute.SpinPreview },
                        onFinish = {
                            scope.launch {
                                busy = true
                                try {
                                    val nextState = withContext(Dispatchers.IO) {
                                        val verified = store.load().draft ?: error("Draft is missing")
                                        verified.photos.forEach { photo ->
                                            require(photo != null && CaptureImageProcessor.isReadableDisplayImage(
                                                store.fileFor(verified, photo, display = true),
                                            )) { "A viewer photo is unreadable; retake that slot before finishing." }
                                        }
                                        store.finalizeDraft(verified)
                                    }
                                    storeState = nextState
                                    route = CaptureRoute.SavedViewer
                                    error = null
                                } catch (failure: CancellationException) {
                                    throw failure
                                } catch (failure: Exception) {
                                    error = failure.message ?: "Could not finish this capture."
                                } finally { busy = false }
                            }
                        },
                    )
                }
            }

            CaptureRoute.SpinPreview, CaptureRoute.SavedViewer -> {
                val sequence = if (route == CaptureRoute.SpinPreview) storeState.draft else storeState.finalized
                if (sequence == null) {
                    CaptureMessageScreen("There is no complete capture to view.", onBack = { route = CaptureRoute.Home })
                } else {
                    PersonalCaptureScreen(
                        sequence = sequence,
                        store = store,
                        isDraft = route == CaptureRoute.SpinPreview,
                        hasDraft = storeState.draft != null,
                        error = error,
                        onBack = { route = if (route == CaptureRoute.SpinPreview) CaptureRoute.Review else CaptureRoute.Home },
                        onReplace = { route = CaptureRoute.Guidance },
                        onResumeDraft = { route = CaptureRoute.Guidance },
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureGuidanceScreen(
    draft: CaptureSequence?,
    hasFinal: Boolean,
    busy: Boolean,
    error: String?,
    permissionDenied: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onViewFinal: () -> Unit,
    onOpenSettings: () -> Unit,
    onDiscard: (CaptureSequence) -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    BackHandler(enabled = !confirmDiscard, onBack = onBack)
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("‹  Home") }
        Text("Capture your motorcycle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Take ${CaptureSequence.SLOT_COUNT} still photos in order. The sequence is a visual spin—not a measurement of exact angles.")
        Text("• Keep the motorcycle stationary and fully inside the framing guide.")
        Text("• Use the same rear camera and lens throughout; keep phone height and distance consistent.")
        Text("• Move a small step in one direction between photos. You may stop or reposition whenever needed.")
        Text("• Complete a circuit around the motorcycle; the last photo should show a view different from photo 1.")
        Text("Photos stay on this device and are removed if the app is uninstalled.", color = Color(0xFF687084))
        if (draft != null) {
            Text("Draft progress: ${draft.completedCount} of ${CaptureSequence.SLOT_COUNT} photos")
            Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Resume capture")
            }
            OutlinedButton(onClick = { confirmDiscard = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Discard draft and restart")
            }
        } else {
            Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Start ${CaptureSequence.SLOT_COUNT}-photo capture")
            }
        }
        if (hasFinal) TextButton(onClick = onViewFinal, enabled = !busy) { Text("View saved capture") }
        if (permissionDenied) TextButton(onClick = onOpenSettings) { Text("Camera permission settings") }
        if (busy) CircularProgressIndicator()
        error?.let { Text(it, color = Color(0xFFB3261E)) }
    }
    if (confirmDiscard && draft != null) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this draft?") },
            text = { Text("The current draft photos will be permanently deleted. Your saved capture, if any, will remain.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDiscard(draft) }) { Text("Discard draft") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep draft") } },
        )
    }
}

@Composable
private fun CaptureReviewScreen(
    sequence: CaptureSequence,
    store: CaptureSessionStore,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSelectSlot: (Int) -> Unit,
    onPreview: () -> Unit,
    onFinish: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹  Back") }
            Text("${sequence.completedCount} / ${CaptureSequence.SLOT_COUNT}")
        }
        Text("Review your photos", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Select any tile to retake that position. Each slot is sequence order, not a measured angle.", color = Color(0xFF687084))
        Spacer(Modifier.height(12.dp))
        CaptureContactSheet(sequence, store, onSelectSlot, Modifier.weight(1f))
        error?.let { Text(it, color = Color(0xFFB3261E), modifier = Modifier.padding(vertical = 8.dp)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPreview, enabled = sequence.isComplete && !busy, modifier = Modifier.weight(1f)) {
                Text("Spin preview")
            }
            Button(onClick = onFinish, enabled = sequence.isComplete && !busy, modifier = Modifier.weight(1f)) {
                Text("Finish")
            }
        }
        if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun PersonalCaptureScreen(
    sequence: CaptureSequence,
    store: CaptureSessionStore,
    isDraft: Boolean,
    hasDraft: Boolean,
    error: String?,
    onBack: () -> Unit,
    onReplace: () -> Unit,
    onResumeDraft: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹  ${if (isDraft) "Review" else "Home"}") }
            Text(if (isDraft) "Preview" else "Your motorcycle", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        LocalCaptureViewer(sequence, store, Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        error?.let { Text(it, color = Color(0xFFB3261E)) }
        if (isDraft) {
            Text("Preview of your ${sequence.photos.size}-photo sequence", color = Color(0xFF687084))
        } else {
            if (hasDraft) TextButton(onClick = onResumeDraft, modifier = Modifier.fillMaxWidth()) { Text("Resume replacement draft") }
            Button(onClick = onReplace, modifier = Modifier.fillMaxWidth()) { Text("Replace capture") }
        }
    }
}

@Composable
private fun CaptureMessageScreen(message: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message)
        TextButton(onClick = onBack) { Text("Back") }
    }
}
