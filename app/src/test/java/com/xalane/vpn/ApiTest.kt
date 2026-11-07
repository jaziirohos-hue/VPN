package com.xalane.vpn

import com.wireguard.config.Config
import com.wireguard.crypto.KeyPair
import com.xalane.vpn.data.ApiException
import com.xalane.vpn.data.ConnectResponse
import com.xalane.vpn.data.XalaneApi
import com.xalane.vpn.data.wireGuardConfigText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RoboConfig

@RunWith(RobolectricTestRunner::class)
@RoboConfig(sdk = [34])
class ApiTest {
    // Exactly what the backend returns from POST /v1/connect
    private val sample = """
        {"server_id":"de-fra-1","interface":{"address":"10.8.0.2/32","dns":["1.1.1.1","1.0.0.1"]},
         "peer":{"public_key":"E/xzS1mSn8G36E0zEcsyKzgeMF3oCg/r9u4vlpW6/P8=","endpoint":"203.0.113.10:51820",
         "allowed_ips":["0.0.0.0/0","::/0"],"persistent_keepalive":25}}
    """.trimIndent()

    @Test fun connectResponseBecomesWireGuardConfig() {
        val r = Json { ignoreUnknownKeys = true }.decodeFromString<ConnectResponse>(sample)
        val keys = KeyPair()
        val config = Config.parse(wireGuardConfigText(keys.privateKey.toBase64(), r).byteInputStream())
        assertEquals(keys.publicKey, config.`interface`.keyPair.publicKey)
        assertEquals("10.8.0.2/32", config.`interface`.addresses.single().toString())
        assertEquals(2, config.`interface`.dnsServers.size)
        val peer = config.peers.single()
        assertEquals("203.0.113.10", peer.endpoint.get().host)
        assertEquals(51820, peer.endpoint.get().port)
        // Java prints ::/0 in long form
        assertEquals(setOf("0.0.0.0/0", "0:0:0:0:0:0:0:0/0"), peer.allowedIps.map { it.toString() }.toSet())
    }

    /** Runs against a live backend when XALANE_TEST_API and XALANE_TEST_SERVER are set. */
    @Test fun liveBackend() = runBlocking {
        val url = System.getenv("XALANE_TEST_API")
        val serverId = System.getenv("XALANE_TEST_SERVER")
        assumeTrue("set XALANE_TEST_API to run", !url.isNullOrBlank() && !serverId.isNullOrBlank())
        val api = XalaneApi(url!!)
        assertTrue(api.servers().any { it.id == serverId && it.online })
        val keys = KeyPair()
        val reg = api.register(keys.publicKey.toBase64())
        val r = api.connect(reg.token, serverId!!)
        val config = Config.parse(wireGuardConfigText(keys.privateKey.toBase64(), r).byteInputStream())
        assertEquals(serverId, r.serverId)
        assertTrue(config.`interface`.addresses.single().toString().startsWith("10.8."))
        val err = runCatching { api.connect("wrong-token", serverId) }.exceptionOrNull()
        assertTrue(err is ApiException && err.code == 401)
        api.deleteDevice(reg.token)
    }
}
