package com.example.atlantvpn

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.Locale

internal fun displayTraffic(bytes:Long):String {
    val value=bytes.coerceAtLeast(0).toDouble()
    return when {
        value>=1073741824 -> String.format(Locale.getDefault(),"%.1f GiB",value/1073741824)
        value>=1048576 -> String.format(Locale.getDefault(),"%.0f MiB",value/1048576)
        else -> String.format(Locale.getDefault(),"%.0f KiB",value/1024)
    }
}

@Composable
fun SubscriptionServerList(
    servers:List<ServerProfile>,selectedServer:ServerProfile?,subscription:SubscriptionInfo?,
    onSelectServer:(ServerProfile)->Unit,onDeleteServer:(ServerProfile)->Unit,
    onEditServer:(ServerProfile)->Unit,onPingServer:(ServerProfile)->Unit,
    onCopyLink:(ServerProfile)->Unit,onUpdateSubscription:()->Unit,
    onEditSubscription:()->Unit,onClearSubscription:()->Unit,
    modifier:Modifier=Modifier,subscriptions:List<SubscriptionInfo> = listOfNotNull(subscription),onFocusSubscription:(String)->Unit={}
) {
    var collapsed by rememberSaveable {mutableStateOf(listOf<String>())}
    val standalone=remember(servers){servers.filterNot{it.isSubscription}}
    val grouped=remember(servers){servers.filter{it.isSubscription}.groupBy{it.subscriptionId}}
    LazyColumn(modifier.testTag("server-list"),contentPadding=PaddingValues(bottom=16.dp)) {
        subscriptions.forEachIndexed {index,sub->
            val key=sub.id.ifBlank{sub.url};val expanded=key !in collapsed;val children=grouped[sub.id].orEmpty()
            if(index>0)item(key="gap:$key"){Spacer(Modifier.height(12.dp))}
            item(key="subscription:$key",contentType="subscription-header") {
                SubscriptionGroupHeader(sub,children.size,expanded,
                    onToggle={collapsed=if(expanded)collapsed+key else collapsed-key},
                    onUpdate={onFocusSubscription(sub.id);onUpdateSubscription()},onEdit={onFocusSubscription(sub.id);onEditSubscription()},onClear={onFocusSubscription(sub.id);onClearSubscription()})
            }
            if(expanded)itemsIndexed(children,key={_,server->"subscription:${server.id}"},contentType={_,_->"subscription-server"}) {position,server->
                Surface(Modifier.fillMaxWidth().testTag("subscription-server:${server.id}"),shape=if(position==children.lastIndex)RoundedCornerShape(bottomStart=24.dp,bottomEnd=24.dp) else RoundedCornerShape(0.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.padding(horizontal=12.dp)) {
                        if(position>0)HorizontalDivider(Modifier.padding(start=52.dp,end=8.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f))
                        SubscriptionServerRow(server,server.id==selectedServer?.id,{onSelectServer(server)},{onPingServer(server)},{onEditServer(server)},{onCopyLink(server)},{onDeleteServer(server)})
                        if(position==children.lastIndex)Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        if(standalone.isNotEmpty()&&subscriptions.isNotEmpty())item(key="manual-title"){Text(tr("Добавленные вручную"),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=22.dp,bottom=10.dp,start=4.dp))}
        items(standalone,key={"manual:${it.id}"},contentType={"standalone"}) {server->
            Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surfaceContainerLow,modifier=Modifier.padding(bottom=8.dp)) {
                Box(Modifier.padding(horizontal=12.dp,vertical=6.dp)){SubscriptionServerRow(server,server.id==selectedServer?.id,{onSelectServer(server)},{onPingServer(server)},{onEditServer(server)},{onCopyLink(server)},{onDeleteServer(server)})}
            }
        }
        if(servers.isEmpty()&&subscriptions.isEmpty())item(key="empty") {
            Column(Modifier.fillMaxWidth().padding(vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {ServerGlyph(Icons.Default.Link);Text(tr("Добавьте подписку"),style=MaterialTheme.typography.titleMedium);Text(tr("Ссылка или QR-код — и серверы появятся здесь"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}

@Composable
private fun SubscriptionServerRow(server:ServerProfile,selected:Boolean,onSelect:()->Unit,onPing:()->Unit,onEdit:()->Unit,onCopy:()->Unit,onDelete:()->Unit) {
    var menu by remember {mutableStateOf(false)}
    Surface(shape=RoundedCornerShape(16.dp),color=if(selected)MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().selectable(selected=selected,role=Role.RadioButton,onClick=onSelect).padding(start=10.dp,end=0.dp,top=10.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            ServerGlyph(if(selected)Icons.Default.Check else Icons.Default.Dns,Modifier.size(36.dp),bright=selected)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {Text(server.name,style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis);Text(server.server,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)}
            TextButton(onClick=onPing,contentPadding=PaddingValues(horizontal=6.dp)) {Text(if(server.pingMs>0)tr("{0} мс",server.pingMs) else tr("Пинг"),style=MaterialTheme.typography.labelMedium)}
            Box {
                IconButton(onClick={menu=true},modifier=Modifier.size(40.dp)){Icon(Icons.Default.MoreVert,tr("Действия сервера {0}",server.name),Modifier.size(20.dp))}
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    DropdownMenuItem(text={Text(tr("Редактировать"))},onClick={menu=false;onEdit()})
                    DropdownMenuItem(text={Text(tr("Копировать ссылку"))},onClick={menu=false;onCopy()})
                    if(!server.isSubscription)DropdownMenuItem(text={Text(tr("Удалить сервер"),color=MaterialTheme.colorScheme.error)},onClick={menu=false;onDelete()})
                }
            }
        }
    }
}

@Composable
internal fun SubscriptionGroupHeader(subscription:SubscriptionInfo,count:Int,expanded:Boolean,onToggle:()->Unit,onUpdate:()->Unit,onEdit:()->Unit,onClear:()->Unit) {
    var menu by remember {mutableStateOf(false)};var remove by remember {mutableStateOf(false)};var qr by remember {mutableStateOf(false)};var info by remember {mutableStateOf(false)}
    val usage=subscriptionUsage(subscription.upload,subscription.download,subscription.total)
    val host=remember(subscription.url){runCatching{Uri.parse(subscription.url).host}.getOrNull().orEmpty()}
    Surface(Modifier.fillMaxWidth().testTag("subscription-header"),shape=if(expanded&&count>0)RoundedCornerShape(topStart=24.dp,topEnd=24.dp) else RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(start=16.dp,end=8.dp,top=12.dp,bottom=if(expanded&&count>0)4.dp else 8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f).testTag("subscription-toggle").semantics{stateDescription=if(expanded)tr("Развёрнута") else tr("Свёрнута")}.clickable(role=Role.Button,onClick=onToggle),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ServerGlyph(Icons.Default.Link)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {Text(subscription.name.ifBlank{host},style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis);Text(tr("Серверов: {0}",count),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick={menu=true},modifier=Modifier.testTag("subscription-actions")){Icon(Icons.Default.MoreVert,tr("Действия подписки"),Modifier.size(20.dp))}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        DropdownMenuItem(text={Text(tr("Редактировать подписку"))},onClick={menu=false;onEdit()})
                        DropdownMenuItem(text={Text(tr("Информация о подписке"))},onClick={menu=false;info=true})
                        DropdownMenuItem(text={Text(tr("Удалить подписку"),color=MaterialTheme.colorScheme.error)},onClick={menu=false;remove=true})
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(if(subscription.total>0)tr("Осталось {0} из {1}",displayTraffic(usage.remaining),displayTraffic(subscription.total)) else tr("Безлимит"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.testTag("subscription-quota-summary"))
                    if(subscription.total>0)LinearProgressIndicator(progress={usage.fraction},modifier=Modifier.fillMaxWidth().height(3.dp),color=MaterialTheme.colorScheme.primary,trackColor=MaterialTheme.colorScheme.surfaceContainerHighest)
                    if(subscription.expire>0)Text(tr("До {0}",DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(subscription.expire*1000))),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                IconButton(onClick={qr=true},enabled=subscription.url.isNotBlank(),modifier=Modifier.testTag("subscription-share")){Icon(Icons.Default.QrCode,tr("Ссылка и QR-код"),Modifier.size(20.dp))}
                IconButton(onClick=onUpdate,modifier=Modifier.testTag("subscription-refresh")){Icon(Icons.Default.Refresh,tr("Обновить подписку"),Modifier.size(20.dp))}
            }
        }
    }
    if(qr)SubscriptionQrDialog(subscription){qr=false}
    if(info)AlertDialog(onDismissRequest={info=false},title={Text(tr("Информация о подписке"))},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text(host);if(subscription.lastUpdated.isNotBlank())Text(tr("Обновлено {0}",subscription.lastUpdated));Text(tr("Использовано: {0}",displayTraffic(usage.used)));if(subscription.announcement.isNotBlank())Text(subscription.announcement)}},confirmButton={TextButton(onClick={info=false}){Text(tr("Закрыть"))}})
    if(remove)AlertDialog(onDismissRequest={remove=false},title={Text(tr("Удалить подписку?"))},text={Text(tr("«{0}» и её серверы будут удалены из приложения. Серверы, добавленные вручную, останутся.",subscription.name))},confirmButton={TextButton(onClick={remove=false;onClear()}){Text(tr("Удалить"),color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={remove=false}){Text(tr("Отмена"))}})
}
