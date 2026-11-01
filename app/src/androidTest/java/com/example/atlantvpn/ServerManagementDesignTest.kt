package com.example.atlantvpn

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.atlantvpn.ui.theme.AtlantVPNTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class ServerManagementDesignTest(private val dark:Boolean,private val large:Boolean) {
    @get:Rule val compose=createComposeRule()

    @Test fun overviewHasWorkingPrimaryActionAndNavigation() {
        var service="";var destination=""
        val status=JSONObject().put("total_users",12).put("active_connections",4).put("total_up_bytes",2147483648L).put("total_down_bytes",8589934592L)
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if(large)1.5f else 1f)) {
                AtlantVPNTheme(darkTheme=dark) {
                    Surface(Modifier.requiredSize(360.dp,760.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {ServerOverviewPanel(status,JSONObject().put("active","active"),{destination=it},{service=it})}
                    }
                }
            }
        }
        compose.onNodeWithTag("admin-service-toggle").performClick();assertEquals("stop",service)
        compose.onNodeWithTag("admin-destination-subscriptions").performScrollTo().performClick();assertEquals("subscriptions",destination)
        compose.onNodeWithTag("admin-destination-certificates").performScrollTo().performClick();assertEquals("certificates",destination)
        capture("overview")
    }

    @Test fun subscriptionKeepsItsServersAndUsesASeparateActionRow() {
        val sub=SubscriptionInfo(id="demo",url="https://example.com/sub/demo",name="Личная подписка",total=21474836480L,upload=1073741824L,download=4294967296L,expire=1893456000)
        val server=ServerProfile("s1","Основной сервер","192.0.2.1:1026","example.com","","","",pingMs=24,isSubscription=true,subscriptionId="demo")
        var refresh=0
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if(large)1.5f else 1f)) {
                AtlantVPNTheme(darkTheme=dark) {Surface(Modifier.requiredSize(360.dp,760.dp)){Box(Modifier.padding(16.dp)){SubscriptionServerList(listOf(server,server.copy(id="s2",name="Резервный сервер",pingMs=38)),server,sub,{},{},{},{},{},{refresh++},{},{})}}}
            }
        }
        compose.onNodeWithTag("subscription-server:s1").assertIsDisplayed()
        compose.onNodeWithTag("subscription-refresh").performClick();assertEquals(1,refresh)
        compose.onNodeWithTag("subscription-toggle").performClick();compose.onNodeWithTag("subscription-server:s1").assertDoesNotExist()
        compose.onNodeWithTag("subscription-toggle").performClick();compose.onNodeWithTag("subscription-server:s1").assertIsDisplayed()
        capture("subscription")
    }

    @Test fun stoppedServerStillOffersItsStartActionWithoutCoreStatistics() {
        var requested=""
        compose.setContent {AtlantVPNTheme(darkTheme=dark){Surface {Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)){ServerOverviewPanel(JSONObject(),JSONObject().put("active","inactive"),{},{requested=it})}}}}
        compose.onNodeWithTag("admin-service-toggle").performClick()
        assertEquals("start",requested)
    }

    @Test fun unlimitedTrafficUsesOnlyACompactSummary() {
        val sub=SubscriptionInfo(id="compact",name="Подписка",url="https://example.com/sub/test")
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if(large)1.5f else 1f)) {
                AtlantVPNTheme(darkTheme=dark){Surface(Modifier.requiredSize(360.dp,760.dp)){SubscriptionServerList(emptyList(),null,sub,{},{},{},{},{},{},{},{})}}
            }
        }
        val density=InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val summary=compose.onNodeWithTag("subscription-quota-summary").fetchSemanticsNode().boundsInRoot
        val header=compose.onNodeWithTag("subscription-header").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(summary.height/density<=if(large)32f else 24f)
        org.junit.Assert.assertTrue(header.height/density<=if(large)180f else 144f)
        capture("subscription-unlimited-compact")
    }

    private fun capture(name:String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val folder=File(context.getExternalFilesDir(null),"ui-proof").apply{mkdirs()}
        File(folder,"$name-dark=$dark-large=$large.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name="dark={0}-large={1}")
        fun cases():List<Array<Any>> = listOf(false,true).flatMap{dark->listOf(false,true).map{arrayOf<Any>(dark,it)}}
    }
}
