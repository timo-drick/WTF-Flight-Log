package de.drick.flightlog.localStorage

import com.russhwolf.settings.Settings
import de.drick.core.log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer


@Serializable
data class AircraftIdentifier(
    val name: String
)

class AircraftIdentifierDB() {
    private val settings = Settings()
    private val storage = KeyValuePersistentStorage(
        serializer = AircraftIdentifier.serializer(),
        prefs = settings
    )

    fun addAircraft(aircraft: AircraftIdentifier) {
        storage.save(aircraft.name, aircraft)
    }
    fun removeAircraft(aircraft: AircraftIdentifier) {
        storage.remove(aircraft.name)
    }
    fun loadAll() = storage.loadAll()
}


class KeyValuePersistentStorage<T>(
    private val serializer: KSerializer<T>,
    val prefs: Settings,
) {
    companion object {
        inline fun<reified T> newInstance(prefs: Settings) =
            KeyValuePersistentStorage<T>(serializer(), prefs)
    }

    val json = Json

    val prefix = "${serializer.descriptor.serialName}:"

    private fun prefixKey(key: String) = "$prefix$key"

    fun updateOrCreate(key: String, block: (T?) -> T) {
        val oldEntry = load(key)
        val newEntry = block(oldEntry)
        save(key, newEntry)
    }

    fun save(key: String, value: T) {
        val jsonStr = json.encodeToString(serializer, value)
        prefs.putString(prefixKey(key), jsonStr)
    }
    fun remove(key: String) {
        prefs.remove(prefixKey(key))
    }
    fun load(key: String): T? {
        return try {
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

    fun loadAll(): List<T> = prefs.keys.filter { it.startsWith(prefix) }
        .mapNotNull {
            val key = it.substring(prefix.length)
            load(key)
        }

    fun clear() {
        prefs.clear()
    }
}
