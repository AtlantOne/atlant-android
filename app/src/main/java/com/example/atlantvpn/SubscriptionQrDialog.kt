package com.example.atlantvpn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun subscriptionQrMatrix(url: String) = QRCodeWriter().encode(
    url.also { require(it.isNotBlank() && it.length <= 2048) }, BarcodeFormat.QR_CODE, 512, 512,
    mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 4,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
)

@Composable
fun SubscriptionQrDialog(subscription: SubscriptionInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var copied by remember(subscription.url) { mutableStateOf(false) }
    val image by produceState<Bitmap?>(null, subscription.url) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val matrix = subscriptionQrMatrix(subscription.url)
                val pixels = IntArray(matrix.width * matrix.height) { index ->
                    if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
                Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
            }.getOrNull()
        }
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(subscription.name) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                image?.let { Image(it.asImageBitmap(), stringResource(R.string.subscription_qr), Modifier.fillMaxWidth().aspectRatio(1f)) }
                if (image == null) Text(stringResource(R.string.qr_copy_fallback))
                Spacer(Modifier.height(12.dp))
                SelectionContainer { Text(subscription.url, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.subscription_share_warning), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Atlant subscription", subscription.url))
            copied = true
        }) { Text(stringResource(if (copied) R.string.link_copied else R.string.copy_subscription_link)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_share)) } })
}
