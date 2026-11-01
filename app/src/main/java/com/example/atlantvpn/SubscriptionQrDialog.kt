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
fun SubscriptionQrDialog(subscription: SubscriptionInfo,connectionLinks:List<Pair<String,String>> = emptyList(),notice:String?=null,onDismiss: () -> Unit) {
    val context = LocalContext.current
    val links=remember(subscription.url,connectionLinks){(if(subscription.url.isNotBlank())listOf(tr("Подписка") to subscription.url) else emptyList())+connectionLinks}
    var selected by remember(links){mutableIntStateOf(0)}
    var menu by remember{mutableStateOf(false)}
    val url=links.getOrNull(selected)?.second.orEmpty()
    var copied by remember(url) { mutableStateOf(false) }
    var generating by remember(url){mutableStateOf(true)}
    val image by produceState<Bitmap?>(null, url) {
        value=null
        value = withContext(Dispatchers.Default) {
            runCatching {
                val matrix = subscriptionQrMatrix(url)
                val pixels = IntArray(matrix.width * matrix.height) { index ->
                    if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
                Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
            }.getOrNull()
        }
        generating=false
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(subscription.name) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                notice?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp))}
                if(links.size>1)Box {
                    OutlinedButton(onClick={menu=true},modifier=Modifier.fillMaxWidth()){Text(links[selected].first)}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                        links.forEachIndexed{index,link->DropdownMenuItem(text={Text(link.first)},onClick={selected=index;menu=false})}
                    }
                } else if(links.isNotEmpty())Text(links[0].first,style=MaterialTheme.typography.labelLarge)
                image?.let { Image(it.asImageBitmap(), stringResource(R.string.subscription_qr), Modifier.fillMaxWidth().aspectRatio(1f)) }
                if(generating)CircularProgressIndicator(Modifier.padding(24.dp))
                else if (image == null) Text(stringResource(R.string.qr_copy_fallback))
                Spacer(Modifier.height(12.dp))
                SelectionContainer { Text(url, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.subscription_share_warning), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(enabled=url.isNotBlank(),onClick = {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Atlant", url))
            copied = true
        }) { Text(if(copied)stringResource(R.string.link_copied) else tr("Копировать ссылку")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_share)) } })
}
