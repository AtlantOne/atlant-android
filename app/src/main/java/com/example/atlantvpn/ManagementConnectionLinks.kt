package com.example.atlantvpn

import android.net.Uri
import org.json.JSONObject

/** Profiles use applied listener keys, never the global key or pending edits. */
internal fun managementConnectionLinks(credentialLink:String,catalog:JSONObject,refs:List<JSONObject>?):List<Pair<String,String>> {
    val credentials=Uri.parse(credentialLink)
    require(credentials.scheme=="atlant")
    val user=requireNotNull(credentials.getQueryParameter("user"))
    val pbk=requireNotNull(credentials.getQueryParameter("pbk"))
    require(user.isNotBlank()&&pbk.length==64)
    val serverId=catalog.getString("server_id")
    val allowed=refs?.filter{it.optString("server_id")==serverId}?.map{it.getString("inbound_id")}?.toSet()
    val records=catalog.getJSONArray("inbounds")
    return (0 until records.length()).mapNotNull {index ->
        val record=records.getJSONObject(index);val spec=record.getJSONObject("settings")
        val id=spec.getString("id")
        if(!spec.optBoolean("enabled")||(allowed!=null&&id !in allowed))return@mapNotNull null
        val address=spec.optString("advertise_addr")
        if(address.isBlank())return@mapNotNull null
        val transports=spec.getJSONArray("transports").let{a->(0 until a.length()).map{a.getString(it)}}
        val transport=when {"tcp" in transports->"reality";"quic" in transports->"quic";else->"xhttp"}
        val title=spec.optString("name").ifBlank{id}
        val uri=Uri.Builder().scheme("atlant").encodedAuthority(address)
            .appendQueryParameter("sni",spec.getString("sni"))
            .appendQueryParameter("user",user).appendQueryParameter("pbk",pbk)
            .appendQueryParameter("server_pub_key",record.getString("server_pub_key"))
            .appendQueryParameter("wire_version",spec.getInt("wire_version").toString())
            .appendQueryParameter("server_id",serverId).appendQueryParameter("inbound_id",id)
            .appendQueryParameter("transport",transport).appendQueryParameter("transports",transports.joinToString(","))
        spec.optString("xhttp_path").takeIf{it.isNotBlank()}?.let{uri.appendQueryParameter("path",it)}
        title to uri.fragment("$user · $title").build().toString()
    }
}
