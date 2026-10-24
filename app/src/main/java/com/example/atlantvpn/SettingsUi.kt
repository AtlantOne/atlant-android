package com.example.atlantvpn

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.atlantvpn.ui.theme.*

internal enum class SettingsPage(val title: String, val description: String) {
    HOME("Настройки", "Изменения сохраняются автоматически"),
    LANGUAGE("Язык", "Язык интерфейса"),
    APPEARANCE("Оформление", "Светлая, тёмная или системная тема"),
    NETWORK("Подключение", "DNS, MTU и параметры Android VPN"),
    TRANSPORT("Транспорт", "TCP, QUIC, XHTTP и повторные подключения"),
    PRIVACY("Маскировка трафика", "TLS-профиль и дополнительные пакеты"),
    DIAGNOSTICS("Диагностика", "Журнал работы и уровень записи")
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    routing: RoutingSettings,
    onSaveSettings: (AppSettings) -> Unit,
    onSaveRouting: (RoutingSettings) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val apps by VpnManager.installedApps.collectAsState()
    val loading by VpnManager.isLoadingApps.collectAsState()
    var destination by rememberSaveable { mutableStateOf("HOME") }
    val back = { destination = if (destination == "LOGS") "DIAGNOSTICS" else "HOME" }
    BackHandler(destination != "HOME", onBack = back)
    when (destination) {
        "ROUTING" -> RoutingSubScreen(routing, apps, loading, back, onSaveRouting)
        "LOGS" -> LogsSubScreen(back, { VpnManager.clearLogs() }, {
            clipboard.setText(AnnotatedString(VpnManager.getLogsFormatted()))
            Toast.makeText(context, tr("Журнал скопирован"), Toast.LENGTH_SHORT).show()
        })
        else -> SettingsContent(
            settings, routing, SettingsPage.valueOf(destination), onSaveSettings,
            { destination = it }, back,
            { context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
        )
    }
}

// Stateless content also renders in previews without initializing the VPN engine.
@Composable
internal fun SettingsContent(
    settings: AppSettings,
    routing: RoutingSettings,
    page: SettingsPage,
    onSave: (AppSettings) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onAndroidVpn: () -> Unit
) {
    Column(Modifier.fillMaxSize().imePadding()) {
        SettingsHeader(tr(page.title), if (page == SettingsPage.HOME) null else onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Hint(if (page == SettingsPage.HOME) tr(page.description) else tr("Сохраняется автоматически. Сетевые параметры применятся после переподключения.")) }
            when (page) {
                SettingsPage.HOME -> {
                    item { SettingsSection(tr("Подключение и трафик")) {
                        NavigationSetting(tr("Подключение"), "DNS: ${settings.dns} · MTU: ${settings.mtu}") { onNavigate("NETWORK") }
                        NavigationSetting(tr("Транспорт"), tr("TCP, QUIC и XHTTP")) { onNavigate("TRANSPORT") }
                        NavigationSetting(tr("Маршрутизация"), routingSummary(routing)) { onNavigate("ROUTING") }
                    } }
                    item { SettingsSection(tr("Приложение")) {
                        NavigationSetting(tr("Язык"), tr("English · Русский · 简体中文")) { onNavigate("LANGUAGE") }
                        NavigationSetting(tr("Оформление"), themeLabel(settings.themeMode)) { onNavigate("APPEARANCE") }
                        NavigationSetting(tr("Маскировка трафика"), tr("TLS-профиль, заполнение пакетов")) { onNavigate("PRIVACY") }
                        NavigationSetting(tr("Диагностика"), tr("Журнал и сообщения об ошибках")) { onNavigate("DIAGNOSTICS") }
                    } }
                }
                SettingsPage.LANGUAGE -> item {
                    val context = LocalContext.current
                    SettingsSection(tr("Язык интерфейса")) {
                        val current = AppLanguage.selected(context)
                        listOf("system" to tr("Как в системе"), "en" to "English", "ru" to tr("Русский"), "zh" to "简体中文").forEach { (tag, label) ->
                            ChoiceRow(label, "", current == tag) {
                                if (current != tag) {
                                    AppLanguage.save(context, tag)
                                    var owner: android.content.Context = context
                                    while (owner is android.content.ContextWrapper && owner !is android.app.Activity) owner = owner.baseContext
                                    (owner as? android.app.Activity)?.recreate()
                                }
                            }
                        }
                    }
                }
                SettingsPage.APPEARANCE -> item { SettingsSection(tr("Тема приложения")) {
                    ThemeMode.entries.forEach { mode ->
                        ChoiceRow(themeLabel(mode), when (mode) {
                            ThemeMode.SYSTEM -> tr("Следовать настройкам телефона")
                            ThemeMode.LIGHT -> tr("Светлый фон")
                            ThemeMode.DARK -> tr("Тёмный фон")
                        }, settings.themeMode == mode) { onSave(settings.copy(themeMode = mode)) }
                    }
                    Hint(tr("Тема меняется сразу и не прерывает подключение."))
                } }
                SettingsPage.NETWORK -> {
                    item { SettingsSection(tr("Параметры сети")) {
                        MtuSetting(settings, onSave)
                        var dns by remember(settings.dns) { mutableStateOf(settings.dns) }
                        OutlinedTextField(
                            value = dns,
                            onValueChange = { dns = it; if (it.isNotBlank()) onSave(settings.copy(dns = it.trim())) },
                            label = { Text(tr("DNS-сервер")) },
                            supportingText = { Text(tr("Адрес DNS-сервера, например 1.1.1.1")) },
                            singleLine = true, isError = dns.isBlank(), modifier = Modifier.fillMaxWidth()
                        )
                        ToggleSetting(tr("DNS через VPN"), tr("При выключении DNS-запросы идут напрямую, FakeDNS не используется."), settings.remoteDns) { onSave(settings.copy(remoteDns = it)) }
                        ToggleSetting("FakeDNS", tr("Подставлять локальные адреса для определения доменов. Требуется DNS через VPN."), settings.fakeDns) { onSave(settings.copy(fakeDns = it)) }
                        ToggleSetting(tr("Блокировать IPv6"), tr("Не использовать IPv6. При выключении IPv6 пойдёт через VPN — сервер должен его поддерживать."), settings.blockIPv6) { onSave(settings.copy(blockIPv6 = it)) }
                    } }
                    item { SettingsSection(tr("Защита при отключении")) {
                        Hint(tr("Блокировку интернета без VPN включает Android. Откройте настройки Atlant и включите постоянный VPN и блокировку соединений без VPN."))
                        OutlinedButton(onClick = onAndroidVpn, modifier = Modifier.fillMaxWidth()) { Text(tr("Открыть настройки VPN")) }
                    } }
                }
                SettingsPage.TRANSPORT -> {
                    item { SettingsSection(tr("Как передавать трафик")) {
                        ValueSetting(tr("Режим"), settings.trafficMode.name, listOf("SPLIT" to tr("Раздельно для TCP и UDP"), "TCP" to tr("Весь трафик через TCP"), "UDP" to tr("Весь трафик через QUIC"))) {
                            onSave(settings.copy(trafficMode = TrafficMode.valueOf(it)))
                        }
                        if (settings.trafficMode != TrafficMode.UDP) {
                            ValueSetting(tr("Транспорт TCP"), settings.tcpProto, listOf("reality" to "REALITY / TCP", "xhttp" to "XHTTP / REALITY", "udp" to tr("По профилю (старое значение)"))) { onSave(settings.copy(tcpProto = it)) }
                            Hint(tr("Транспорт выбирается здесь, а не в профиле сервера."))
                        }
                        if (settings.trafficMode == TrafficMode.SPLIT) {
                            ValueSetting(tr("Передача UDP-трафика"), settings.udpProto, listOf("tcp" to tr("Через TCP"), "quic" to tr("Через QUIC"))) { onSave(settings.copy(udpProto = it)) }
                        }
                        ToggleSetting(tr("Общее TCP-соединение"), tr("Объединять потоки в одну сессию Yamux (Mux)."), settings.mux) { onSave(settings.copy(mux = it)) }
                    } }
                    item { SettingsSection(tr("Повторные подключения")) {
                        ToggleSetting(tr("Резервное подключение через TCP"), tr("Пробовать TCP, если подключиться через QUIC не удалось."), settings.autoFallback) { onSave(settings.copy(autoFallback = it)) }
                        ToggleSetting(tr("Кэш QUIC-сессий"), tr("Использовать сохранённую TLS-сессию при повторном подключении."), settings.zeroRtt) { onSave(settings.copy(zeroRtt = it)) }
                        ToggleSetting(tr("Смена порта сервера"), tr("Экспериментально: выбирать другой порт каждые 2 минуты. Требуется поддержка на сервере, иначе подключение не установится."), settings.portHopping) { onSave(settings.copy(portHopping = it)) }
                    } }
                }
                SettingsPage.PRIVACY -> item { SettingsSection(tr("Параметры маскировки")) {
                    ValueSetting(tr("TLS-профиль"), normalizeTlsProfile(settings.fingerprint), listOf("chrome" to "Chrome 144", "chrome_120" to tr("Chrome 120 (совместимость)"), "firefox" to "Firefox 120", "safari" to "Safari 16", "ios" to "iOS 14", "edge" to "Edge 106")) { onSave(settings.copy(fingerprint = it)) }
                    Hint(tr("Профиль задаёт только ClientHello, а не поведение всего браузера. Старый профиль Android/OkHttp несовместим с REALITY: выберите один из списка. Случайный профиль не имитирует браузер."))
                    Hint(tr("Шаблон TLS ClientHello. В профиле сервера может быть указан отдельный шаблон."))
                    ToggleSetting(tr("Заполнение пакетов"), tr("Добавлять данные для изменения размеров зашифрованных блоков (Padding)."), settings.trafficPadding) { onSave(settings.copy(trafficPadding = it)) }
                    ToggleSetting(tr("Фоновые запросы"), tr("Отправлять дополнительные запросы к SNI-узлу. Увеличивает расход трафика."), settings.decoyTraffic) { onSave(settings.copy(decoyTraffic = it)) }
                    Hint(tr("Эти настройки не гарантируют нераспознаваемость VPN. Отключение заполнения меняет характеристики трафика."))
                } }
                SettingsPage.DIAGNOSTICS -> item { SettingsSection(tr("Журнал")) {
                    ValueSetting(tr("Что записывать"), settings.logLevel, listOf("off" to tr("Не записывать"), "error" to tr("Только ошибки"), "warn" to tr("Ошибки и предупреждения"), "info" to tr("Основные события"), "debug" to tr("Подробный журнал"))) { onSave(settings.copy(logLevel = it)) }
                    Hint(tr("Подробный журнал может содержать адреса подключений. Проверяйте его перед отправкой другим людям."))
                    NavigationSetting(tr("Открыть журнал"), tr("Просмотреть, скопировать или очистить записи")) { onNavigate("LOGS") }
                } }
            }
        }
    }
}

