package com.example.atlantvpn

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import atlant.Atlant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.Locale

private data class AdminField(val key: String, val label: String, val value: String = "", val kind: String = "text", val locked: Boolean = false)
private data class AdminEditor(val title: String, val fields: List<AdminField>, val choices: List<Pair<String,String>> = emptyList(), val selected: Set<String> = emptySet(),val choicesTitle:String?=null,val emptyChoicesText:String="Сначала создайте пользователя или подписку", val save: suspend (Map<String,String>, Set<String>) -> Unit)
private fun JSONArray.records(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.array(key: String) = optJSONArray(key) ?: JSONArray()
private fun gib(value: Long) = String.format(Locale.ROOT, "%.2f GiB", value / 1073741824.0)
private fun adminError(error:Exception):String = when {
    error.message.orEmpty().startsWith("group_changed") -> tr("Состав группы изменился. Обновите её перед выполнением действия.")
    error.message.orEmpty().startsWith("group_partial_update") -> tr("Часть участников уже обновлена. Обновите группу перед повторной попыткой.")
    else -> error.message ?: tr("Операция не выполнена")
}
private fun nonnegative(value: String): Long = value.toLongOrNull()?.also { require(it >= 0) } ?: error("Invalid number")
private fun quotaBytes(value: String): Long {
    val n = value.toBigDecimalOrNull() ?: error("Invalid quota")
    require(n.signum() >= 0)
    return n.multiply(1073741824.toBigDecimal()).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact().also { require(it <= 1L.shl(60)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerManagementScreen(job: String, name: String, onBack: () -> Unit,rpc:suspend(String)->String={Atlant.adminCall(job,it)}) {
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf("overview") }
    var data by remember { mutableStateOf<Any?>(null) }
    var loadedPage by remember {mutableStateOf("")}
    var catalog by remember { mutableStateOf(JSONObject()) }
    var service by remember { mutableStateOf(JSONObject()) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<AdminEditor?>(null) }
    var confirmation by remember { mutableStateOf<Pair<String,Boolean>?>(null) }
    var share by remember { mutableStateOf<SubscriptionInfo?>(null) }
    var shareConnections by remember { mutableStateOf(emptyList<Pair<String,String>>()) }
    var shareNotice by remember { mutableStateOf<String?>(null) }
    var renewSubscriptionLink by remember { mutableStateOf<JSONObject?>(null) }
    var expandedSubscription by remember {mutableStateOf("")}
    var groupAccounts by remember {mutableStateOf(emptyList<JSONObject>())}
    var groupRecords by remember {mutableStateOf(emptyList<JSONObject>())}
    var groupSubscriptions by remember {mutableStateOf(emptyList<JSONObject>())}
    var selectedGroup by remember(job) {mutableStateOf("")}
    var groupFilterOpen by remember {mutableStateOf(false)}
    var search by remember(page) {mutableStateOf("")}
    val drawerState=rememberDrawerState(DrawerValue.Closed)
    val pages=listOf("subscriptions" to "Подписки","certificates" to "Сертификаты","inbounds" to "Подключения","groups" to "Группы","overview" to "Обзор","logs" to "Журнал","settings" to "Настройки")
    BackHandler(enabled = editor == null && share == null && confirmation == null && renewSubscriptionLink==null) { if(page!="overview")page="overview" else onBack() }

    suspend fun call(command: String, reveal: Boolean = false, confirmed: Boolean = false): Any? {
        val response = JSONObject(withContext(Dispatchers.IO) {
            rpc(JSONObject().put("command",command).put("reveal",reveal).put("confirmed",confirmed).toString())
        })
        if (response.optBoolean("confirmation_required")) { confirmation = command to reveal; return null }
        return response.opt("result")
    }
    fun action(command: String, reveal: Boolean = false) {
        scope.launch { loading = true; error = ""; try { call(command,reveal); revision++ } catch (e: Exception) { error=adminError(e) } finally { loading=false } }
    }
    fun openUser(user: JSONObject? = null) {
        editor = AdminEditor(tr(if(user==null) "Добавить пользователя" else "Изменить"), listOf(
            AdminField("name","Имя пользователя",user?.optString("name").orEmpty(),locked=user!=null),
            AdminField("note","Заметка",user?.optString("note").orEmpty()),
            AdminField("ip_limit","Лимит IP",(user?.optInt("ip_limit") ?: 0).toString(),"number"),
            AdminField("expires_at","Срок действия",(user?.optLong("expires_at") ?: 0).toString(),"date"),
            AdminField("enabled","Включено",(user?.optBoolean("enabled") ?: true).toString(),"bool")
        ),save={ values,_ ->
            val patch=JSONObject().put("note",values.getValue("note")).put("ip_limit",nonnegative(values.getValue("ip_limit")))
                .put("expires_at",nonnegative(values.getValue("expires_at"))).put("enabled",values["enabled"]=="true")
            call(if(user==null) "usercreate "+patch.put("name",values.getValue("name")) else "set ${user.getString("name")} $patch")
            search="";if(user==null)selectedGroup=""
        })
    }
    fun openSubscription(sub: JSONObject? = null) {
        scope.launch { loading=true;error="";try {
        val currentCatalog=call("inbounds") as JSONObject
        catalog=currentCatalog
        val serverId = currentCatalog.getString("server_id")
        val selected = sub?.array("inbound_refs")?.records()?.filter { it.optString("server_id")==serverId }?.map { it.getString("inbound_id") }?.toSet() ?: emptySet()
        editor = AdminEditor(tr(if(sub==null) "Создать подписку" else "Изменить подписку"),listOf(
            AdminField("title","Название",sub?.optString("title").orEmpty()),
            AdminField("quota","Лимит трафика (ГиБ)",((sub?.optLong("total_limit") ?: 0)/1073741824.0).toString(),"decimal"),
            AdminField("expires_at","Срок действия",(sub?.optLong("expires_at") ?: 0).toString(),"date"),
            AdminField("ip_limit","Лимит IP",(sub?.optInt("ip_limit") ?: 0).toString(),"number"),
            AdminField("note","Заметка",sub?.optString("note").orEmpty()),
            AdminField("enabled","Включено",(sub?.optBoolean("enabled") ?: true).toString(),"bool")
        ).filter {sub==null||sub.optBoolean("managed")||it.key !in setOf("ip_limit","note")},choices=currentCatalog.array("inbounds").records().map { val spec=it.getJSONObject("settings");spec.getString("id") to "${spec.optString("name")} · ${spec.optString("listen")}" },selected=selected,choicesTitle="Подключения",emptyChoicesText="Сначала создайте подключение в разделе «Подключения»",save={ values,chosen ->
            val refs=JSONArray()
            sub?.array("inbound_refs")?.records()?.filter { it.optString("server_id")!=serverId }?.forEach { refs.put(it) }
            chosen.forEach { refs.put(JSONObject().put("server_id",serverId).put("inbound_id",it)) }
            require(refs.length()>0 || (sub?.array("nodes")?.length() ?: 0)>0) { tr("Выберите подключения") }
            val patch=JSONObject().put("title",values.getValue("title")).put("total_limit",quotaBytes(values.getValue("quota")))
                .put("enabled",values["enabled"]=="true").put("expires_at",nonnegative(values.getValue("expires_at"))).put("inbound_refs",refs)
            if(sub==null||sub.optBoolean("managed")) patch.put("ip_limit",nonnegative(values.getValue("ip_limit"))).put("note",values.getValue("note"))
            if(sub!=null)patch.put("revision",sub.get("revision"))
            call(if(sub==null) "subscribercreate $patch" else "subset ${sub.getString("id")} $patch",reveal=sub==null)
            search="";if(sub==null)selectedGroup=""
        })
        }catch(e:Exception){error=adminError(e)}finally{loading=false}}
    }
    fun openInbound(spec: JSONObject? = null) {
        val snapshot=JSONObject(catalog.toString())
        editor=AdminEditor(tr(if(spec==null) "Добавить подключение" else "Изменить подключение"),listOf(
            AdminField("id","Идентификатор",spec?.optString("id") ?: "in_"+UUID.randomUUID().toString().replace("-","").take(12),locked=spec!=null),
            AdminField("name","Название",spec?.optString("name").orEmpty()),
            AdminField("listen","Адрес прослушивания",spec?.optString("listen") ?: ":1026"),
            AdminField("advertise_addr","Адрес VPN для клиентов (IP/домен:порт)",spec?.optString("advertise_addr").orEmpty()),
            AdminField("sni","Имя сервера REALITY (SNI)",spec?.optString("sni").orEmpty()),
            AdminField("enabled","Включено",(spec?.optBoolean("enabled") ?: true).toString(),"bool")
        ),choices=listOf("tcp" to "TCP","quic" to "QUIC","xhttp" to "XHTTP"),selected=spec?.array("transports")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: setOf("tcp","quic","xhttp"),save={ values,chosen ->
            require(chosen.isNotEmpty()) { tr("Выберите транспорт") }
            val patch=if(spec==null)JSONObject() else JSONObject(spec.toString())
            values.forEach { (key,value) -> patch.put(key,if(key=="enabled")value=="true" else value) }
            patch.put("wire_version",7).put("transports",JSONArray(chosen.toList()))
            call((if(spec==null)"inboundcreate " else "inboundset ")+snapshot.getLong("revision")+" "+patch)
        })
    }
    fun accountLabel(user:JSONObject):String {
        val account=user.optString("name")
        val titles=groupSubscriptions.filter{it.optString("user")==account||it.optString("id")==user.optString("subscription_id")}.map{it.optString("title")}.filter{it.isNotBlank()}
        return if(titles.isEmpty())account else titles.joinToString(", ")
    }
    fun openGroup(group: JSONObject? = null) {
        scope.launch { try {
            val all=if(page=="groups")JSONObject().put("users",JSONArray(groupAccounts)) else call("usersall") as JSONObject
            editor=AdminEditor(tr(if(group==null)"Создать группу" else "Настроить группу"),listOf(AdminField("name","Название",group?.optString("name").orEmpty()),AdminField("note","Заметка",group?.optString("note").orEmpty())),
                choices=all.array("users").records().map {user -> user.getString("name") to accountLabel(user) },
                selected=group?.array("users")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet(),choicesTitle="Участники группы",save={values,selected ->
                    require(values.getValue("name").isNotBlank()){tr("Укажите название группы")}
                    val patch=JSONObject(values).put("name",values.getValue("name").trim()).put("users",JSONArray(selected.toList()));if(group!=null)patch.put("revision",group.get("revision"))
                    call(if(group==null)"groupcreate $patch" else "groupset ${group.getString("id")} $patch")
                    search=""
                })
        }catch(e:Exception){error=adminError(e)} }
    }
    suspend fun connectionLinks(user:String,refs:List<JSONObject>?=null):List<Pair<String,String>> {
        val credentials=call("link $user",true) as JSONObject
        val applied=call("appliedinbounds") as JSONObject
        return managementConnectionLinks(credentials.getString("link"),applied,refs)
    }
    fun showUserLink(user:JSONObject) {scope.launch {loading=true;error="";try {
        val owner=groupSubscriptions.firstOrNull{it.optString("user")==user.optString("name")}
        shareConnections=connectionLinks(user.getString("name"),owner?.array("inbound_refs")?.records())
        require(shareConnections.isNotEmpty()){tr("Нет доступных подключений")}
        shareNotice=null
        share=SubscriptionInfo(name=accountLabel(user))
    }catch(e:Exception){error=adminError(e)}finally{loading=false}}}
    fun showLink(sub: JSONObject) { scope.launch {loading=true;error="";try {
        val result=call("subshare ${sub.getString("id")}",true) as JSONObject
        val url=result.optString("url").takeIf{it.startsWith("https://")}.orEmpty()
        if(url.isBlank()) {
            val https=call("tls") as JSONObject
            if(https.optBoolean("enabled")&&https.optString("public_url").startsWith("https://"))renewSubscriptionLink=sub
            else {page="certificates";error=tr("Настройте HTTPS в разделе «Сертификаты», затем создайте ссылку подписки.")}
            return@launch
        }
        shareConnections=emptyList()
        shareNotice=null
        share=SubscriptionInfo(id=sub.getString("id"),name=sub.optString("title"),url=url)
    }catch(e:Exception){error=adminError(e)}finally{loading=false} } }

    LaunchedEffect(job,page,revision) {
        loading=true;error=""
        do {
            try {
                val command=mapOf("subscriptions" to "subscriptions","users" to "usersall","certificates" to "tls","inbounds" to "inbounds","groups" to "groups","logs" to "logs 50","overview" to "status","settings" to "configinfo").getValue(page)
                if(page=="overview") {
                    service=call("service status") as JSONObject
                    data=JSONObject()
                    if(service.optString("active")=="active")try {data=call("status")}catch(e:Exception){error=adminError(e)}
                } else data=call(command)
                if(page in listOf("subscriptions","inbounds"))catalog=if(page=="inbounds")data as JSONObject else call("inbounds") as JSONObject
                if(page in setOf("groups","users","subscriptions")) {
                    groupRecords=if(page=="groups")(data as JSONArray).records() else (call("groups") as JSONArray).records()
                    if(page=="groups")groupAccounts=(call("usersall") as JSONObject).array("users").records()
                    if(page=="users")groupAccounts=(data as JSONObject).array("users").records()
                    groupSubscriptions=if(page=="subscriptions")(data as JSONArray).records() else (call("subscriptions") as JSONArray).records()
                    if(selectedGroup.isNotEmpty()&&groupRecords.none{it.optString("id")==selectedGroup})selectedGroup=""
                }
                loadedPage=page
            }catch(e:Exception){error=adminError(e)}finally{loading=false}
            if(page=="logs")delay(2500)
        }while(page=="logs")
    }
    ModalNavigationDrawer(drawerState=drawerState,gesturesEnabled=false,drawerContent={
        ModalDrawerSheet(drawerContainerColor=MaterialTheme.colorScheme.surface,drawerShape=RoundedCornerShape(topEnd=28.dp,bottomEnd=28.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(name,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(12.dp,24.dp),maxLines=2,overflow=TextOverflow.Ellipsis)
                for((key,label)in pages)NavigationDrawerItem(label={Text(tr(label))},selected=page==key,onClick={page=key;scope.launch{drawerState.snapTo(DrawerValue.Closed)}},modifier=Modifier.padding(vertical=3.dp),shape=RoundedCornerShape(16.dp))
                Spacer(Modifier.height(20.dp))
                TextButton(onClick=onBack) {Text(tr("Назад к моим серверам"))}
            }
        }
    }) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick={scope.launch{drawerState.snapTo(DrawerValue.Open)}}) {Icon(Icons.Default.Menu,contentDescription=tr("Меню"))}
            Column(Modifier.weight(1f)) {Text(name,style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis);Text(tr("Ваш сервер"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            IconButton(onClick=onBack) {Icon(Icons.Default.Close,contentDescription=tr("Назад к моим серверам"))}
        }
        Row(Modifier.fillMaxWidth().padding(start=20.dp,end=12.dp,bottom=12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Text(tr(pages.firstOrNull{it.first==page}?.second?:"Участники группы"),style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f))
            if(page in setOf("subscriptions","users","inbounds","groups"))FilledTonalIconButton(enabled=!loading,onClick={when(page){"subscriptions"->openSubscription();"users"->openUser();"inbounds"->openInbound();"groups"->openGroup()}}) {Icon(Icons.Default.Add,contentDescription=tr("Добавить"))}
        }
        if(page in setOf("subscriptions","users","inbounds","groups"))Row(Modifier.fillMaxWidth().padding(horizontal=16.dp).padding(bottom=6.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            CompactManagementSearch(search,{search=it},Modifier.weight(1f).testTag("admin-search"))
            if(page in setOf("users","subscriptions")&&groupRecords.isNotEmpty())Box {
                IconButton(onClick={groupFilterOpen=true}){Icon(Icons.Default.FilterList,tr("Фильтр по группе"))}
                DropdownMenu(expanded=groupFilterOpen,onDismissRequest={groupFilterOpen=false}) {
                    DropdownMenuItem(text={Text(tr("Все группы"))},onClick={selectedGroup="";groupFilterOpen=false})
                    for(group in groupRecords)DropdownMenuItem(text={Text(group.optString("name"))},onClick={selectedGroup=group.getString("id");groupFilterOpen=false})
                }
            }
        }
        val membersFilter=groupRecords.firstOrNull{it.optString("id")==selectedGroup}?.array("users")?.let{a->(0 until a.length()).map{a.getString(it)}.toSet()}?:emptySet()
        if(selectedGroup.isNotEmpty()&&page in setOf("users","subscriptions"))Row(Modifier.fillMaxWidth().padding(start=20.dp,end=12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {Text(groupRecords.firstOrNull{it.optString("id")==selectedGroup}?.optString("name").orEmpty(),style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f));TextButton(onClick={selectedGroup=""}){Text(tr("Сбросить"))}}
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(16.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(loadedPage==page) when(page) {
                "certificates" -> item {SubscriptionCertificatesPanel((data as? JSONObject)?:JSONObject(),loading,
                    onSave={patch->action("tlsset $patch")},onRestart={action("service restart")})}
                "subscriptions" -> {
                    items(((data as? JSONArray)?.records() ?: emptyList()).filter{sub->sub.optString("title").contains(search,true)&&(selectedGroup.isEmpty()||sub.optString("user") in membersFilter||sub.array("nodes").records().any{node->node.optString("user",sub.optString("user")) in membersFilter})},key={it.getString("id")}) { sub ->
                        val quota=sub.optJSONObject("global_quota")
                        val used=if(quota!=null)quota.optLong("upload")+quota.optLong("download") else sub.optLong("up_bytes")+sub.optLong("down_bytes")
                        val limit=quota?.optLong("total_limit") ?: sub.optLong("total_limit")
                        AdminRecord(sub.optString("title"),gib(used)+if(limit>0)" / "+gib(limit) else " · "+tr("Безлимит"),icon=Icons.Default.Link) {
                            if(limit>0)LinearProgressIndicator(progress={ (used.toFloat()/limit).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
                            TextButton(onClick={expandedSubscription=if(expandedSubscription==sub.getString("id"))"" else sub.getString("id")}) {Text(tr("Подключения")+" · "+(sub.array("inbound_refs").length()+sub.array("nodes").length()))}
                            if(expandedSubscription==sub.getString("id")) {
                                for(ref in sub.array("inbound_refs").records()) {
                                    val spec=catalog.array("inbounds").records().map {it.getJSONObject("settings")}.find {catalog.optString("server_id")==ref.optString("server_id")&&it.optString("id")==ref.optString("inbound_id")}
                                    Text(if(spec!=null)spec.optString("name")+" · "+spec.optString("advertise_addr") else ref.optString("inbound_id"),modifier=Modifier.padding(start=12.dp),style=MaterialTheme.typography.bodySmall)
                                }
                                for(node in sub.array("nodes").records())Text(node.optString("name")+" · "+node.optString("server"),modifier=Modifier.padding(start=12.dp),style=MaterialTheme.typography.bodySmall)
                            }
                            AdminActions(!loading,listOf(AdminAction("Изменить",{openSubscription(sub)}),AdminAction("Ссылка и QR-код",{showLink(sub)}),AdminAction("Удалить",{action("subdelete ${sub.getString("id")}")},danger=true)))
                        }
                    }
                }
                "users" -> {
                    items(((data as? JSONObject)?.array("users")?.records() ?: emptyList()).filter{(accountLabel(it).contains(search,true)||it.optString("name").contains(search,true)||it.optString("note").contains(search,true))&&(selectedGroup.isEmpty()||it.optString("name") in membersFilter)},key={it.getString("name")}) { user ->
                        AdminRecord(accountLabel(user),tr(if(user.optBoolean("enabled"))"Включено" else "Отключено")+user.optString("note").takeIf{it.isNotBlank()}?.let{" · $it"}.orEmpty(),icon=Icons.Default.Person) {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(enabled=!loading,onClick={showUserLink(user)}){Icon(Icons.Default.QrCode,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(tr("Ссылка и QR-код"))}}
                            AdminActions(!loading,listOf(AdminAction("Изменить",{openUser(user)}),AdminAction(if(user.optBoolean("enabled"))"Отключить" else "Включить",{action((if(user.optBoolean("enabled"))"disable " else "enable ")+user.getString("name"))}),AdminAction("Отключить сессии",{action("kick ${user.getString("name")}")}),AdminAction("Удалить",{action("delete ${user.getString("name")}")},danger=true)))
                        }
                    }
                }
                "inbounds" -> {
                    if(catalog.optLong("revision")!=catalog.optLong("applied_revision"))item {
                        Text(tr("Изменения не применены"),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                        TextButton(enabled=!loading,onClick={action("inboundapply ${catalog.optLong("revision")}")}) {Text(tr("Повторить применение"))}
                    }
                    items(catalog.array("inbounds").records().filter{it.getJSONObject("settings").optString("name").contains(search,true)},key={it.getJSONObject("settings").getString("id")}) { record ->
                        val spec=record.getJSONObject("settings")
                        AdminRecord(spec.optString("name"),spec.optString("listen")+" · "+spec.array("transports").join(" / ").replace("\"","")) {
                            AdminActions(!loading,listOf(AdminAction("Изменить",{openInbound(spec)}),AdminAction("Удалить",{action("inbounddelete ${catalog.optLong("revision")} ${spec.getString("id")}")},danger=true)))
                        }
                    }
                }
                "groups" -> {
                    if(((data as? JSONArray)?.length()?:0)==0)item {
                        Column(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            ServerGlyph(Icons.Default.Workspaces)
                            Text(tr("Пока нет групп"),style=MaterialTheme.typography.titleMedium)
                            Button(onClick={openGroup()}){Text(tr("Создать группу"))}
                        }
                    }
                    items(((data as? JSONArray)?.records() ?: emptyList()).filter{it.optString("name").contains(search,true)},key={it.getString("id")}) { group ->
                        val members=group.array("users");val names=(0 until members.length()).map{members.getString(it)}.toSet()
                        val active=groupAccounts.count{it.optString("name") in names&&it.optBoolean("enabled")}
                        AdminRecord(group.optString("name"),tr("Участников: {0}",members.length())+" · "+tr("Включено: {0}",active),icon=Icons.Default.Workspaces,onOpen={openGroup(group)}) {
                            if(group.optString("note").isNotBlank())Text(group.optString("note"),style=MaterialTheme.typography.bodySmall)
                            val preview=groupAccounts.filter{it.optString("name") in names}.take(3).map{accountLabel(it)}
                            Text(if(preview.isEmpty())tr("Добавьте участников в группу") else preview.joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                            AdminActions(!loading,listOf(
                                AdminAction("Настроить группу",{openGroup(group)}),
                                AdminAction("Показать пользователей",{selectedGroup=group.getString("id");page="users"}),
                                AdminAction("Включить всех",{action("groupaccess ${group.getString("id")} "+JSONObject().put("revision",group.getLong("revision")).put("enabled",true))},enabled=names.isNotEmpty()),
                                AdminAction("Отключить всех",{action("groupaccess ${group.getString("id")} "+JSONObject().put("revision",group.getLong("revision")).put("enabled",false))},danger=true,enabled=names.isNotEmpty()),
                                AdminAction("Удалить",{action("groupdelete ${group.getString("id")}")},danger=true)),prominent=true)
                        }
                    }
                }
                "logs" -> item {
                    val logs=(data as? JSONObject)?.opt("logs")
                    val text=if(logs is JSONArray)(maxOf(0,logs.length()-50) until logs.length()).joinToString("\n"){logs.optString(it)} else logs?.toString().orEmpty()
                    Surface(shape=RoundedCornerShape(20.dp),tonalElevation=2.dp) {SelectionContainer {Text(text,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(16.dp))}}
                }
                "settings" -> item {
                    val level=(data as? JSONObject)?.optString("log_level","info") ?: "info"
                    AdminRecord(tr("Уровень логирования"),level,icon=Icons.Default.Tune) {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {for(value in listOf("debug","info","warn","error","none"))TextButton(enabled=!loading,onClick={action("configsave "+JSONObject().put("log_level",value))}) {Text(value)}}
                        Text(tr("Изменение уровня логирования применяется после перезапуска службы VPN."),style=MaterialTheme.typography.bodySmall)
                        TextButton(enabled=!loading,onClick={action("service restart")}) {Text(tr("Перезапустить"))}
                    }
                }
                "overview" -> item {ServerOverviewPanel((data as? JSONObject)?:JSONObject(),service,onNavigate={page=it},onService={action("service $it")},busy=loading)}
            }
        }
    }
    confirmation?.let { pending -> AlertDialog(onDismissRequest={confirmation=null},title={Text(tr("Подтвердить изменение"))},text={Text(tr("Это действие изменит доступ или остановит активные подключения."))},confirmButton={TextButton(onClick={confirmation=null;scope.launch {loading=true;try {call(pending.first,pending.second,true);revision++}catch(e:Exception){error=adminError(e)}finally{loading=false}}}) {Text(tr("Подтвердить"))}},dismissButton={TextButton(onClick={confirmation=null}){Text(tr("Отмена"))}}) }
    renewSubscriptionLink?.let{sub->AlertDialog(onDismissRequest={renewSubscriptionLink=null},title={Text(tr("Создать HTTPS-ссылку"))},text={Text(tr("Ссылка этой подписки не сохранилась. Создать новую? Прежняя ссылка перестанет работать."))},confirmButton={TextButton(onClick={renewSubscriptionLink=null;scope.launch{loading=true;try{
        call("subrotate ${sub.getString("id")}",true,true)
        val result=call("subshare ${sub.getString("id")}",true) as JSONObject
        val url=result.getString("url");require(url.startsWith("https://")){tr("HTTPS-ссылка недоступна")}
        shareConnections=emptyList();shareNotice=null;share=SubscriptionInfo(id=sub.getString("id"),name=sub.optString("title"),url=url);revision++
    }catch(e:Exception){error=adminError(e)}finally{loading=false}}}){Text(tr("Создать ссылку"))}},dismissButton={TextButton(onClick={renewSubscriptionLink=null}){Text(tr("Отмена"))}})}
    editor?.let { current -> AdminEditDialog(current,onDismiss={editor=null},onSaved={editor=null;revision++}) }
    share?.let { SubscriptionQrDialog(it,onDismiss={share=null},connectionLinks=shareConnections,notice=shareNotice) }
    }
}

private data class AdminAction(val label:String,val run:()->Unit,val danger:Boolean=false,val enabled:Boolean=true)

@Composable
private fun AdminActions(enabled:Boolean,actions:List<AdminAction>,prominent:Boolean=false) {
    var expanded by remember {mutableStateOf(false)}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
        actions.firstOrNull()?.let { first ->
            if(prominent)FilledTonalButton(enabled=enabled&&first.enabled,onClick=first.run,shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)){Icon(Icons.Default.Edit,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text(tr(first.label))}
            else TextButton(enabled=enabled&&first.enabled,onClick=first.run) {Icon(Icons.Default.Edit,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text(tr(first.label))}
        }
        if(actions.size>1)Box {
            IconButton(enabled=enabled,onClick={expanded=true}) {Icon(Icons.Default.MoreVert,tr("Действия"))}
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                for(action in actions.drop(1))DropdownMenuItem(text={Text(tr(action.label),color=if(action.danger)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)},enabled=enabled&&action.enabled,onClick={expanded=false;action.run()})
            }
        }
    }
}

@Composable
private fun AdminRecord(title: String, subtitle: String,icon:ImageVector=Icons.Default.Dns,onOpen:(()->Unit)?=null, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().then(if(onOpen!=null)Modifier.clickable(onClick=onOpen) else Modifier),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ServerGlyph(icon)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {Text(title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis);if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)}
            }
            content()
        }
    }
}

@Composable
internal fun ManagementDialog(onDismiss:()->Unit,content:@Composable ()->Unit) {
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=true)) {
        val view=LocalView.current
        DisposableEffect(view) {(view.parent as? DialogWindowProvider)?.window?.setWindowAnimations(0);onDispose {}}
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {content()}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminEditDialog(editor: AdminEditor, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope=rememberCoroutineScope()
    val values=remember(editor) {mutableStateMapOf<String,String>().apply {editor.fields.forEach{put(it.key,it.value)}}}
    val selected=remember(editor) {mutableStateMapOf<String,Boolean>().apply {editor.selected.forEach{put(it,true)}}}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var search by remember {mutableStateOf("")}
    var dateField by remember {mutableStateOf<AdminField?>(null)}
    val choices=remember(editor.choices,search) {editor.choices.filter{it.second.contains(search,true)}}
    fun save(){scope.launch {busy=true;error="";try {editor.save(values.toMap(),selected.filterValues{it}.keys);onSaved()}catch(e:Exception){error=adminError(e)}finally{busy=false}}}
    ManagementDialog(onDismiss={if(!busy)onDismiss()}) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(enabled=!busy,onClick=onDismiss){Icon(Icons.Default.Close,tr("Отмена"))}
                Text(editor.title,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                Button(enabled=!busy,onClick={save()},shape=RoundedCornerShape(14.dp)){Text(tr("Сохранить"))}
            }
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(horizontal=20.dp,vertical=8.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                val visible=editor.fields.filter{it.key!="id"}
                for((section,fields)in visible.groupBy{if(it.key in setOf("quota","expires_at","ip_limit","enabled"))"Доступ" else if(it.key in setOf("listen","advertise_addr","sni"))"Подключение" else "Основное"}) {
                    item(key="heading_"+section){Text(tr(section),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    items(fields,key={it.key}) {field ->
                        if(field.kind=="bool")Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text(tr(field.label));Switch(checked=values[field.key]=="true",enabled=!busy,onCheckedChange={values[field.key]=it.toString()})}
                        else if(field.kind=="date") {
                            val timestamp=values[field.key]?.toLongOrNull()?:0
                            Column {Text(tr(field.label),style=MaterialTheme.typography.labelLarge);Row {OutlinedButton(enabled=!busy,onClick={dateField=field}){Text(if(timestamp>0)java.text.DateFormat.getDateInstance().format(java.util.Date(timestamp*1000)) else tr("Без срока"))};if(timestamp>0)TextButton(enabled=!busy,onClick={values[field.key]="0"}){Text(tr("Сбросить"))}}}
                        } else OutlinedTextField(value=values[field.key].orEmpty(),onValueChange={values[field.key]=it},enabled=!busy&&!field.locked,label={Text(tr(field.label))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(16.dp),keyboardOptions=KeyboardOptions(keyboardType=when(field.kind){"number"->KeyboardType.Number;"decimal"->KeyboardType.Decimal;else->KeyboardType.Text}))
                    }
                }
                if(editor.choices.isNotEmpty()||editor.choicesTitle!=null) {
                    item(key="choices_header"){Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Icon(Icons.Default.Link,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.width(8.dp));Text(tr(editor.choicesTitle?:"Выбор"),style=MaterialTheme.typography.titleSmall)}
                        Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh){Text(tr("Выбрано")+": "+selected.count{it.value},modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    }}
                    if(editor.choices.isEmpty())item {Text(tr(editor.emptyChoicesText),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    if(editor.choices.size>6)item(key="choices_search"){CompactManagementSearch(search,{search=it},Modifier.fillMaxWidth(),enabled=!busy)}
                    items(choices,key={"choice_"+it.first}) {(key,label)->
                        Surface(shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,if(selected[key]==true)MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f)),color=MaterialTheme.colorScheme.surfaceContainerLow) {
                            Row(Modifier.fillMaxWidth().clickable(enabled=!busy){selected[key]=selected[key]!=true}.padding(start=16.dp,end=8.dp,top=2.dp,bottom=2.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                                Text(label,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium);Checkbox(checked=selected[key]==true,onCheckedChange=null,enabled=!busy)
                            }
                        }
                    }
                }


            }
        }
    }
    dateField?.let {field ->
        val timestamp=values[field.key]?.toLongOrNull()?:0
        val picker=rememberDatePickerState(initialSelectedDateMillis=timestamp.takeIf{it>0}?.times(1000))
        DatePickerDialog(onDismissRequest={dateField=null},confirmButton={TextButton(onClick={picker.selectedDateMillis?.let{values[field.key]=(it/1000).toString()};dateField=null}){Text(tr("Применить"))}},dismissButton={TextButton(onClick={dateField=null}){Text(tr("Отмена"))}}){DatePicker(state=picker)}
    }
}
