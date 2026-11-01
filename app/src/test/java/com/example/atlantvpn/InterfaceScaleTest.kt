package com.example.atlantvpn

import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceScaleTest {
    @Test fun storedScaleCannotMakeTheInterfaceUnusable() {
        assertEquals(1f,normalizedInterfaceScale(Float.NaN))
        assertEquals(1f,normalizedInterfaceScale(Float.POSITIVE_INFINITY))
        assertEquals(MIN_INTERFACE_SCALE,normalizedInterfaceScale(-1f))
        assertEquals(MAX_INTERFACE_SCALE,normalizedInterfaceScale(100f))
        assertEquals(.95f,normalizedInterfaceScale(.95f))
    }
}
