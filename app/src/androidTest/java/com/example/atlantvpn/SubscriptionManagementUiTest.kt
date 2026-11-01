package com.example.atlantvpn

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.atlantvpn.ui.theme.AtlantVPNTheme
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SubscriptionManagementUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun selectedInboundsAreSavedAndRemainSelectedOnEditing() {
        val serverId="d".repeat(32)
        var subscription:JSONObject?=null
        var saved:JSONObject?=null
        var catalogReads=0
        val lock=Any()
        fun rpc(raw:String):String=synchronized(lock) {
            val command=JSONObject(raw).getString("command")
            val result:Any=when {
                command=="service status"->JSONObject().put("active","inactive")
                command=="groups"->JSONArray()
                command=="subscriptions"->JSONArray().apply{subscription?.let{put(JSONObject(it.toString()))}}
                command=="inbounds"->{
                    catalogReads++
                    JSONObject().put("server_id",serverId).put("revision",1).put("inbounds",JSONArray().apply{
                        for((id,name) in listOf("one" to "Основной вход","two" to "Запасной вход"))
                            put(JSONObject().put("settings",JSONObject().put("id",id).put("name",name).put("listen",":1026")))
                    })
                }
                command.startsWith("subscribercreate ")||command.startsWith("subset ")->{
                    saved=JSONObject(command.substring(command.indexOf('{')))
                    subscription=JSONObject(saved.toString()).put("id","a".repeat(32)).put("managed",true).put("revision","r1")
                    JSONObject().put("success",true)
                }
                else->error("Unexpected test operation: $command")
            }
            JSONObject().put("result",result).toString()
        }
        compose.setContent {AtlantVPNTheme(darkTheme=true){Surface{ServerManagementScreen("ui-subscription","Тестовый сервер",{},rpc={rpc(it)})}}}
        compose.onNodeWithTag("admin-destination-subscriptions").performScrollTo().performClick()
        compose.waitUntil(10000){synchronized(lock){catalogReads>=1}}
        compose.onNodeWithContentDescription("Добавить").performClick()
        compose.onNodeWithText("Основной вход · :1026").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Запасной вход · :1026").performScrollTo().assertIsDisplayed().performClick()
        compose.onNode(hasSetTextAction() and hasText("Название")).performScrollTo().performTextInput("Моя подписка")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10000){synchronized(lock){saved!=null}}
        synchronized(lock){assertEquals(2,saved!!.getJSONArray("inbound_refs").length())}
        compose.onNodeWithText("Изменить").performScrollTo().performClick()
        compose.onNodeWithText("Выбрано: 2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Запасной вход · :1026").performClick()
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10000){synchronized(lock){saved!!.getJSONArray("inbound_refs").length()==1}}
        synchronized(lock){
            val ref=saved!!.getJSONArray("inbound_refs").getJSONObject(0)
            assertEquals("one",ref.getString("inbound_id"))
            assertEquals(serverId,ref.getString("server_id"))
        }
    }
}
