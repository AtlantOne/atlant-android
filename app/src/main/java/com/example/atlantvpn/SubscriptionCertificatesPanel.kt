package com.example.atlantvpn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.net.IDN
import java.net.URI

internal fun subscriptionHttpsSettings(previous:JSONObject,mode:String,hostInput:String,portInput:String,certFile:String,keyFile:String,accepted:Boolean,enabled:Boolean):JSONObject {
    if(!enabled)return JSONObject().put("enabled",false).put("listen",previous.optString("listen")).put("public_url",previous.optString("public_url")).put("tls",previous.optJSONObject("tls")?:JSONObject())
    require(mode in setOf("acme-domain","acme-ip","manual"))
    val raw=hostInput.trim().removePrefix("[").removeSuffix("]")
    require(raw.isNotBlank()&&!raw.contains('/')&&!raw.contains('@')&&!raw.any{it.isWhitespace()}){"Укажите домен или IP"}
    val host=if(':' in raw)raw.lowercase() else IDN.toASCII(raw).lowercase()
    val port=portInput.toIntOrNull()?.takeIf{it in 1..65535}?:error("Укажите порт от 1 до 65535")
    require(mode=="manual"||accepted){"Примите условия Let’s Encrypt"}
    require(mode!="manual"||(certFile.isNotBlank()&&keyFile.isNotBlank())){"Укажите файлы сертификата и ключа"}
    val tls=JSONObject((previous.optJSONObject("tls")?:JSONObject()).toString())
        .put("mode",mode).put("host",host).put("accept_terms",accepted).put("staging",false)
    if(mode=="manual"){
        tls.put("cert_file",certFile.trim()).put("key_file",keyFile.trim())
        tls.remove("cache_dir");tls.remove("challenge_listen");tls.remove("challenge_dir")
    }else{
        tls.remove("cert_file");tls.remove("key_file")
        if(tls.optString("cache_dir").isBlank())tls.put("cache_dir","/data/subscription-tls")
    }
    val origin=URI("https",null,host,if(port==443)-1 else port,null,null,null).toASCIIString()
    return JSONObject().put("enabled",enabled).put("listen","0.0.0.0:$port").put("public_url",origin).put("tls",tls)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubscriptionCertificatesPanel(settings:JSONObject,busy:Boolean,onSave:(JSONObject)->Unit,onRestart:()->Unit) {
    val tls=settings.optJSONObject("tls")?:JSONObject()
    val snapshot=settings.toString()
    var mode by remember(snapshot){mutableStateOf(tls.optString("mode").ifBlank{"acme-domain"})}
    var host by remember(snapshot){mutableStateOf(tls.optString("host"))}
    var port by remember(snapshot){mutableStateOf(settings.optString("listen").substringAfterLast(':').ifBlank{"443"})}
    var cert by remember(snapshot){mutableStateOf(tls.optString("cert_file"))}
    var key by remember(snapshot){mutableStateOf(tls.optString("key_file"))}
    var accepted by remember(snapshot){mutableStateOf(tls.optBoolean("accept_terms"))}
    var enabled by remember(snapshot){mutableStateOf(settings.optBoolean("enabled",true))}
    var error by remember{mutableStateOf("")}
    val status=settings.optJSONObject("status")?:JSONObject()
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){
            Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(tr("HTTPS для подписок"),style=MaterialTheme.typography.titleMedium)
                Text(tr(if(status.optBoolean("listening"))"HTTPS работает" else "HTTPS не запущен"),style=MaterialTheme.typography.bodyMedium)
                settings.optString("public_url").takeIf{it.isNotBlank()}?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
                status.optString("not_after").takeIf{it.isNotBlank()}?.let{Text(tr("Действует до")+": "+it,style=MaterialTheme.typography.bodySmall)}
                status.optString("error").takeIf{it.isNotBlank()}?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
                Text(tr("Сертификат используется для HTTPS-подписок. VPN продолжает работать через REALITY."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(tr("Включить HTTPS-подписки"));Switch(checked=enabled,onCheckedChange={enabled=it},enabled=!busy)}
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            for((id,label)in listOf("acme-domain" to "Домен","acme-ip" to "IP","manual" to "Свой сертификат"))
                FilterChip(selected=mode==id,onClick={mode=id},enabled=!busy,label={Text(tr(label))})
        }
        OutlinedTextField(host,{host=it},Modifier.fillMaxWidth(),enabled=!busy,label={Text(tr(if(mode=="acme-ip")"IP-адрес" else "Домен или IP"))},singleLine=true,shape=RoundedCornerShape(16.dp))
        OutlinedTextField(port,{port=it},Modifier.fillMaxWidth(),enabled=!busy,label={Text(tr("Порт HTTPS-подписок"))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),shape=RoundedCornerShape(16.dp))
        if(mode=="manual"){
            OutlinedTextField(cert,{cert=it},Modifier.fillMaxWidth(),enabled=!busy,label={Text(tr("Файл сертификата на сервере"))},singleLine=true,shape=RoundedCornerShape(16.dp))
            OutlinedTextField(key,{key=it},Modifier.fillMaxWidth(),enabled=!busy,label={Text(tr("Файл ключа на сервере"))},singleLine=true,shape=RoundedCornerShape(16.dp))
        }else{
            Text(tr("Сертификат выпускается и обновляется автоматически. Для HTTP-01 нужен доступ к порту 80."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(accepted,{accepted=it},enabled=!busy);Text(tr("Принимаю условия Let’s Encrypt"),style=MaterialTheme.typography.bodySmall)}
        }
        if(error.isNotBlank())Text(tr(error),color=MaterialTheme.colorScheme.error)
        Button(enabled=!busy,onClick={try {val patch=subscriptionHttpsSettings(settings,mode,host,port,cert,key,accepted,enabled);error="";onSave(patch)}catch(e:Exception){error=e.message.orEmpty()}},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text(tr("Сохранить"))}
        OutlinedButton(enabled=!busy,onClick=onRestart,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text(tr("Применить и перезапустить VPN"))}
    }
}
