package com.example.atlantvpn

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.example.atlantvpn.ui.theme.AtlantVPNTheme
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class GroupManagementUiTest {
    @get:Rule val compose=createComposeRule()

    private class Store(initial:Boolean=true) {
        private var group:JSONObject?=if(initial)JSONObject().put("id","a".repeat(32)).put("revision",1).put("name","Команда").put("note","").put("users",JSONArray(listOf("alice"))) else null
        private val users=JSONArray().put(JSONObject().put("name","alice").put("enabled",true)).put(JSONObject().put("name","bob").put("enabled",true)).put(JSONObject().put("name","charlie").put("enabled",true))
        private val subs=JSONArray().put(JSONObject().put("id","b".repeat(32)).put("user","alice").put("title","Личная подписка")).put(JSONObject().put("id","c".repeat(32)).put("user","bob").put("title","Рабочая подписка"))
        @Synchronized fun snapshot():JSONObject?=group?.let{JSONObject(it.toString())}
        @Synchronized fun call(raw:String):String {
            val request=JSONObject(raw);val command=request.getString("command")
            val result:Any=when {
                command=="service status"->JSONObject().put("active","inactive")
                command=="groups"->JSONArray().apply{group?.let{put(JSONObject(it.toString()))}}
                command=="usersall"->JSONObject().put("users",JSONArray(users.toString()))
                command=="subscriptions"->JSONArray(subs.toString())
                command.startsWith("groupset ")-> {
                    val patch=JSONObject(command.substring(command.indexOf('{')))
                    check(patch.getLong("revision")==group!!.getLong("revision"))
                    group=JSONObject(group.toString()).put("name",patch.getString("name")).put("note",patch.getString("note")).put("users",patch.getJSONArray("users")).put("revision",patch.getLong("revision")+1)
                    JSONObject().put("success",true)
                }
                command.startsWith("groupcreate ")-> {
                    val patch=JSONObject(command.substringAfter(' '));group=patch.put("id","a".repeat(32)).put("revision",1);JSONObject().put("success",true)
                }
                else->error("Unexpected test operation: $command")
            }
            return JSONObject().put("result",result).toString()
        }
    }

    private fun open(store:Store) {
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1f)) {
                AtlantVPNTheme(darkTheme=true){Surface{ServerManagementScreen("isolated-ui-test","Тестовый сервер",onBack={},rpc={store.call(it)})}}
            }
        }
        compose.onNodeWithTag("admin-destination-groups").performScrollTo().performClick()
    }

    @Test fun groupCanBeRenamedAndItsMembersChangedAndUsedAsAFilter() {
        val store=Store();open(store)
        compose.onNodeWithText("Настроить группу").performClick()
        compose.onNode(hasSetTextAction() and hasText("Название")).performTextReplacement("Дом")
        compose.onNodeWithText("Рабочая подписка").performScrollTo().performClick()
        capture("group-settings")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10000){store.snapshot()?.optString("name")=="Дом"&&store.snapshot()?.optJSONArray("users")?.length()==2}
        compose.onNodeWithText("Дом").assertIsDisplayed()
        compose.onNodeWithText("Настроить группу").performClick()
        compose.onNode(hasSetTextAction() and hasText("Название")).assertTextContains("Дом")
        compose.onNodeWithText("Личная подписка").performScrollTo().performClick()
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10000){store.snapshot()?.optJSONArray("users")?.toString()=="[\"bob\"]"}
        compose.onNodeWithContentDescription("Действия").performClick()
        compose.onNodeWithText("Показать пользователей").performClick()
        compose.onNodeWithText("Рабочая подписка").assertIsDisplayed()
        compose.onNodeWithText("Личная подписка").assertDoesNotExist()
        compose.onNodeWithText("charlie").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasContentDescription("Поиск")).performTextInput("Рабочая")
        compose.onNodeWithText("Рабочая подписка").assertIsDisplayed()
        val search=compose.onNodeWithTag("admin-search").fetchSemanticsNode().boundsInRoot
        val density=InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertTrue("Search should remain compact",search.height/density<=44f)
        capture("group-filter")
    }

    @Test fun emptyGroupsHaveAnExplicitCreateAction() {
        val store=Store(false);open(store)
        compose.onNodeWithText("Создать группу").performClick()
        compose.onNode(hasSetTextAction() and hasText("Название")).performTextReplacement("  Новая группа  ")
        compose.onNodeWithText("Рабочая подписка").performScrollTo().performClick()
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10000){store.snapshot()?.optString("name")=="Новая группа"}
        assertEquals("[\"bob\"]",store.snapshot()!!.getJSONArray("users").toString())
        compose.onNodeWithText("Новая группа").assertIsDisplayed()
    }

    private fun capture(name:String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val folder=File(context.getExternalFilesDir(null),"ui-proof").apply{mkdirs()}
        File(folder,"$name.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
}
