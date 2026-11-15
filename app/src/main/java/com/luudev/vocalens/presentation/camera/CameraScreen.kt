package com.luudev.vocalens.presentation.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon

@Composable
fun CameraRoute(viewModel: CameraViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun hasPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    var permissionGranted by remember { mutableStateOf(hasPermission()) }
    var requestedPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted && !requestedPermission) {
            requestedPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    // Refresh permission after returning from system settings, including one-time grants.
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionGranted = hasPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state = viewModel.uiState
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                AppIcon(AppGlyph.Back, Color.White, description = stringResource(R.string.camera_back))
            }
            Text(stringResource(R.string.camera_title), color = Color.White, modifier = Modifier.weight(1f))
            if (permissionGranted && state.canSwitchCamera) {
                TextButton(onClick = viewModel::onSwitchCamera) {
                    Text(stringResource(R.string.camera_switch), color = Color.White)
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (permissionGranted) {
                CameraPreview(viewModel, Modifier.fillMaxSize())
                if (state.hasError) {
                    CameraMessage(stringResource(R.string.camera_error)) {
                        Button(onClick = viewModel::onRetry) { Text(stringResource(R.string.camera_retry)) }
                    }
                } else if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                CameraMessage(stringResource(R.string.camera_permission_message)) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.camera_grant_permission))
                    }
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)))
                    }) { Text(stringResource(R.string.camera_settings), color = Color.White) }
                }
            }
        }
        if (permissionGranted && !state.hasError) {
            Text(stringResource(R.string.camera_hint), color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}

@Composable
private fun CameraPreview(viewModel: CameraViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = viewModel.uiState
    val previewView = remember(context) {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner, previewView, state.useFrontCamera, state.retryCount) {
        viewModel.onOpening()
        var disposed = false
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        var cameraStates: androidx.lifecycle.LiveData<CameraState>? = null
        val cameraObserver = Observer<CameraState> { cameraState ->
            if (cameraState.error != null) viewModel.onError()
            else if (cameraState.type == CameraState.Type.OPEN) viewModel.onReady()
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
                    val useCase = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    preview = useCase
                    val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, useCase)
                    cameraStates = camera.cameraInfo.cameraState
                    cameraStates?.observe(lifecycleOwner, cameraObserver)
                } catch (_: Exception) {
                    viewModel.onError()
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            cameraStates?.removeObserver(cameraObserver)
            preview?.let { provider?.unbind(it) }
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
