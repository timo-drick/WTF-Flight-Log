package de.drick.flightlog.localStorage

import com.russhwolf.settings.Settings
import de.drick.core.log
import de.drick.flightlog.file.OSDSummeryData
import de.drick.flightlog.file.SrtSummeryData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AircraftIdentifier(
    val name: String
)

class AircraftIdentifierDB {
    private val storage = KeyValuePersistentStorage(
        version = 0,
        serializer = AircraftIdentifier.serializer(),
        prefs = Settings()
    )

    suspend fun addAircraft(aircraft: AircraftIdentifier) {
        storage.save(aircraft.name, aircraft)
    }
    suspend fun removeAircraft(aircraft: AircraftIdentifier) {
        storage.remove(aircraft.name)
    }
    suspend fun loadAll() = storage.loadAll()
}

object OsdSummeryDataCache: KeyValuePersistentStorage<OSDSummeryData>(
    version = 0,
    serializer = OSDSummeryData.serializer(),
    prefs = Settings()
)

object SrtSummeryDataCache: KeyValuePersistentStorage<SrtSummeryData>(
    version = 0,
    serializer = SrtSummeryData.serializer(),
    prefs = Settings()
)

open class KeyValuePersistentStorage<T>(
    val version: Int,
    private val serializer: KSerializer<T>,
    val prefs: Settings,
) {
    private val storageLock = Mutex()

    val json = Json

    val prefix = "${serializer.descriptor.serialName}:"

    private fun prefixKey(key: String) = "$prefix$key"

    suspend fun updateOrCreate(key: String, block: (T?) -> T) {
        storageLock.withLock {
            val oldEntry = loadInternal(key)
            val newEntry = block(oldEntry)
            save(key, newEntry)
        }
    }
    suspend fun save(key: String, value: T) {
        storageLock.withLock {
            saveInternal(key, value)
        }
    }
    suspend fun remove(key: String) {
        storageLock.withLock {
            removeInternal(key)
        }
    }
    suspend fun load(key: String): T? = storageLock.withLock {
        loadInternal(key)
    }
    suspend fun clear() {
        storageLock.withLock {
            clearInternal()
        }
    }
    suspend fun loadAll(): List<T> = storageLock.withLock {
        checkVersion()
        prefs.keys
            .filter { it.startsWith(prefix) }
            .mapNotNull {
                val key = it.substring(prefix.length)
                loadInternal(key)
            }
    }


    private fun saveInternal(key: String, value: T) {
        val jsonStr = json.encodeToString(serializer, value)
        checkVersion()
        prefs.putString(prefixKey(key), jsonStr)
    }

    private fun removeInternal(key: String) {
        checkVersion()
        prefs.remove(prefixKey(key))
    }

    private fun loadInternal(key: String): T? {
        return try {
            checkVersion()
            val jsonStr: String? = prefs.getStringOrNull(prefixKey(key))
            if (jsonStr != null)
                json.decodeFromString(serializer, jsonStr)
            else
                null
        } catch (err: Throwable) {
            log(err)
            null
        }
    }

    private fun clearInternal() {
        prefs.keys
            .filter { it.startsWith(prefix) }
            .forEach { prefs.remove(it) }
    }

    private val versionKey = "version:${serializer.descriptor.serialName}"

    /**
     * If the version of the saved db does not match the current version db will be cleared
     */
    private fun checkVersion() {
        val oldVersion = prefs.getInt(versionKey, 0)
        if (oldVersion != version) {
            clearInternal()
            prefs.putInt(versionKey, version)
        }
    }
}
