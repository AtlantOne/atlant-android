package com.example.atlantvpn

import org.junit.Assert.assertEquals
import org.junit.Test

class TransportModesTest {
    @Test fun tcpModeCarriesBothPayloadTypesOverTcp() {
        val settings = AppSettings(trafficMode = TrafficMode.TCP, udpProto = "quic")
        assertEquals("reality" to "tcp", settings.transportModes())
        assertEquals("xhttp" to "tcp", settings.copy(tcpProto = "xhttp").transportModes())
    }

    @Test fun udpModeOverridesTcpProfileTransport() {
        val settings = AppSettings(trafficMode = TrafficMode.UDP, tcpProto = "xhttp")
        assertEquals("quic" to "quic", settings.transportModes())
    }

    @Test fun splitModePreservesIndependentChoices() {
        val settings = AppSettings(trafficMode = TrafficMode.SPLIT, tcpProto = "xhttp", udpProto = "quic")
        assertEquals("xhttp" to "quic", settings.transportModes())
    }
}
