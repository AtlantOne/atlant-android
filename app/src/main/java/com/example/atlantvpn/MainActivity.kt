package com.example.atlantvpn

import android.app.Activity
import android.content.Context
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.atlantvpn.ui.theme.AtlantBackground
import com.example.atlantvpn.ui.theme.AtlantCardBorder
import com.example.atlantvpn.ui.theme.AtlantConnectedGreen
import com.example.atlantvpn.ui.theme.AtlantConnectingOrange
import com.example.atlantvpn.ui.theme.AtlantPrimary
import com.example.atlantvpn.ui.theme.AtlantSurface
import com.example.atlantvpn.ui.theme.AtlantTextMuted
import com.example.atlantvpn.ui.theme.AtlantTextPrimary
import com.example.atlantvpn.ui.theme.AtlantTextSecondary
import com.example.atlantvpn.ui.theme.AtlantVPNTheme
import com.example.atlantvpn.ui.theme.AtlantSurfaceVariant
import com.example.atlantvpn.ui.theme.AtlantBadgeBg
import com.example.atlantvpn.ui.theme.AtlantDanger
import com.example.atlantvpn.ui.theme.AtlantDangerSurface

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VpnManager.init(this)
        enableEdgeToEdge()
        setContent {
            val settings by VpnManager.appSettings.collectAsStateWithLifecycle()
            AtlantVPNTheme(darkTheme = settings.themeMode.isDark(isSystemInDarkTheme())) {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showQrDialog by rememberSaveable { mutableStateOf(false) }
    var serverToEdit by remember { mutableStateOf<ServerProfile?>(null) }
    var subscriptionToEdit by remember { mutableStateOf<SubscriptionInfo?>(null) }

    val vpnStatus by VpnManager.status.collectAsStateWithLifecycle()
    val selectedServer by VpnManager.selectedServer.collectAsStateWithLifecycle()
    val serverList by VpnManager.servers.collectAsStateWithLifecycle()
    val appSettings by VpnManager.appSettings.collectAsStateWithLifecycle()
    val routingSettings by VpnManager.routingSettings.collectAsStateWithLifecycle()
    val subscriptionInfo by VpnManager.subscription.collectAsStateWithLifecycle()
    val subscriptions by VpnManager.subscriptions.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val started = VpnManager.startVpn(context)
            if (!started) {
                Toast.makeText(context, tr("Выберите сервер для подключения"), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, tr("Требуется разрешение для запуска VPN"), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleConnectToggle() {
        if (vpnStatus == VpnStatus.CONNECTED || vpnStatus == VpnStatus.CONNECTING) {
            VpnManager.stopVpn(context)
        } else {
            var serverToConnect = selectedServer
            if (serverToConnect == null) {
                if (serverList.isNotEmpty()) {
                    serverToConnect = serverList.first()
                    VpnManager.selectServer(context, serverToConnect)
                } else {
                    Toast.makeText(context, tr("Сначала добавьте сервер через '+' в правом верхнем углу!"), Toast.LENGTH_SHORT).show()
                    return
                }
            }
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                vpnPrepareLauncher.launch(vpnIntent)
            } else {
                VpnManager.startVpn(context)
            }
        }
    }

    // Separate Dedicated Screen for Editing Server
    val currentServerToEdit = serverToEdit
    if (currentServerToEdit != null) {
        EditServerScreen(
            server = currentServerToEdit,
            onBack = { serverToEdit = null },
            onConfirm = { updated ->
                VpnManager.updateServer(context, updated)
                serverToEdit = null
                Toast.makeText(context, tr("Сервер '{0}' сохранен", updated.name), Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                VpnManager.removeServer(context, currentServerToEdit.id)
                serverToEdit = null
                Toast.makeText(context, tr("Сервер удален"), Toast.LENGTH_SHORT).show()
            },
            onPing = { srv ->
                VpnManager.pingServer(srv)
            },
            onCopyLink = { srv ->
                val link = VpnManager.generateAtlantLink(srv)
                clipboardManager.setText(AnnotatedString(link))
                Toast.makeText(context, tr("Ключ скопирован в буфер!"), Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = AtlantSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = tr("Главная")) },
                    label = { Text(tr("Главная"), fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AtlantPrimary,
                        selectedTextColor = AtlantPrimary,
                        unselectedIconColor = AtlantTextMuted,
                        unselectedTextColor = AtlantTextMuted,
                        indicatorColor = AtlantBadgeBg
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = tr("Настройки")) },
                    label = { Text(tr("Настройки"), fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AtlantPrimary,
                        selectedTextColor = AtlantPrimary,
                        unselectedIconColor = AtlantTextMuted,
                        unselectedTextColor = AtlantTextMuted,
                        indicatorColor = AtlantBadgeBg
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AtlantBackground)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> ConnectionScreen(
                    status = vpnStatus,
                    durationSec = null,
                    servers = serverList,
                    selectedServer = selectedServer,
                    subscription = subscriptionInfo,
                    subscriptions = subscriptions,
                    onFocusSubscription = VpnManager::focusSubscription,
                    onConnectToggle = { handleConnectToggle() },
                    onSelectServer = { VpnManager.selectServer(context, it) },
                    onDeleteServer = { VpnManager.removeServer(context, it.id) },
                    onEditServer = { serverToEdit = it },
                    onEditSubscription = { subscriptionToEdit = VpnManager.subscription.value },
                    onPingServer = { VpnManager.pingServer(it) },
                    onPingAll = { VpnManager.pingAll() },
                    onAddServerClick = { showAddDialog = true },
                    onPasteClipboard = {
                        val clip = clipboardManager.getText()?.text ?: ""
                        if (clip.isNotBlank()) {
                            VpnManager.importClipboardOrUrl(context, clip) { ok, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, tr("Буфер обмена пуст"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onQrScanClick = { showQrDialog = true },
                    onUpdateSubscription = {
                        VpnManager.updateSubscription(context) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClearSubscription = {
                        VpnManager.clearSubscription(context)
                        Toast.makeText(context, tr("Подписка удалена"), Toast.LENGTH_SHORT).show()
                    },
                    onCopyLink = {
                        val link = VpnManager.generateAtlantLink(it)
                        clipboardManager.setText(AnnotatedString(link))
                        Toast.makeText(context, tr("Ключ скопирован в буфер!"), Toast.LENGTH_SHORT).show()
                    }
                )
                1 -> SettingsScreen(
                    settings = appSettings,
                    routing = routingSettings,
                    onSaveSettings = {
                        VpnManager.saveSettings(context, it)
                    },
                    onSaveRouting = {
                        VpnManager.saveRouting(context, it)
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { profile ->
                VpnManager.addServer(context, profile)
                showAddDialog = false
                Toast.makeText(context, tr("Сервер добавлен"), Toast.LENGTH_SHORT).show()
            }
        )
    }

    subscriptionToEdit?.let { editing ->
        EditSubscriptionDialog(
            subscription = editing,
            onDismiss = { subscriptionToEdit = null },
            onConfirm = { newName, newUrl, refreshNow ->
                subscriptionToEdit = null
                VpnManager.updateSubscriptionInfo(context, newName, newUrl, refreshNow, editing.id) { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showQrDialog) {
        QrScanDialog(
            onDismiss = { showQrDialog = false },
            onConfirm = { clip ->
                VpnManager.importClipboardOrUrl(context, clip) { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                showQrDialog = false
            }
        )
    }
}

// Подключение и список профилей.
@Composable
fun ConnectionScreen(
    status: VpnStatus,
    durationSec: Long?,
    servers: List<ServerProfile>,
    selectedServer: ServerProfile?,
    subscription: SubscriptionInfo?,
    onConnectToggle: () -> Unit,
    onSelectServer: (ServerProfile) -> Unit,
    onDeleteServer: (ServerProfile) -> Unit,
    onEditServer: (ServerProfile) -> Unit,
    onPingServer: (ServerProfile) -> Unit,
    onPingAll: () -> Unit,
    onAddServerClick: () -> Unit,
    onPasteClipboard: () -> Unit,
    onQrScanClick: () -> Unit,
    onUpdateSubscription: () -> Unit,
    onEditSubscription: () -> Unit,
    onClearSubscription: () -> Unit,
    onCopyLink: (ServerProfile) -> Unit,
    subscriptions: List<SubscriptionInfo> = listOfNotNull(subscription),
    onFocusSubscription: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Row with Title and Small Plus Menu (at the very top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Atlant",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AtlantTextPrimary,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = tr("Личное пространство в сети"),
                    fontSize = 11.sp,
                    color = AtlantTextMuted
                )
            }

            // Small Plus Button in Top Right with Dropdown Menu
            Box {
                var showAddMenu by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { showAddMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AtlantSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = tr("Добавить"),
                        tint = AtlantTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(tr("Вставить из буфера"), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showAddMenu = false
                            onPasteClipboard()
                        }
                    )
                        DropdownMenuItem(
                            text = { Text(tr("Добавить вручную"), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showAddMenu = false
                                onAddServerClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(tr("Сканировать QR-код"), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showAddMenu = false
                                onQrScanClick()
                            }
                        )
                    }
                }
            }

        // Center Connect Button + Time Under Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
        ) {
            // Elegant Soft Grey Outlined White Disc Button
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .shadow(
                        elevation = if (status == VpnStatus.CONNECTED) 3.dp else 1.5.dp,
                        shape = CircleShape,
                        spotColor = Color(0x18000000)
                    )
                    .clip(CircleShape)
                    .background(AtlantSurface)
                    .border(
                        width = if (status == VpnStatus.CONNECTED) 2.dp else 1.5.dp,
                        color = when (status) {
                            VpnStatus.CONNECTED -> AtlantPrimary
                            VpnStatus.CONNECTING -> AtlantConnectingOrange
                            VpnStatus.DISCONNECTED -> AtlantCardBorder
                        },
                        shape = CircleShape
                    )
                    .clickable(role = Role.Button) { onConnectToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (status == VpnStatus.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = AtlantTextSecondary,
                        strokeWidth = 2.dp,
                        trackColor = AtlantCardBorder
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = if (status == VpnStatus.CONNECTED) tr("Отключить VPN") else tr("Подключить VPN"),
                            tint = if (status == VpnStatus.CONNECTED) AtlantPrimary else AtlantTextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (status == VpnStatus.CONNECTED) tr("ОТКЛЮЧИТЬ") else tr("ПОДКЛЮЧИТЬ"),
                            color = if (status == VpnStatus.CONNECTED) AtlantPrimary else AtlantTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Connection Time Directly Under Button (Soft White-Grey Typography)
            if (status == VpnStatus.CONNECTED) {
                Text(tr("Туннель запущен"), color = AtlantPrimary, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                ConnectionDurationText(durationSec)
            } else {
                Text(
                    text = if (status == VpnStatus.CONNECTING) tr("Устанавливаем соединение…") else tr("Соединение отключено"),
                    fontSize = 11.5.sp,
                    color = AtlantTextMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }


        selectedServer?.let { profile ->
            Text(
                text = profile.name,
                color = AtlantTextSecondary, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        // Servers Section Header: Title & Ping All Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Серверы ({0})", servers.size),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AtlantTextPrimary
            )

            IconButton(
                onClick = onPingAll,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AtlantSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = tr("Пинг всех"),
                    tint = AtlantTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        SubscriptionServerList(
            servers = servers, selectedServer = selectedServer, subscription = subscription,
            subscriptions = subscriptions, onFocusSubscription = onFocusSubscription,
            onSelectServer = onSelectServer, onDeleteServer = onDeleteServer,
            onEditServer = onEditServer, onPingServer = onPingServer, onCopyLink = onCopyLink,
            onUpdateSubscription = onUpdateSubscription, onEditSubscription = onEditSubscription,
            onClearSubscription = onClearSubscription,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}


@Composable
fun ServerItemCard(
    server: ServerProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPing: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AtlantSurfaceVariant else AtlantSurface
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AtlantSurfaceVariant else AtlantSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Active", tint = AtlantTextPrimary, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.Dns, contentDescription = "Server", tint = AtlantTextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AtlantTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = server.server,
                        fontSize = 11.sp,
                        color = AtlantTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPing, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(
                        text = if (server.pingMs > 0) tr("{0} мс", server.pingMs) else tr("Пинг"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (server.pingMs > 0 && server.pingMs < 100) AtlantPrimary else AtlantTextSecondary
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = tr("Действия сервера {0}", server.name), tint = AtlantTextSecondary, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text(tr("Редактировать")) }, onClick = { menuExpanded = false; onEdit() })
                        DropdownMenuItem(text = { Text(tr("Копировать ссылку")) }, onClick = { menuExpanded = false; onCopy() })
                        if (!server.isSubscription) {
                            DropdownMenuItem(text = { Text(tr("Удалить сервер"), color = AtlantDanger) }, onClick = { menuExpanded = false; onDelete() })
                        }
                    }
                }
            }
        }
    }
}

// Журнал работы ядра.
@Composable
fun LogsSubScreen(onBack: () -> Unit, onClear: () -> Unit, onCopy: () -> Unit) {
    val logs by VpnManager.logs.collectAsStateWithLifecycle()
    LogsContent(logs, onBack, onClear, onCopy)
}

// Настройки приложения.
@Composable
fun ModeChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AtlantPrimary else AtlantSurfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else AtlantTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Ручной ввод ссылки; камера реализована отдельно в CameraQrScanner.kt.
@Composable
fun PasteLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var qrText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = AtlantPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Вставить ссылку"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr("Вставьте ссылку или данные из QR-кода (atlant:// или подписку https://):"),
                    fontSize = 12.sp,
                    color = AtlantTextMuted
                )

                OutlinedTextField(
                    value = qrText,
                    onValueChange = { qrText = it },
                    label = { Text(tr("Содержимое QR-кода / URL")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val clip = clipboardManager.getText()?.text ?: ""
                            if (clip.isNotBlank()) {
                                qrText = clip.trim()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AtlantSurfaceVariant, contentColor = AtlantTextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Из буфера"), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (qrText.isNotBlank()) {
                        onConfirm(qrText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AtlantPrimary)
            ) {
                Text(tr("Импортировать"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Отмена"))
            }
        }
    )
}

// Добавление профиля.
@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (ServerProfile) -> Unit
) {
    var name by remember { mutableStateOf("Atlant Server") }
    var server by remember { mutableStateOf("") }
    var sni by remember { mutableStateOf("www.microsoft.com") }
    var user by remember { mutableStateOf("") }
    var pbk by remember { mutableStateOf("") }
    var serverPubKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Добавить сервер Atlant"), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Имя сервера")) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = server, onValueChange = { server = it }, label = { Text(tr("Адрес (IP:Порт)")) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sni, onValueChange = { sni = it }, label = { Text(tr("SNI Домен")) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text(tr("Пользователь (Username)")) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pbk, onValueChange = { pbk = it }, label = { Text(tr("Ключ PBK (64 hex)")) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = serverPubKey, onValueChange = { serverPubKey = it }, label = { Text(tr("Статический ключ сервера")) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (server.isNotBlank() && user.isNotBlank() && pbk.isNotBlank()) {
                        val profile = ServerProfile(
                            id = "srv-" + System.currentTimeMillis(),
                            name = name.ifBlank { "Atlant ($user)" },
                            server = server.trim(),
                            sni = sni.trim(),
                            pbk = pbk.trim(),
                            user = user.trim(),
                            serverPubKey = serverPubKey.trim(),
                            transport = "REALITY",
                            pingMs = -1
                        )
                        onConfirm(profile)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AtlantPrimary)
            ) {
                Text(tr("Добавить"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Отмена"))
            }
        }
    )
}

// Редактор профиля: поля формы и JSON представляют одни настройки.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditServerScreen(
    server: ServerProfile,
    onBack: () -> Unit,
    onConfirm: (ServerProfile) -> Unit,
    onDelete: () -> Unit,
    onPing: (ServerProfile) -> Unit,
    onCopyLink: (ServerProfile) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    var selectedMode by remember { mutableIntStateOf(0) } // 0 = Ручное (Параметры), 1 = JSON
    var name by remember { mutableStateOf(server.name) }
    var serverAddress by remember { mutableStateOf(server.server) }
    var sni by remember { mutableStateOf(server.sni) }
    var user by remember { mutableStateOf(server.user) }
    var pbk by remember { mutableStateOf(server.pbk) }
    var serverPubKey by remember { mutableStateOf(server.serverPubKey) }
    var transport by remember { mutableStateOf(if (server.transport.equals("xhttp", ignoreCase = true)) "xhttp" else "tcp") }
    var group by remember { mutableStateOf(server.group) }
    var fingerprint by remember { mutableStateOf(normalizeTlsProfile(server.fingerprint)) }
    var currentPing by remember { mutableIntStateOf(server.pingMs) }
    var isPinging by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun buildJson(): String {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("server", serverAddress)
        obj.put("sni", sni)
        obj.put("user", user)
        obj.put("pbk", pbk)
        obj.put("server_pub_key", serverPubKey)
        obj.put("security", "reality")
        obj.put("transport", if (transport.equals("xhttp", ignoreCase = true)) "xhttp" else "tcp")
        if (fingerprint.isNotBlank()) {
            obj.put("fingerprint", fingerprint)
        }
        if (group.isNotBlank()) {
            obj.put("group", group)
        }
        return obj.toString(2)
    }

    var jsonContent by remember { mutableStateOf(buildJson()) }
    var jsonParseError by remember { mutableStateOf<String?>(null) }

    fun tryParseJson(text: String): Boolean {
        return try {
            val obj = JSONObject(text)
            name = obj.optString("name", name)
            serverAddress = obj.optString("server", serverAddress)
            sni = obj.optString("sni", sni)
            user = obj.optString("user", user)
            pbk = obj.optString("pbk", pbk)
            serverPubKey = if (obj.has("server_pub_key")) obj.getString("server_pub_key")
                           else obj.optString("serverPubKey", serverPubKey)
            val trRaw = if (obj.has("transport")) obj.getString("transport") else obj.optString("tcp_proto", transport)
            transport = if (trRaw.equals("xhttp", ignoreCase = true)) "xhttp" else "tcp"
            group = obj.optString("group", group)
            fingerprint = obj.optString("fingerprint", obj.optString("fp", fingerprint))
            jsonParseError = null
            true
        } catch (e: Exception) {
            jsonParseError = e.message ?: tr("Ошибка синтаксиса JSON")
            false
        }
    }

    fun handleSave() {
        if (selectedMode == 1) {
            val ok = tryParseJson(jsonContent)
            if (!ok) {
                Toast.makeText(context, tr("Исправьте синтаксис JSON перед сохранением!"), Toast.LENGTH_LONG).show()
                return
            }
        }
        val trimmedAddress = serverAddress.trim()
        if (trimmedAddress.isBlank()) {
            Toast.makeText(context, tr("Адрес сервера (IP:Порт) не может быть пустым!"), Toast.LENGTH_SHORT).show()
            return
        }
        val updated = server.copy(
            name = name.ifBlank { trimmedAddress },
            server = trimmedAddress,
            sni = sni.trim(),
            user = user.trim(),
            pbk = pbk.trim(),
            serverPubKey = serverPubKey.trim(),
            transport = if (transport.equals("xhttp", ignoreCase = true)) "xhttp" else "tcp",
            group = group.trim(),
            fingerprint = fingerprint.trim(),
            pingMs = currentPing
        )
        onConfirm(updated)
    }

    fun executePing() {
        coroutineScope.launch {
            isPinging = true
            val parts = serverAddress.split(":")
            val host = parts.getOrNull(0) ?: ""
            val port = parts.getOrNull(1)?.toIntOrNull() ?: 443
            val start = System.currentTimeMillis()
            var success = false
            withContext(Dispatchers.IO) {
                try {
                    java.net.Socket().use { s ->
                        s.connect(java.net.InetSocketAddress(host, port), 2500)
                        success = true
                    }
                } catch (_: Exception) {}
            }
            currentPing = if (success) (System.currentTimeMillis() - start).toInt() else 999
            isPinging = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AtlantBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar: Back Button, Title, and Save Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AtlantSurface)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = tr("Назад"),
                        tint = AtlantTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = tr("Редактирование сервера"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = AtlantTextPrimary
                    )
                    Text(
                        text = if (name.isNotBlank()) name else serverAddress,
                        fontSize = 12.sp,
                        color = AtlantTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = { handleSave() },
                colors = ButtonDefaults.buttonColors(containerColor = AtlantPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Сохранить"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        HorizontalDivider(color = AtlantCardBorder)

        // Mode Switcher (Два режима: Параметры / JSON)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(12.dp),
            color = AtlantCardBorder
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                // Tab 0: Manual Form
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selectedMode == 0) AtlantSurface else Color.Transparent)
                        .clickable {
                            if (selectedMode == 1) {
                                if (tryParseJson(jsonContent)) {
                                    selectedMode = 0
                                } else {
                                    Toast.makeText(context, tr("В JSON есть ошибка. Исправьте её перед переходом."), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = tr("Параметры"),
                            tint = if (selectedMode == 0) AtlantPrimary else AtlantTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Параметры (Ручное)"),
                            fontWeight = if (selectedMode == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = if (selectedMode == 0) AtlantPrimary else AtlantTextSecondary
                        )
                    }
                }

                // Tab 1: JSON Editor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selectedMode == 1) AtlantSurface else Color.Transparent)
                        .clickable {
                            if (selectedMode == 0) {
                                jsonContent = buildJson()
                                try {
                                    JSONObject(jsonContent)
                                    jsonParseError = null
                                } catch (e: Exception) {
                                    jsonParseError = e.message
                                }
                                selectedMode = 1
                            }
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "JSON",
                            tint = if (selectedMode == 1) AtlantPrimary else AtlantTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("JSON редактор"),
                            fontWeight = if (selectedMode == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = if (selectedMode == 1) AtlantPrimary else AtlantTextSecondary
                        )
                    }
                }
            }
        }

        // Content
        if (selectedMode == 0) {
            // Редактирование через поля формы.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Основные параметры
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AtlantSurface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = tr("ОСНОВНЫЕ ПАРАМЕТРЫ"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AtlantTextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(tr("Имя сервера")) },
                            placeholder = { Text(tr("Например, Мой VPS")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = serverAddress,
                            onValueChange = { serverAddress = it },
                            label = { Text(tr("Адрес (IP:Порт)")) },
                            placeholder = { Text(tr("например: 1.2.3.4:443")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = sni,
                            onValueChange = { sni = it },
                            label = { Text(tr("SNI Домен (маскировка)")) },
                            placeholder = { Text(tr("например: www.microsoft.com")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                // Card 2: Аутентификация и ключи
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AtlantSurface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = tr("АУТЕНТИФИКАЦИЯ И КЛЮЧИ"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AtlantTextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = user,
                            onValueChange = { user = it },
                            label = { Text(tr("Пользователь (Username)")) },
                            placeholder = { Text(tr("например: user1")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pbk,
                            onValueChange = { pbk = it },
                            label = { Text(tr("Ключ PBK (64 hex)")) },
                            placeholder = { Text(tr("Публичный ключ REALITY")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (pbk.isNotEmpty()) {
                                    IconButton(onClick = { pbk = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = tr("Очистить"), tint = AtlantTextMuted, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(onClick = {
                                        val clip = clipboardManager.getText()?.text ?: ""
                                        if (clip.isNotBlank()) pbk = clip.trim()
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = tr("Вставить"), tint = AtlantPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = serverPubKey,
                            onValueChange = { serverPubKey = it },
                            label = { Text(tr("Статический ключ сервера (server_pub_key)")) },
                            placeholder = { Text(tr("Статический ключ сервера")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (serverPubKey.isNotEmpty()) {
                                    IconButton(onClick = { serverPubKey = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = tr("Очистить"), tint = AtlantTextMuted, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(onClick = {
                                        val clip = clipboardManager.getText()?.text ?: ""
                                        if (clip.isNotBlank()) serverPubKey = clip.trim()
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = tr("Вставить"), tint = AtlantPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    }
                }

                // Card 3: Защита REALITY и Транспорт
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AtlantSurface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = tr("ЗАЩИТА И ТРАНСПОРТ"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AtlantTextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Reality Security Status Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AtlantBadgeBg)
                                .border(1.dp, AtlantCardBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AtlantPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tr("Защита: REALITY TLS 1.3"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AtlantPrimary)
                                Text(tr("TLS-маскировка под SNI домен и ключи PBK. Защита работает для всех транспортов."), fontSize = 11.sp, color = AtlantPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = tr("TCP, XHTTP и QUIC выбираются в настройках клиента. Создавать отдельный профиль для каждого транспорта не нужно."),
                            fontSize = 11.sp,
                            color = AtlantTextMuted,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(tr("TLS-профиль:"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtlantTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "" to tr("По умолч."),
                                "chrome" to "Chrome 144",
                                "chrome_120" to tr("Chrome 120 (совместимость)"),
                                "firefox" to "Firefox 120",
                                "safari" to "Safari 16",
                                "ios" to "iOS 14",
                                "edge" to "Edge 106"
                            ).forEach { (valKey, label) ->
                                val isSel = fingerprint.equals(valKey, ignoreCase = true)
                                ModeChip(text = label, isSelected = isSel) {
                                    fingerprint = valKey
                                }
                            }
                        }

                        Text(
                            tr("Профиль задаёт только ClientHello. Android/OkHttp несовместим с REALITY; случайный профиль не имитирует браузер. Импортированное значение: {0}.", fingerprint.ifBlank { "по умолчанию" }),
                            fontSize = 11.sp,
                            color = AtlantTextMuted,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = group,
                            onValueChange = { group = it },
                            label = { Text(tr("Группа / Категория")) },
                            placeholder = { Text(tr("например: Личный VPS или Подписка")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                // Card 4: Действия
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AtlantSurface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = tr("ДЕЙСТВИЯ"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AtlantTextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Ping Test Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AtlantSurfaceVariant)
                                .clickable { executePing() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SignalCellularAlt,
                                    contentDescription = "Ping",
                                    tint = AtlantPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(tr("Проверить задержку (Ping)"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(tr("Проверка доступности TCP сокета"), fontSize = 11.sp, color = AtlantTextMuted)
                                }
                            }
                            if (isPinging) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AtlantPrimary)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (currentPing > 0 && currentPing < 200) AtlantBadgeBg else AtlantCardBorder)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (currentPing > 0) "$currentPing ms" else tr("Тест"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentPing > 0 && currentPing < 200) AtlantPrimary else AtlantTextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Copy atlant:// link Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AtlantSurfaceVariant)
                                .clickable {
                                    val currentProfile = server.copy(
                                        name = name.ifBlank { serverAddress },
                                        server = serverAddress.trim(),
                                        sni = sni.trim(),
                                        user = user.trim(),
                                        pbk = pbk.trim(),
                                        serverPubKey = serverPubKey.trim(),
                                        transport = transport.trim(),
                                        group = group.trim()
                                    )
                                    onCopyLink(currentProfile)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = tr("Копировать"),
                                tint = AtlantPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tr("Скопировать ссылку atlant://"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(tr("Для экспорта и передачи конфигурации"), fontSize = 11.sp, color = AtlantTextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Delete Server Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AtlantDangerSurface)
                                .clickable { showDeleteConfirm = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = tr("Удалить"),
                                tint = AtlantDanger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tr("Удалить этот сервер"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AtlantDanger)
                                Text(tr("Сервер будет удален из списка"), fontSize = 11.sp, color = AtlantDanger.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            // Редактирование JSON.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action Buttons Row: Format, Copy, Paste, Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Format
                    Button(
                        onClick = {
                            try {
                                val obj = JSONObject(jsonContent)
                                jsonContent = obj.toString(2)
                                jsonParseError = null
                                Toast.makeText(context, tr("JSON отформатирован"), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, tr("Ошибка в JSON: {0}", e.message), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AtlantSurface),
                        border = BorderStroke(1.dp, AtlantCardBorder),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AtlantPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Формат"), fontSize = 11.5.sp, color = AtlantTextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    // Copy
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonContent))
                            Toast.makeText(context, tr("JSON скопирован"), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AtlantSurface),
                        border = BorderStroke(1.dp, AtlantCardBorder),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AtlantPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Копия"), fontSize = 11.5.sp, color = AtlantTextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    // Paste
                    Button(
                        onClick = {
                            val clip = clipboardManager.getText()?.text ?: ""
                            if (clip.isNotBlank()) {
                                jsonContent = clip
                                try {
                                    JSONObject(clip)
                                    jsonParseError = null
                                    Toast.makeText(context, tr("Вставлено из буфера"), Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    jsonParseError = e.message
                                }
                            } else {
                                Toast.makeText(context, tr("Буфер пуст"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AtlantSurface),
                        border = BorderStroke(1.dp, AtlantCardBorder),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = AtlantPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Вставить"), fontSize = 11.5.sp, color = AtlantTextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    // Reset from form
                    Button(
                        onClick = {
                            jsonContent = buildJson()
                            jsonParseError = null
                            Toast.makeText(context, tr("Сброшено к текущим параметрам"), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AtlantSurface),
                        border = BorderStroke(1.dp, AtlantCardBorder),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = AtlantPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Сброс"), fontSize = 11.5.sp, color = AtlantTextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Status Banner
                if (jsonParseError == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AtlantBadgeBg)
                            .border(1.dp, AtlantCardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AtlantPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("JSON валиден и готов к сохранению"), fontSize = 12.sp, color = AtlantPrimary, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AtlantDangerSurface)
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AtlantDanger, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Ошибка JSON: {0}", jsonParseError), fontSize = 12.sp, color = AtlantDanger, fontWeight = FontWeight.Medium)
                    }
                }

                // Dark Code Editor Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "server_config.json",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = tr("{0} строк", jsonContent.lines().size),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        OutlinedTextField(
                            value = jsonContent,
                            onValueChange = {
                                jsonContent = it
                                try {
                                    JSONObject(it)
                                    jsonParseError = null
                                } catch (e: Exception) {
                                    jsonParseError = e.message
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp, max = 500.dp),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFFF8FAFC),
                                lineHeight = 18.sp
                            ),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF161B22),
                                unfocusedContainerColor = Color(0xFF161B22),
                                focusedBorderColor = AtlantPrimary,
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = AtlantPrimary
                            )
                        )
                    }
                }

                // Field schema documentation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AtlantSurface),
                    border = BorderStroke(1.dp, AtlantCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(tr("Справка по полям JSON:"), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AtlantTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr("• \"name\" — имя сервера\n") +
                                   tr("• \"server\" — адрес host:port (обязательно)\n") +
                                   tr("• \"sni\" — SNI маскировка REALITY\n") +
                                   tr("• \"user\" — логин / пользователь\n") +
                                   tr("• \"pbk\" — публичный ключ REALITY\n") +
                                   tr("• \"server_pub_key\" — ключ сервера\n") +
                                   tr("• \"security\" — reality (защита TLS 1.3)\n") +
                                   tr("• \"transport\" — tcp (прямой) или xhttp (HTTP/2 поверх REALITY)\n") +
                                   tr("• \"group\" — группа серверов"),
                            fontSize = 11.sp,
                            color = AtlantTextMuted,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(tr("Удалить сервер?"), fontWeight = FontWeight.Bold) },
            text = { Text(tr("Вы действительно хотите удалить сервер '{0}'?", name.ifBlank { serverAddress })) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(tr("Удалить"), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(tr("Отмена"))
                }
            }
        )
    }
}

// Настройки подписки.
@Composable
fun EditSubscriptionDialog(
    subscription: SubscriptionInfo,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newUrl: String, refreshNow: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(subscription.name) }
    var url by remember { mutableStateOf(subscription.url) }
    var refreshNow by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Настройки подписки"), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Название подписки")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(tr("Ссылка на подписку (URL)")) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { refreshNow = !refreshNow },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = refreshNow,
                        onCheckedChange = { refreshNow = it },
                        colors = CheckboxDefaults.colors(checkedColor = AtlantPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Обновить серверы сразу после сохранения"), fontSize = 12.sp, color = AtlantTextPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onConfirm(name.ifBlank { tr("Подписка") }, url.trim(), refreshNow)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AtlantPrimary)
            ) {
                Text(tr("Сохранить"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Отмена"))
            }
        }
    )
}

fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, secs)
}
