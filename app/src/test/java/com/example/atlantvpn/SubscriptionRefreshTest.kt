package com.example.atlantvpn

import org.junit.Assert.*
import org.junit.Test

class SubscriptionRefreshTest {
    private fun node(id: String, group: String) = ServerProfile(id, id, "example.com:443", "example.com", "key", "user", "pin", subscriptionId = group)

    @Test fun refreshKeepsPositionNameAndUpdatesUsage() {
        val first = SubscriptionInfo(id = "a", name = "Моё название")
        val second = SubscriptionInfo(id = "b", name = "Вторая")
        var saved = listOf(first, second)
        repeat(3) { index ->
            saved = refreshSubscriptionInfo(saved, SubscriptionInfo(id = "a", name = "Название сервера", upload = (index + 1).toLong()))
            assertEquals(listOf("a", "b"), saved.map { it.id })
            assertEquals(first.name, saved[0].name)
            assertEquals((index + 1).toLong(), saved[0].upload)
        }
    }

    @Test fun serverGroupStaysBetweenUnrelatedEntries() {
        val before = listOf(node("manual", ""), node("a1", "a"), node("b1", "b"), node("a2", "a"))
        val after = replaceSubscriptionProfiles(before, "a", listOf(node("new1", "a"), node("new2", "a")))
        assertEquals(listOf("manual", "new1", "new2", "b1"), after.map { it.id })
        assertEquals(before.filter { it.subscriptionId != "a" }, replaceSubscriptionProfiles(before, "a", emptyList()))
    }

    @Test fun newSubscriptionsAppendAndBlankNamesUseProviderTitle() {
        assertEquals("Provider", refreshSubscriptionInfo(listOf(SubscriptionInfo(id = "a")), SubscriptionInfo(id = "a", name = "Provider"))[0].name)
        assertEquals(listOf("a", "b"), refreshSubscriptionInfo(listOf(SubscriptionInfo(id = "a")), SubscriptionInfo(id = "b")).map { it.id })
    }
}
