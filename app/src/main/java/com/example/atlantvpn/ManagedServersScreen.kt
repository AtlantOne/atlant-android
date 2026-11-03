package com.example.atlantvpn

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import atlant.Atlant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private object ManagedServerSessions {
    val jobs = ConcurrentHashMap<String, String>()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagedServersScreen() {
    ManagementTheme { ManagedServersContent() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagedServersContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(JSONArray()) }
    var selected by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var login by remember { mutableStateOf("root") }
    var password by remember { mutableStateOf("") }
    var vpnPort by remember { mutableStateOf("1026") }
    var panelPort by remember { mutableStateOf("19090") }
    var fingerprint by remember { mutableStateOf("") }
    var candidate by remember { mutableStateOf("") }
    var hasStoredPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var managementJob by remember { mutableStateOf("") }
    var installConfirmation by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    var pendingInstall by remember { mutableStateOf(false) }
    var menuServer by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try { list = JSONArray(withContext(Dispatchers.IO) { SecretPreferences.read(context,"managed_servers") } ?: "[]") }
        catch (_: Exception) { status = tr("Не удалось открыть защищённый список серверов") }
    }
    fun request() = JSONObject().put("host",host.trim()).put("port",port.toIntOrNull() ?: 22)
        .put("username",login).put("password",password).put("fingerprint",fingerprint)
        .put("vpn_port",vpnPort.toIntOrNull() ?: 1026).put("panel_port",panelPort.toIntOrNull() ?: 19090)
    suspend fun save() {
        check(host.isNotBlank()) { tr("Укажите адрес сервера") }
        if (name.isBlank()) name = host.trim()
        val id = selected.ifEmpty { UUID.randomUUID().toString() }
        val saved = request()
        val next = JSONArray()
        var replaced = false
        for (i in 0 until list.length()) {
            if (list.getJSONObject(i).getString("id") == id) {
                val previous = list.getJSONObject(i).getJSONObject("request")
                if (password.isBlank() && previous.optString("host") == host.trim() && previous.optInt("port",22) == (port.toIntOrNull() ?: 22) && previous.optString("username") == login && previous.optString("fingerprint") == fingerprint)
                    saved.put("password",previous.optString("password"))
                next.put(JSONObject().put("id",id).put("name",name).put("request",saved)); replaced = true
            } else next.put(list.getJSONObject(i))
        }
        if (!replaced) next.put(JSONObject().put("id",id).put("name",name).put("request",saved))
        withContext(Dispatchers.IO) {SecretPreferences.write(context,"managed_servers",next.toString())}
        list = next; selected = id
    }
    fun connect(install: Boolean) {
        scope.launch {
            busy = true
            status = tr("SSH-подключение")
            try {
                check(host.isNotBlank()) { tr("Укажите адрес сервера") }
                if (fingerprint.isBlank()) {
                    pendingInstall=install
                    candidate=withContext(Dispatchers.IO){Atlant.adminProbe(host.trim(),(port.toIntOrNull()?:22).toLong())}
                    return@launch
                }
                val config = request().put("install",install).put("install_docker",install)
                if (password.isBlank()) for (i in 0 until list.length()) {
                    val item = list.getJSONObject(i)
                    if (item.getString("id") == selected) {
                        val stored = item.getJSONObject("request")
                        if (stored.optString("host") == host.trim() && stored.optInt("port",22) == (port.toIntOrNull() ?: 22) && stored.optString("username") == login && stored.optString("fingerprint") == fingerprint)
                            config.put("password",stored.optString("password"))
                    }
                }
                var previous = ManagedServerSessions.jobs[selected]
                for (i in 0 until list.length()) {
                    val item = list.getJSONObject(i)
                    if (item.getString("id") == selected) {
                        val stored = item.getJSONObject("request")
                        if (stored.optString("host") != host.trim() || stored.optInt("port",22) != (port.toIntOrNull() ?: 22) || stored.optString("username") != login || stored.optString("fingerprint") != fingerprint) {
                            previous?.let { withContext(Dispatchers.IO) { Atlant.adminClose(it) } }
                            ManagedServerSessions.jobs.remove(selected);previous = null
                        }
                    }
                }
                val previousJob = previous
                val phase = if (previousJob == null) "missing" else JSONObject(withContext(Dispatchers.IO) { Atlant.adminStatus(previousJob) }).optString("phase")
                save()
                val job = if (!install && previousJob != null && phase !in listOf("failed", "missing", "closed", "stopped")) previousJob else {
                    if (previousJob != null) withContext(Dispatchers.IO) { Atlant.adminClose(previousJob) }
                    withContext(Dispatchers.IO) { Atlant.adminBegin(config.toString()) }
                }
                ManagedServerSessions.jobs[selected] = job
                password="";showEditor=false
                while (true) {
                    val result = JSONObject(withContext(Dispatchers.IO) { Atlant.adminStatus(job) })
                    val phase = result.optString("phase")
                    status = when (phase) {
                        "connecting" -> tr("SSH-подключение")
                        "preparing" -> tr("Подготовка релиза")
                        "installing" -> tr("Загрузка готового релиза и установка. Можно переключиться на другой экран.")
                        "updating" -> tr("Обновление Atlant")
                        "opening" -> tr("Подключение панели")
                        "ready" -> tr("Панель готова")
                        "closed", "missing" -> tr("Подключение закрыто")
                        "stopped" -> tr("Контейнер остановлен")
                        else -> result.optString("error",phase)
                    }
                    if (phase == "ready") { status="";managementJob = job; break }
                    if (phase in listOf("failed","missing","closed","stopped")) break
                    delay(1500)
                }
            } catch (error: Exception) { status = error.message ?: tr("Операция не выполнена") }
            finally { busy = false }
        }
    }

    if (managementJob.isNotEmpty()) {
        ServerManagementScreen(managementJob, name.ifBlank { host }, onBack = { managementJob = "" })
        return
    }

    fun load(item:JSONObject) {
        selected=item.getString("id");name=item.getString("name")
        val config=item.getJSONObject("request")
        host=config.optString("host");port=config.optInt("port",22).toString();login=config.optString("username","root");password=""
        vpnPort=config.optInt("vpn_port",1026).toString();panelPort=config.optInt("panel_port",19090).toString()
        fingerprint=config.optString("fingerprint");hasStoredPassword=config.optString("password").isNotEmpty();advanced=false;status=""
    }
    fun add() {selected="";name="";host="";login="root";password="";fingerprint="";port="22";vpnPort="1026";panelPort="19090";hasStoredPassword=false;advanced=false;status="";showEditor=true}
    Column(Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text(tr("Мои серверы"),style=MaterialTheme.typography.headlineMedium);Text(tr("Установка и управление"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            if(list.length()>0) FilledTonalIconButton(enabled=!busy,onClick={add()},shape=RoundedCornerShape(8.dp),modifier=Modifier.size(48.dp)) {Icon(Icons.Default.Add,tr("Добавить сервер"))}
        }
        if(list.length()==0) {
            Surface(Modifier.fillMaxWidth().weight(1f),shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),color=MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(22.dp,Alignment.CenterVertically)) {
                    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh) {Icon(Icons.Default.Dns,null,Modifier.padding(20.dp).size(32.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}
                    Text(tr("Добавьте свой сервер"),style=MaterialTheme.typography.titleLarge)
                    Button(enabled=!busy,onClick={add()},contentPadding=PaddingValues(horizontal=24.dp,vertical=14.dp)) {Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text(tr("Добавить сервер"))}
                }
            }
        } else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items((0 until list.length()).map {list.getJSONObject(it)},key={it.getString("id")}) { item ->
                val id=item.getString("id")
                Surface(onClick={if(!busy){load(item);val phase=ManagedServerSessions.jobs[id]?.let {JSONObject(Atlant.adminStatus(it)).optString("phase")};if(hasStoredPassword||phase in listOf("ready","connecting","preparing","installing","opening"))connect(false) else showEditor=true}},shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),color=MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(Modifier.fillMaxWidth().padding(start=18.dp,end=8.dp,top=16.dp,bottom=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        ManagementGlyph(Icons.Default.Dns)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(item.getString("name"),style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text(item.getJSONObject("request").optString("host"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.ChevronRight,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                        Box {
                            IconButton(onClick={menuServer=id}) {Icon(Icons.Default.MoreVert,tr("Действия сервера"))}
                            DropdownMenu(expanded=menuServer==id,onDismissRequest={menuServer=""}) {
                                DropdownMenuItem(enabled=!busy,text={Text(tr("Изменить"))},onClick={menuServer="";load(item);showEditor=true})
                                DropdownMenuItem(enabled=!busy,text={Text(tr("Обновить Atlant"))},onClick={menuServer="";load(item);showEditor=true})
                                DropdownMenuItem(text={Text(tr("Закрыть SSH-канал"))},onClick={menuServer="";scope.launch{ManagedServerSessions.jobs.remove(id)?.let{withContext(Dispatchers.IO){Atlant.adminClose(it)}}}})
                                DropdownMenuItem(enabled=!busy,text={Text(tr("Удалить из списка"),color=MaterialTheme.colorScheme.error)},onClick={menuServer="";load(item);deleteConfirmation=true})
                            }
                        }
                    }
                }
            }
        }
        if(status.isNotBlank()) Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {if(busy) LinearProgressIndicator(Modifier.fillMaxWidth());Text(status,style=MaterialTheme.typography.bodySmall)}
        }
    }
    if(showEditor) ManagementDialog(onDismiss={if(!busy)showEditor=false}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(start=24.dp,end=24.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text(tr(if(selected.isBlank())"Добавить сервер" else "Изменить сервер"),style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f));TextButton(enabled=!busy,onClick={showEditor=false}){Text(tr("Отмена"))}}
            OutlinedTextField(host,{host=it;fingerprint=""},enabled=!busy,label={Text(tr("IP или домен"))},singleLine=true,shape=RoundedCornerShape(8.dp),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(login,{login=it},enabled=!busy,label={Text(tr("SSH-логин"))},singleLine=true,shape=RoundedCornerShape(8.dp),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(password,{password=it},enabled=!busy,label={Text(tr("SSH-пароль"))},placeholder={if(hasStoredPassword)Text(tr("Пароль сохранён"))},singleLine=true,shape=RoundedCornerShape(8.dp),visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
            TextButton(onClick={advanced=!advanced}) {Text(tr("Дополнительно"))}
            if(advanced) {
                OutlinedTextField(name,{name=it},enabled=!busy,label={Text(tr("Название"))},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(port,{port=it;fingerprint=""},enabled=!busy,label={Text(tr("SSH-порт"))},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(vpnPort,{vpnPort=it},enabled=!busy,label={Text(tr("Порт VPN"))},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(panelPort,{panelPort=it},enabled=!busy,label={Text(tr("Локальный порт панели на сервере"))},singleLine=true,modifier=Modifier.fillMaxWidth())
                if(fingerprint.isNotBlank()) Text(fingerprint,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(status.isNotBlank()) Text(status,style=MaterialTheme.typography.bodySmall)
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(enabled=!busy,onClick={if(selected.isBlank())installConfirmation=true else connect(false)},modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)) {Text(tr(if(selected.isBlank())"Установить Atlant" else "Открыть"))}
            if(selected.isNotBlank())OutlinedButton(enabled=!busy,onClick={installConfirmation=true},modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)) {Text(tr("Обновить Atlant"))}
            TextButton(enabled=!busy,onClick={scope.launch {busy=true;try{save();showEditor=false;status=""}catch(e:Exception){status=e.message?:tr("Не удалось сохранить")}finally{busy=false}}},modifier=Modifier.fillMaxWidth()) {Text(tr("Сохранить сервер"))}
        }
    }
    if (candidate.isNotEmpty()) AlertDialog(onDismissRequest={candidate=""},title={Text(tr("Подтвердите SSH-ключ"))},text={Text(tr("Сверьте отпечаток с консолью провайдера:\n{0}", candidate))},confirmButton={TextButton(onClick={fingerprint=candidate;candidate="";connect(pendingInstall)}){Text(tr("Доверять"))}},dismissButton={TextButton(onClick={candidate=""}){Text(tr("Отмена"))}})
    if (installConfirmation) AlertDialog(onDismissRequest={installConfirmation=false},title={Text(tr("Установить сервер?"))},text={Text(tr("Установить или обновить Atlant? Существующая установка будет обновлена с сохранением пользователей, подписок, ключей и настроек. Другие службы не изменяются."))},confirmButton={TextButton(onClick={installConfirmation=false;connect(true)}){Text(tr("Установить"))}},dismissButton={TextButton(onClick={installConfirmation=false}){Text(tr("Отмена"))}})
    if (deleteConfirmation) AlertDialog(onDismissRequest={deleteConfirmation=false},title={Text(tr("Удалить сервер из списка?"))},text={Text(tr("Данные на VDS останутся."))},confirmButton={TextButton(onClick={
        try { val removed = selected;val next=JSONArray();for(i in 0 until list.length())if(list.getJSONObject(i).getString("id")!=removed)next.put(list.getJSONObject(i));SecretPreferences.write(context,"managed_servers",next.toString());list=next;selected="";scope.launch { ManagedServerSessions.jobs.remove(removed)?.let { withContext(Dispatchers.IO) { Atlant.adminClose(it) } } } }catch(e:Exception){status=e.message?:tr("Ошибка сохранения")};deleteConfirmation=false
    }){Text(tr("Удалить"))}},dismissButton={TextButton(onClick={deleteConfirmation=false}){Text(tr("Отмена"))}})
}
