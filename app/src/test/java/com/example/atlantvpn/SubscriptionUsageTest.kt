package com.example.atlantvpn

import org.junit.Assert.*
import org.junit.Test

class SubscriptionUsageTest {
    @Test fun quotaAndUnlimited() {
        assertEquals(0.5f, subscriptionUsage(20, 30, 100).fraction)
        assertEquals(50L, subscriptionUsage(20, 30, 100).remaining)
        assertEquals(0f, subscriptionUsage(20, 30, 0).fraction)
        assertEquals(1f, subscriptionUsage(200, 30, 100).fraction)
    }
    @Test fun counterOverflowAndNegative() {
        assertEquals(Long.MAX_VALUE, subscriptionUsage(Long.MAX_VALUE, 1, 100).used)
        assertEquals(0L, subscriptionUsage(-1, -1, 100).used)
    }
    @Test fun sameEndpointInTwoSubscriptionsStaysSeparate() {
        val p = ServerProfile("one", "one", "one:443", "example.com", "a", "u", "b", isSubscription = true)
        val groups = mergeSubscriptionEndpoints(listOf(p.copy(subscriptionId = "a"), p.copy(subscriptionId = "b")))
        assertEquals(2, groups.size)
        assertNotEquals(groups[0].id, groups[1].id)
        assertEquals(subscriptionId("https://example.com/a"), subscriptionId("https://example.com/a"))
        assertNotEquals(subscriptionId("https://example.com/a"), subscriptionId("https://example.com/b"))
    }
}
