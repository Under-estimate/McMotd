package org.zrnq.mcmotd.data

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import org.zrnq.mcmotd.genericLogger
import java.io.File

@Serializable
class McMotdStandaloneData : McMotdData {
    override val relation = mutableMapOf<Long, MutableList<Pair<String, String>>>()
    override val history = mutableMapOf<String, MutableList<Pair<Long, Int>>>()

    fun save() {
        val file = File(savefile)
        file.writeText(Yaml.default.encodeToString(serializer(), this))
    }
    companion object {
        private const val savefile = "mcmotd_data.yml"
        fun load() : McMotdStandaloneData {
            val file = File(savefile)
            if(file.exists()) {
                try {
                    return Yaml.default.decodeFromString(serializer(), file.readText())
                } catch (e: Exception) {
                    genericLogger.error("Error reading data file", e)
                }
            }
            val newInstance = McMotdStandaloneData()
            newInstance.save()
            return newInstance
        }
    }
}