@Composable
private fun MtuSetting(settings: AppSettings, onSave: (AppSettings) -> Unit) {
    var text by remember(settings.mtu) { mutableStateOf(settings.mtu.toString()) }
    val mtu = validMtu(text, !settings.blockIPv6)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; validMtu(it, !settings.blockIPv6)?.let { value -> onSave(settings.copy(mtu = value)) } },
        label = { Text("MTU") }, singleLine = true, isError = mtu == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = { Text(tr("Размер пакета: {0}–1500 байт. Обычно 1360. Некорректное значение не сохраняется.", if (settings.blockIPv6) 1200 else 1280)) },
        modifier = Modifier.fillMaxWidth()
    )
}

internal fun validMtu(text: String, ipv6: Boolean): Int? = text.toIntOrNull()?.takeIf { it in (if (ipv6) 1280 else 1200)..1500 }
internal fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> tr("Как в системе")
    ThemeMode.LIGHT -> tr("Светлая")
    ThemeMode.DARK -> tr("Тёмная")
}
internal fun routingSummary(routing: RoutingSettings): String = when {
    !routing.enabled -> tr("Правила выключены · всё через VPN")
    routing.mode == RouteMode.ALL -> tr("Всё через VPN")
    routing.mode == RouteMode.RULES -> tr("Только по правилам")
    else -> tr("Через VPN, кроме исключений")
}

