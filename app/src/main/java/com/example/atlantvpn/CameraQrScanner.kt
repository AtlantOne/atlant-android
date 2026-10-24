package com.example.atlantvpn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun QrScanDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    var manual by rememberSaveable { mutableStateOf(false) }
    var cameraFailed by rememberSaveable { mutableStateOf(false) }
    var launched by rememberSaveable { mutableStateOf(false) }
    val prompt = stringResource(R.string.qr_camera_prompt)
    val latestConfirm by rememberUpdatedState(onConfirm)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents?.trim()
        if (contents.isNullOrEmpty()) latestDismiss() else latestConfirm(contents)
    }
    fun scan() {
        denied = false
        launched = true
        try {
            scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(prompt).setBeepEnabled(false).setBarcodeImageEnabled(false)
                .setOrientationLocked(false))
        } catch (_: Exception) { launched = false; cameraFailed = true }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        launched = false
        if (granted) scan() else denied = true
    }
    val owner = LocalLifecycleOwner.current
    val latestScan by rememberUpdatedState { scan() }
    DisposableEffect(owner, denied) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && denied &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) latestScan()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        if (launched || denied || manual || cameraFailed) return@LaunchedEffect
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) scan()
        else { launched = true; permission.launch(Manifest.permission.CAMERA) }
    }
    if (manual) {
        PasteLinkDialog(onDismiss = latestDismiss, onConfirm = latestConfirm)
    } else if (denied || cameraFailed) {
        AlertDialog(onDismissRequest = latestDismiss,
            title = { Text(stringResource(R.string.qr_camera_title)) },
            text = { Text(stringResource(if (cameraFailed) R.string.qr_camera_unavailable else R.string.qr_camera_permission)) },
            confirmButton = { TextButton(onClick = {
                if (cameraFailed) { cameraFailed = false; scan() }
                else try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
                catch (_: Exception) { cameraFailed = true }
            }) { Text(stringResource(if (cameraFailed) R.string.qr_camera_retry else R.string.qr_camera_settings)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { manual = true }) { Text(stringResource(R.string.qr_manual_input)) }
                    TextButton(onClick = latestDismiss) { Text(stringResource(R.string.close_share)) }
                }
            })
    }
}
