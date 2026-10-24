package com.example.atlantvpn

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atlantvpn.ui.theme.AtlantTextSecondary

@Composable
fun ConnectionDurationText(previewSeconds: Long? = null) {
    // Only the clock observes the one-second tick, not the whole server list.
    val seconds = if (previewSeconds != null) previewSeconds else {
        val live by VpnManager.connectionDuration.collectAsStateWithLifecycle()
        live
    }
    Text(formatDuration(seconds), fontSize = 13.sp, fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold, color = AtlantTextSecondary, letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 6.dp))
}
