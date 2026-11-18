package com.luudev.vocalens.presentation.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.RecognizedObject
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.theme.KoiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CameraRoute(
    viewModel: CameraViewModel,
    onWordSelected: (RecognizedObject) -> Unit = {},
    wordEditor: @Composable () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun hasPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    var permissionGranted by remember { mutableStateOf(hasPermission()) }
    var requestedPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        if (!it) viewModel.onRetake()
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted && !requestedPermission) {
            requestedPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = hasPermission()
                if (!permissionGranted) viewModel.onRetake()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(viewModel) { onDispose { viewModel.onRetake() } }
    val state = viewModel.uiState
    BackHandler(state.stage != CameraStage.Preview) { viewModel.onRetake() }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (state.stage == CameraStage.Preview) onBack() else viewModel.onRetake()
            }) { AppIcon(AppGlyph.Back, Color.White, description = stringResource(R.string.camera_back)) }
            Text(stringResource(R.string.camera_title), color = Color.White, modifier = Modifier.weight(1f))
            if (permissionGranted && state.canSwitchCamera && state.stage == CameraStage.Preview) {
                TextButton(onClick = viewModel::onSwitchCamera, enabled = !state.isLoading) {
                    Text(stringResource(R.string.camera_switch), color = Color.White)
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (!permissionGranted) {
                CameraMessage(stringResource(R.string.camera_permission_message)) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.camera_grant_permission))
                    }
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)))
                    }) { Text(stringResource(R.string.camera_settings), color = Color.White) }
                }
            } else if (state.photo != null) {
                RecognitionResults(state, viewModel::onRetake, viewModel::retryRecognition, onWordSelected)
            } else {
                CameraPreview(viewModel, Modifier.fillMaxSize())
                if (state.hasError) {
                    CameraMessage(stringResource(R.string.camera_error)) {
                        Button(onClick = viewModel::onRetry) { Text(stringResource(R.string.camera_retry)) }
                    }
                } else if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
    wordEditor()
}

@Composable
private fun CameraPreview(viewModel: CameraViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = viewModel.uiState
    val previewView = remember(context) {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(
            android.util.Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build()).build() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = .55f))
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(if (state.captureError == CaptureError.Capture) R.string.camera_capture_error
                else R.string.camera_hint), color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(
                modifier = Modifier.testTag("camera_capture"),
                enabled = !state.isLoading && !state.hasError && state.stage == CameraStage.Preview,
                colors = ButtonDefaults.buttonColors(containerColor = KoiColors.Primary),
                onClick = {
                    val ticket = viewModel.onCaptureStarted()
                    if (ticket != null) {
                        capture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                        try {
                            capture.takePicture(Dispatchers.IO.asExecutor(), object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    try {
                                        val bytes = uprightJpeg(image)
                                        mainExecutor.execute { viewModel.onCaptured(ticket, bytes) }
                                    } catch (_: Exception) {
                                        mainExecutor.execute { viewModel.onCaptureFailed(ticket) }
                                    } finally { image.close() }
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    mainExecutor.execute { viewModel.onCaptureFailed(ticket) }
                                }
                            })
                        } catch (_: Exception) { viewModel.onCaptureFailed(ticket) }
                    }
                }
            ) {
                if (state.stage == CameraStage.Capturing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else Text(stringResource(R.string.camera_capture))
            }
        }
    }
    DisposableEffect(lifecycleOwner, previewView, state.useFrontCamera, state.retryCount) {
        viewModel.onOpening()
        var disposed = false
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        var cameraStates: androidx.lifecycle.LiveData<CameraState>? = null
        val cameraObserver = Observer<CameraState> {
            if (it.error != null) viewModel.onError()
            else if (it.type == CameraState.Type.OPEN) viewModel.onReady()
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!disposed) {
                try {
                    val cameraProvider = future.get()
                    provider = cameraProvider
                    val hasBack = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    val hasFront = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                    viewModel.onCameraAvailability(hasBack, hasFront)
                    check(hasBack || hasFront)
                    val selector = if (viewModel.uiState.useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                        else CameraSelector.DEFAULT_BACK_CAMERA
                    val useCase = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    preview = useCase
                    val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, useCase, capture)
                    cameraStates = camera.cameraInfo.cameraState
                    cameraStates?.observe(lifecycleOwner, cameraObserver)
                } catch (_: Exception) { viewModel.onError() }
            }
        }, mainExecutor)
        onDispose {
            disposed = true
            cameraStates?.removeObserver(cameraObserver)
            preview?.let { provider?.unbind(it, capture) }
        }
    }
}