@Composable
fun RoutingSubScreen(
    routing: RoutingSettings,
    installedApps: List<InstalledAppInfo>,
    isLoadingApps: Boolean,
    onBack: () -> Unit,
    onSave: (RoutingSettings) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf("domains") }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(installedApps, query) { installedApps.filter { it.appName.contains(query, true) || it.packageName.contains(query, true) } }
    val selected = if (routing.mode == RouteMode.RULES) routing.proxyApps else routing.bypassApps
    fun saveApps(next: Set<String>) = onSave(if (routing.mode == RouteMode.RULES) routing.copy(proxyApps = next) else routing.copy(bypassApps = next))
    Column(Modifier.fillMaxSize().imePadding()) {
        SettingsHeader(tr("Маршрутизация"), onBack)
        // One scrolling surface: controls and domain editors cannot squeeze the app list off screen.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Hint(tr("Правила сохраняются автоматически. После изменения режима или списка приложений переподключите VPN.")) }
            item { SettingsSection(tr("Куда направлять трафик")) {
                ToggleSetting(tr("Использовать правила"), routingSummary(routing), routing.enabled) { onSave(routing.copy(enabled = it)) }
                if (routing.enabled) {
                    ChoiceRow(tr("Всё через VPN"), tr("Без исключений по приложениям и адресам"), routing.mode == RouteMode.ALL) { onSave(routing.copy(mode = RouteMode.ALL)) }
                    ChoiceRow(tr("Только по правилам"), tr("Совпавшие адреса через VPN, остальные напрямую"), routing.mode == RouteMode.RULES) { onSave(routing.copy(mode = RouteMode.RULES)) }
                    ChoiceRow(tr("Кроме исключений"), tr("Выбранные приложения и адреса напрямую, остальные через VPN"), routing.mode == RouteMode.BYPASS) { onSave(routing.copy(mode = RouteMode.BYPASS)) }
                }
            } }
            if (routing.enabled && routing.mode != RouteMode.ALL) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = tab == "domains", onClick = { tab = "domains" }, label = { Text(tr("Адреса")) })
                        FilterChip(selected = tab == "apps", onClick = { tab = "apps" }, label = { Text(tr("Приложения")) })
                    }
                }
                if (tab == "domains") {
                    item { SettingsSection(if (routing.mode == RouteMode.RULES) tr("Адреса через VPN") else tr("Адреса без VPN")) {
                        Hint(tr("Домен, IP-адрес или подсеть — по одному на строку. Домен включает поддомены. Для всех сайтов .ru: *.ru. Например: example.com или 192.168.0.0/16."))
                        val text = if (routing.mode == RouteMode.RULES) routing.proxyDomains else routing.directDomains
                        OutlinedTextField(
                            value = text,
                            onValueChange = { onSave(if (routing.mode == RouteMode.RULES) routing.copy(proxyDomains = it) else routing.copy(directDomains = it)) },
                            label = { Text(tr("Список адресов")) }, minLines = 5, maxLines = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Hint(tr("Прямые подключения не защищены VPN. Имя определяется через FakeDNS, TLS/HTTP или QUIC. Если браузер скрывает имя через ECH, включите FakeDNS и используйте системный DNS вместо защищённого DNS браузера. Уже открытые соединения сохраняют прежний маршрут."))
                    } }
                } else {
                    item { SettingsSection(if (routing.mode == RouteMode.RULES) tr("Приложения для VPN") else tr("Приложения без VPN")) {
                        if (routing.mode == RouteMode.RULES) {
                            Hint(tr("Выбранные приложения целиком идут через VPN. Для остальных применяются адресные правила. На Android ниже 10 приложение соединения может не определиться — тогда трафик остаётся через VPN."))
                        }
                        Text(tr("Выбрано: {0}", selected.size), style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                            label = { Text(tr("Найти приложение")) }, supportingText = { Text(tr("По названию или имени пакета")) }, modifier = Modifier.fillMaxWidth())
                        // Wrapping actions stay reachable with large accessibility fonts.
                        RoutingActions(
                            canSelect = filtered.isNotEmpty(), canClear = selected.isNotEmpty(),
                            onSelect = { saveApps(selected + filtered.map { it.packageName }) },
                            onClear = { saveApps(emptySet()) }
                        )
                    } }
                    if (isLoadingApps && installedApps.isEmpty()) item { Hint(tr("Загрузка списка приложений…")) }
                    else if (filtered.isEmpty()) item { Hint(if (query.isBlank()) tr("Список приложений пуст") else tr("Ничего не найдено. Измените поисковый запрос.")) }
                    items(filtered, key = { it.packageName }) { app ->
                        Surface(shape = RoundedCornerShape(18.dp), color = AtlantSurface, border = BorderStroke(1.dp, AtlantCardBorder)) {
                            Row(Modifier.fillMaxWidth().toggleable(value = app.packageName in selected, role = Role.Checkbox,
                                onValueChange = { checked -> saveApps(if (checked) selected + app.packageName else selected - app.packageName) }).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(app.appName, style = MaterialTheme.typography.titleSmall)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = AtlantTextSecondary)
                                }
                                Checkbox(checked = app.packageName in selected, onCheckedChange = null)
                            }
                        }
                    }
                }
            } else item { Hint(tr("Списки приложений и адресов сохранены. Чтобы изменить их, включите правила и выберите соответствующий режим.")) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutingActions(canSelect: Boolean, canClear: Boolean, onSelect: () -> Unit, onClear: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onSelect, enabled = canSelect) { Text(tr("Выбрать найденные")) }
        TextButton(onClick = onClear, enabled = canClear) { Text(tr("Снять выбор со всех")) }
    }
}

