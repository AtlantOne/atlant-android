package com.example.atlantvpn

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SubscriptionCertificatesTest {
    @Test fun manualSelectionPreservesTheChosenPathsAndHttpsOrigin() {
        val value=subscriptionHttpsSettings(JSONObject(),"manual","vpn.example.com","8444","/data/custom.pem","/data/custom.key",false,true)
        assertEquals("https://vpn.example.com:8444",value.getString("public_url"))
        assertEquals("manual",value.getJSONObject("tls").getString("mode"))
        assertEquals("/data/custom.key",value.getJSONObject("tls").getString("key_file"))
        assertFalse(value.getJSONObject("tls").has("cache_dir"))
    }
    @Test fun autoModeHasRenewableCacheAndNoManualPrivateKeyPath() {
        val old=JSONObject().put("tls",JSONObject().put("cert_file","/old.pem").put("key_file","/old.key"))
        val value=subscriptionHttpsSettings(old,"acme-domain","EXAMPLE.COM","443","","",true,true)
        assertEquals("https://example.com",value.getString("public_url"))
        assertEquals("acme-domain",value.getJSONObject("tls").getString("mode"))
        assertEquals("/data/subscription-tls",value.getJSONObject("tls").getString("cache_dir"))
        assertFalse(value.getJSONObject("tls").has("key_file"))
        assertEquals("/old.key",old.getJSONObject("tls").getString("key_file"))
    }
    @Test(expected=IllegalArgumentException::class) fun autoIssueRequiresExplicitAcceptance() {
        subscriptionHttpsSettings(JSONObject(),"acme-domain","example.com","443","","",false,true)
    }
    @Test fun disableDoesNotRequireNewDomainOrReplaceCertificateSettings() {
        val old=JSONObject().put("public_url","https://example.com").put("tls",JSONObject().put("mode","manual").put("key_file","/chosen.key"))
        val value=subscriptionHttpsSettings(old,"acme-ip","","","","",false,false)
        assertFalse(value.getBoolean("enabled"))
        assertEquals("/chosen.key",value.getJSONObject("tls").getString("key_file"))
    }
}
