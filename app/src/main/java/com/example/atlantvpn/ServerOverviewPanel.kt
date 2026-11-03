package com.example.atlantvpn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
internal fun ServerOverviewPanel(status:JSONObject,service:JSONObject,onNavigate:(String)->Unit,onService:(String)->Unit,busy:Boolean=false) {
    val active=service.optString("active")=="active"
    Column(verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Surface(shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),color=MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {ManagementGlyph(Icons.Default.Dns,bright=active);ServerStatusPill(tr(if(active)"Запущен" else "Остановлен"),emphasized=active)}
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(tr(if(active)"Сервер работает" else "Сервер выключен"),style=MaterialTheme.typography.headlineSmall);Text(tr(if(active)"Управляйте доступом и подключениями" else "Запустите VPN, когда будете готовы"),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Button(enabled=!busy,onClick={onService(if(active)"stop" else "start")},shape=RoundedCornerShape(8.dp),modifier=Modifier.weight(1f).testTag("admin-service-toggle"),contentPadding=PaddingValues(vertical=14.dp)) {Icon(Icons.Default.PowerSettingsNew,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(tr(if(active)"Остановить" else "Запустить"))}
                    if(active)OutlinedIconButton(enabled=!busy,onClick={onService("restart")},shape=RoundedCornerShape(8.dp),modifier=Modifier.size(48.dp)) {Icon(Icons.Default.Refresh,tr("Перезапустить"))}
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth()) {ManagementMetric(tr("Пользователи"),if(status.has("total_users")||status.has("users"))status.optLong("total_users",status.optLong("users",0)).toString() else "—",Modifier.weight(1f));ManagementMetric(tr("Подключения"),if(status.has("active_connections"))status.optLong("active_connections").toString() else if(!active)"0" else "—",Modifier.weight(1f))}
            }
        }
        Text(tr("Управление"),style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(start=4.dp))
        Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ManagementDestination(tr("Подписки"),tr("Ссылки и лимиты"),Icons.Default.Link,{onNavigate("subscriptions")},Modifier.weight(1f).fillMaxHeight().testTag("admin-destination-subscriptions"))
            ManagementDestination(tr("Сертификаты"),tr("HTTPS для подписок"),Icons.Default.VerifiedUser,{onNavigate("certificates")},Modifier.weight(1f).fillMaxHeight().testTag("admin-destination-certificates"))
        }
        Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ManagementDestination(tr("Подключения"),tr("Порты и транспорты"),Icons.Default.Tune,{onNavigate("inbounds")},Modifier.weight(1f).fillMaxHeight())
            ManagementDestination(tr("Группы"),tr("Участники и доступ"),Icons.Default.Workspaces,{onNavigate("groups")},Modifier.weight(1f).fillMaxHeight().testTag("admin-destination-groups"))
        }
        Surface(shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),color=MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=16.dp)) {ManagementMetric(tr("Отправлено"),if(status.has("total_up_bytes"))displayTraffic(status.optLong("total_up_bytes")) else "—",Modifier.weight(1f));ManagementMetric(tr("Получено"),if(status.has("total_down_bytes"))displayTraffic(status.optLong("total_down_bytes")) else "—",Modifier.weight(1f))}
        }
    }
}