@Composable
private fun SettingsHeader(title: String, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Назад")) }
        Text(title, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = AtlantSurface, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, AtlantCardBorder)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AtlantPrimary, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun Hint(text: String) { Text(text, style = MaterialTheme.typography.bodySmall, color = AtlantTextSecondary) }

@Composable
private fun NavigationSetting(title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 64.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Hint(description)
        }
        Icon(Icons.Default.ChevronRight, null, tint = AtlantTextSecondary)
    }
}

@Composable
private fun ToggleSetting(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange).heightIn(min = 56.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Hint(description)
        }
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
private fun ChoiceRow(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick).heightIn(min = 56.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RadioButton(selected, onClick = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Hint(description)
        }
    }
}

@Composable
private fun ValueSetting(title: String, value: String, choices: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val normalized = when (value.lowercase()) { "none", "silent" -> "off"; "random" -> "randomized"; else -> value.lowercase() }
    val label = choices.firstOrNull { it.first.equals(normalized, true) }?.second ?: value
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = AtlantTextSecondary)
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (key, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(key) }) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LogsContent(logs: List<LogEntry>, onBack: () -> Unit, onClear: () -> Unit, onCopy: () -> Unit) {
    val listState = rememberLazyListState()
    val time = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
    var previousSize by remember { mutableIntStateOf(0) }
    LaunchedEffect(logs.size) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (logs.isNotEmpty() && (previousSize == 0 || lastVisible >= previousSize - 1)) listState.scrollToItem(logs.lastIndex)
        previousSize = logs.size
    }
    Column(Modifier.fillMaxSize()) {
        SettingsHeader(tr("Журнал"), onBack)
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCopy, enabled = logs.isNotEmpty()) { Text(tr("Копировать")) }
            OutlinedButton(onClick = onClear, enabled = logs.isNotEmpty()) { Text(tr("Очистить")) }
        }
        Text(tr("Записей: {0}", logs.size), Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall, color = AtlantTextSecondary)
        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(tr("Пока нет записей. Уровень записи можно изменить в разделе «Диагностика»."), color = AtlantTextSecondary)
            }
        } else LazyColumn(state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { entry ->
                Surface(shape = RoundedCornerShape(16.dp), color = AtlantSurface) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val label = when (entry.level) { LogLevel.INFO -> tr("Информация"); LogLevel.SUCCESS -> tr("Готово"); LogLevel.WARN -> tr("Предупреждение"); LogLevel.ERROR -> tr("Ошибка") }
                        Text("${time.format(java.util.Date(entry.timestamp))} · $label", style = MaterialTheme.typography.labelMedium,
                            color = if (entry.level == LogLevel.ERROR || entry.level == LogLevel.WARN) MaterialTheme.colorScheme.error else AtlantPrimary)
                        Text(entry.message, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = AtlantTextPrimary)
                    }
                }
            }
        }
    }
}
