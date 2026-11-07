package com.xalane.vpn.data

import android.content.Context
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Identity(val deviceId: String, val token: String, val privateKey: String)

/**
 * The phone's anonymous identity with the Xalane API: a WireGuard key pair made on the device
 * plus the token the API returned. Stored in app-private storage; the private key is never sent.
 */
class DeviceIdentity(context: Context) {
    private val file = File(context.filesDir, "identity.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()

    suspend fun get(api: XalaneApi): Identity = lock.withLock {
        load() ?: register(api)
    }

    /** Drops the stored identity, for example after the API no longer recognises the token. */
    suspend fun reset() = lock.withLock { withContext(Dispatchers.IO) { file.delete() } }

    suspend fun current(): Identity? = lock.withLock { load() }

    private suspend fun load(): Identity? = withContext(Dispatchers.IO) {
        if (!file.exists()) null else runCatching { json.decodeFromString<Identity>(file.readText()) }.getOrNull()
    }

    private suspend fun register(api: XalaneApi): Identity {
        val keys = KeyPair()
        val r = api.register(keys.publicKey.toBase64())
        val identity = Identity(r.deviceId, r.token, keys.privateKey.toBase64())
        withContext(Dispatchers.IO) { file.writeText(json.encodeToString(Identity.serializer(), identity)) }
        return identity
    }
}
