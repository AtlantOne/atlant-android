package com.example.atlantvpn

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ManagementConnectionLinksTest {
    @Test fun linksUseAppliedInboundKeysAndRespectSubscriptionSelection() {
        val sid="d".repeat(32)
        val catalog=JSONObject().put("server_id",sid).put("inbounds",JSONArray().apply{
            for((id,key)in listOf("one" to "1".repeat(64),"two" to "2".repeat(64)))
                put(JSONObject().put("server_pub_key",key).put("settings",JSONObject()
                    .put("id",id).put("name",id).put("enabled",true).put("sni","example.com")
                    .put("advertise_addr","192.0.2.1:1026").put("wire_version",7)
                    .put("transports",JSONArray(listOf("tcp","quic","xhttp"))).put("xhttp_path","/sync")))
        })
        val credential="atlant://192.0.2.1:1026?user=alice&pbk="+"a".repeat(64)+"&server_pub_key="+"f".repeat(64)
        val all=managementConnectionLinks(credential,catalog,null)
        assertEquals(2,all.size)
        assertEquals("1".repeat(64),Uri.parse(all[0].second).getQueryParameter("server_pub_key"))
        val links=managementConnectionLinks(credential,catalog,listOf(JSONObject().put("server_id",sid).put("inbound_id","two")))
        assertEquals(1,links.size)
        val parsed=Uri.parse(links[0].second)
        assertEquals("2".repeat(64),parsed.getQueryParameter("server_pub_key"))
        assertEquals("alice",parsed.getQueryParameter("user"))
        assertEquals("two",parsed.getQueryParameter("inbound_id"))
        assertEquals("tcp,quic,xhttp",parsed.getQueryParameter("transports"))
        assertEquals("/sync",parsed.getQueryParameter("path"))
        assertTrue(managementConnectionLinks(credential,catalog,emptyList()).isEmpty())
    }
}