/** Copy pixels while the proxy is open, apply rotation, and cap memory retained by the UI. */
private fun uprightJpeg(image: ImageProxy): ByteArray {
    val bitmap = image.toBitmap()
    var upright: Bitmap? = null
    var resized: Bitmap? = null
    try {
        upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }, true)
        val scale = min(1f, 1280f / maxOf(upright.width, upright.height))
        resized = Bitmap.createScaledBitmap(upright, (upright.width * scale).roundToInt().coerceAtLeast(1),
            (upright.height * scale).roundToInt().coerceAtLeast(1), true)
        return ByteArrayOutputStream().use {
            check(resized.compress(Bitmap.CompressFormat.JPEG, 92, it))
            it.toByteArray()
        }
    } finally {
        if (resized != null && resized !== upright && resized !== bitmap) resized.recycle()
        if (upright != null && upright !== bitmap) upright.recycle()
        bitmap.recycle()
    }
}

@Composable
internal fun RecognitionResults(state: CameraUiState, onRetake: () -> Unit, onRetry: () -> Unit,
    onWordSelected: (RecognizedObject) -> Unit = {}) {
    val photo = state.photo
    val bitmap by produceState<Bitmap?>(null, photo) {
        value = withContext(Dispatchers.Default) {
            photo?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
    }
    Column(Modifier.fillMaxSize().background(KoiColors.Mint)) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
            bitmap?.let { image ->
                Image(image.asImageBitmap(), contentDescription = stringResource(R.string.camera_photo),
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                Canvas(Modifier.fillMaxSize()) {
                    val scale = min(size.width / image.width, size.height / image.height)
                    val width = image.width * scale
                    val height = image.height * scale
                    val origin = Offset((size.width - width) / 2, (size.height - height) / 2)
                    state.objects.forEach {
                        val b = it.bounds
                        drawRect(Color(0xFF70F3BE), origin + Offset(b.left * width, b.top * height),
                            Size((b.right - b.left) * width, (b.bottom - b.top) * height),
                            style = Stroke(2.dp.toPx()))
                    }
                }
            }
            if (state.stage == CameraStage.Analyzing) {
                Column(Modifier.background(Color.Black.copy(alpha = .7f)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.camera_analyzing), color = Color.White)
                }
            }
        }
        if (state.stage == CameraStage.Results) {
            Text(stringResource(when {
                state.captureError != null -> R.string.camera_recognition_error
                state.objects.isEmpty() -> R.string.camera_no_objects
                else -> R.string.camera_results
            }), modifier = Modifier.padding(16.dp), color = KoiColors.Ink)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 200.dp).testTag("camera_results"),
                contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.objects.groupBy { it.classId }.values.map { matches -> matches.maxBy { it.confidence } },
                    key = { it.classId }) { item ->
                    Surface(onClick = { onWordSelected(item) },
                        shape = MaterialTheme.shapes.medium, color = KoiColors.Surface) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.label, Modifier.weight(1f), color = KoiColors.Ink)
                            Text(stringResource(R.string.camera_confidence, (item.confidence * 100).roundToInt()),
                                style = MaterialTheme.typography.bodySmall, color = KoiColors.Muted)
                            Spacer(Modifier.width(8.dp))
                            AppIcon(AppGlyph.Chevron, KoiColors.Primary)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.camera_retake))
            }
            if (state.captureError == CaptureError.Recognition) {
                Button(onClick = onRetry, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.camera_retry)) }
            }
        }
    }
}

@Composable
private fun CameraMessage(message: String, actions: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        actions()
    }
}
