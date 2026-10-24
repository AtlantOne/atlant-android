package com.example.atlantvpn

import android.graphics.Bitmap
import androidx.compose.foundation.layout.requiredSize
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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class SubscriptionGroupLayoutTest(private val dark: Boolean, private val large: Boolean) {
    @get:Rule val compose = createComposeRule()

    @Test fun groupCollapsesAndActionsDoNotOverlap() {
        val first = ServerProfile("one", "Москва — сервер с длинным названием", "192.0.2.1:1026", "example.com", "key", "alice", "public", isSubscription = true)
        val second = first.copy(id = "two", name = "Берлин", server = "192.0.2.2:1026")
        val manual = first.copy(id = "manual", name = "Мой сервер", isSubscription = false)
        val subscription = SubscriptionInfo(url = "https://example.com/sub/test", name = "Подписка с длинным названием и несколькими серверами",
            count = 2, remainingGB = "Безлимит", lastUpdated = "18.10.2026 19:30", expire = 1893456000,
            usageScope = "local-account")
        var refreshed = 0
        var removed = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.5f else 1f)) {
                AtlantVPNTheme(darkTheme = dark) {
                    Surface(Modifier.requiredSize(320.dp, 568.dp)) {
                        SubscriptionServerList(listOf(first, second, manual), first, subscription,
                            {}, {}, {}, {}, {}, { refreshed++ }, {}, { removed++ })
                    }
                }
            }
        }
        compose.onNodeWithTag("subscription-server:one").assertIsDisplayed()
        val toggle = compose.onNodeWithTag("subscription-toggle").fetchSemanticsNode().boundsInRoot
        val refresh = compose.onNodeWithTag("subscription-refresh").fetchSemanticsNode().boundsInRoot
        val actions = compose.onNodeWithTag("subscription-actions").fetchSemanticsNode().boundsInRoot
        assertTrue("Expand and refresh targets overlap", toggle.right <= refresh.left)
        assertTrue("Refresh and menu targets overlap", refresh.right <= actions.left)
        compose.onNodeWithTag("subscription-toggle").performClick()
        compose.onNodeWithTag("subscription-server:one").assertDoesNotExist()
        compose.onNodeWithText("Мой сервер").assertIsDisplayed()
        compose.onNodeWithTag("subscription-toggle").performClick()
        compose.onNodeWithTag("subscription-server:one").assertIsDisplayed()
        compose.onNodeWithTag("subscription-refresh").performClick()
        assertEquals(1, refreshed)
        compose.onNodeWithTag("subscription-actions").performClick()
        compose.onNodeWithText("Удалить подписку").performClick()
        compose.onNodeWithText("Удалить подписку?").assertIsDisplayed()
        assertEquals("Deletion needs confirmation", 0, removed)
        compose.onNodeWithText("Отмена").performClick()
        assertEquals(0, removed)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "ui-proof").apply { mkdirs() }
        File(folder, "subscription-dark=$dark-large=$large.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun serversStayJoinedInsideSubscriptionAndGroupsRemainCompact() {
        val first = ServerProfile("gap-one", "One", "192.0.2.1:1026", "example.com", "key", "alice", "public", isSubscription = true, subscriptionId = "sub-one")
        val second = first.copy(id = "gap-two", subscriptionId = "sub-two")
        val sibling = first.copy(id = "gap-sibling", name = "Sibling")
        val subscriptions = listOf(
            SubscriptionInfo(id = "sub-one", url = "https://example.com/sub/one", name = "One"),
            SubscriptionInfo(id = "sub-two", url = "https://example.com/sub/two", name = "Two"))
        compose.setContent {
            AtlantVPNTheme(darkTheme = dark) {
                Surface(Modifier.requiredSize(320.dp, 568.dp)) {
                    SubscriptionServerList(listOf(first, sibling, second), first, subscriptions.first(), {}, {}, {}, {}, {}, {}, {}, {}, subscriptions = subscriptions)
                }
            }
        }
        val firstCard = compose.onNodeWithTag("subscription-server:gap-one").fetchSemanticsNode().boundsInRoot
        val siblingCard = compose.onNodeWithTag("subscription-server:gap-sibling").fetchSemanticsNode().boundsInRoot
        val headers = compose.onAllNodesWithTag("subscription-toggle").fetchSemanticsNodes()
        assertEquals(2, headers.size)
        assertEquals("Servers in one subscription must have no outer gap", firstCard.bottom, siblingCard.top, 1f)
        assertTrue("Different subscriptions remain separate", siblingCard.bottom < headers[1].boundsInRoot.top)
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}-large={1}")
        fun cases(): List<Array<Any>> = listOf(false, true).flatMap { dark -> listOf(false, true).map { arrayOf<Any>(dark, it) } }
    }
}
