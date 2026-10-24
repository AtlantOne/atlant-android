package com.example.atlantvpn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.atlantvpn.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionServerList(
    servers: List<ServerProfile>, selectedServer: ServerProfile?, subscription: SubscriptionInfo?,
    onSelectServer: (ServerProfile) -> Unit, onDeleteServer: (ServerProfile) -> Unit,
    onEditServer: (ServerProfile) -> Unit, onPingServer: (ServerProfile) -> Unit,
    onCopyLink: (ServerProfile) -> Unit, onUpdateSubscription: () -> Unit,
    onEditSubscription: () -> Unit, onClearSubscription: () -> Unit,
    modifier: Modifier = Modifier,
    subscriptions: List<SubscriptionInfo> = listOfNotNull(subscription),
    onFocusSubscription: (String) -> Unit = {}
) {
    var collapsed by rememberSaveable { mutableStateOf(listOf<String>()) }
    val standalone = remember(servers) { servers.filterNot { it.isSubscription } }
    val groupedServers = remember(servers) { servers.filter { it.isSubscription }.groupBy { it.subscriptionId } }

    LazyColumn(modifier = modifier.testTag("server-list"), contentPadding = PaddingValues(bottom = 12.dp)) {
        subscriptions.forEachIndexed { index, subscription ->
            val groupKey = subscription.id.ifBlank { subscription.url }
            val expanded = groupKey !in collapsed
            val grouped = groupedServers[subscription.id].orEmpty()
            if (index > 0) item(key = "subscription-gap:$groupKey") { Spacer(Modifier.height(4.dp)) }
            item(key = "subscription-header:$groupKey", contentType = "subscription-header") {
                SubscriptionGroupHeader(subscription, grouped.size, expanded,
                    onToggle = { collapsed = if (expanded) collapsed + groupKey else collapsed - groupKey },
                    onUpdate = { onFocusSubscription(subscription.id); onUpdateSubscription() },
                    onEdit = { onFocusSubscription(subscription.id); onEditSubscription() },
                    onClear = { onFocusSubscription(subscription.id); onClearSubscription() })
            }
            if (expanded) {
                itemsIndexed(grouped, key = { _, server -> "subscription:${server.id}" }, contentType = { _, _ -> "subscription-server" }) { serverIndex, server ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("subscription-server:${server.id}"),
                        color = AtlantSurface, border = BorderStroke(1.dp, AtlantCardBorder),
                        shape = if (serverIndex == grouped.lastIndex)
                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        else RoundedCornerShape(0.dp)
                    ) {
                        Box(Modifier.padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)) {
                            ServerItemCard(server, server.id == selectedServer?.id,
                                { onSelectServer(server) }, { onPingServer(server) },
                                { onEditServer(server) }, { onCopyLink(server) }, { onDeleteServer(server) })
                        }
                    }
                }
            }
        }
        if (standalone.isNotEmpty() && subscriptions.isNotEmpty()) {
            item(key = "standalone-header") {
                Text(tr("Добавленные вручную"), color = AtlantTextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            }
        }
        items(standalone, key = { "manual:${it.id}" }, contentType = { "server" }) { server ->
            Box(Modifier.fillMaxWidth()) {
                ServerItemCard(server, server.id == selectedServer?.id,
                    { onSelectServer(server) }, { onPingServer(server) },
                    { onEditServer(server) }, { onCopyLink(server) }, { onDeleteServer(server) })
            }
        }
        if (servers.isEmpty() && subscriptions.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("Список серверов пуст"), color = AtlantTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(tr("Нажмите +, чтобы добавить сервер или подписку"), color = AtlantTextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SubscriptionGroupHeader(
    subscription: SubscriptionInfo, count: Int, expanded: Boolean,
    onToggle: () -> Unit, onUpdate: () -> Unit, onEdit: () -> Unit, onClear: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(), color = AtlantSurface,
        border = BorderStroke(1.dp, AtlantCardBorder),
        shape = if (expanded && count > 0)
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        else RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .testTag("subscription-toggle")
                        .semantics { stateDescription = if (expanded) tr("Развёрнута") else tr("Свёрнута") }
                        .clickable(role = Role.Button, onClick = onToggle),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(subscription.name, color = AtlantTextPrimary, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(tr("Подписка · серверов: {0}", count), color = AtlantTextMuted, fontSize = 11.sp)
                    }
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null, tint = AtlantTextMuted, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showQr = true }, enabled = subscription.url.isNotBlank(), modifier = Modifier.testTag("subscription-share")) {
                    Icon(Icons.Default.QrCode, contentDescription = stringResource(R.string.subscription_qr), tint = AtlantTextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onUpdate, modifier = Modifier.testTag("subscription-refresh")) {
                    Icon(Icons.Default.Refresh, contentDescription = tr("Обновить подписку"), tint = AtlantTextSecondary, modifier = Modifier.size(18.dp))
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.testTag("subscription-actions")) {
                        Icon(Icons.Default.MoreVert, contentDescription = tr("Действия подписки"), tint = AtlantTextSecondary, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(tr("Редактировать подписку")) }, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(text = { Text(tr("Удалить подписку"), color = AtlantDanger) }, onClick = { menu = false; confirmDelete = true })
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(tr("Трафик: {0}", subscription.remainingGB), color = AtlantPrimary, fontSize = 11.sp)
                if (expanded && subscription.expire > 0) {
                    Text(tr("До ") + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(subscription.expire * 1000)),
                        color = AtlantTextMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (subscription.total > 0) {
                val usage = subscriptionUsage(subscription.upload, subscription.download, subscription.total)
                LinearProgressIndicator(progress = { usage.fraction },
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 6.dp, bottom = 6.dp), color = AtlantPrimary)
            }
            if (expanded) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (subscription.lastUpdated.isNotBlank()) Text(tr("Обновлено {0}", subscription.lastUpdated), color = AtlantTextMuted, fontSize = 10.sp)
                    if (subscription.usageScope == "local-account") Text(tr("Учёт: сервер подписки"), color = AtlantTextMuted,
                        fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                    if (subscription.usageScope == "global-subscription") Text(tr("Общий учёт всех VDS"), color = AtlantTextMuted,
                        fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                }
                if (subscription.announcement.isNotBlank()) Text(subscription.announcement, color = AtlantTextSecondary,
                    fontSize = 11.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
            }
        }
    }
    if (showQr) SubscriptionQrDialog(subscription, onDismiss = { showQr = false })
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text(tr("Удалить подписку?")) },
            text = { Text(tr("«{0}» и её серверы будут удалены из приложения. Серверы, добавленные вручную, останутся.", subscription.name)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onClear() }) { Text(tr("Удалить"), color = AtlantDanger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Отмена")) } })
    }
}
