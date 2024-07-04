package org.zrnq.mcmotd.data

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import org.zrnq.mcmotd.genericLogger
import java.io.File

@Serializable
class McMotdStandaloneConfig : McMotdConfig {
    override var fontPath = ""
    override var fontName = "Microsoft YaHei"
    override var showTrueAddress = false
    override var showServerVersion = false
    override var showPlayerList = true
    override var dnsServerList = mutableListOf("223.5.5.5", "8.8.8.8")
    override var recordOnlinePlayer = mutableListOf<String>()
    override var recordInterval = 300
    override var recordLimit = 21600
    override var background = "#000000"

    override var httpServerPort = 0
    override var httpServerMapping = mutableMapOf<String, String>()
    override var httpServerParallelRequest = 32
    override var httpServerRequestCoolDown = 3000
    override var httpServerAccessRecordRefresh = 0

    fun save() {
        val file = File(savefile)
        file.writeText(Yaml.default.encodeToString(serializer(), this))
    }

    companion object {
        private const val savefile = "mcmotd.yml"
        fun load() : McMotdStandaloneConfig {
            val file = File(savefile)
            if(file.exists()) {
                try {
                    return Yaml.default.decodeFromString(serializer(), file.readText())
                } catch (e : Exception) {
                    genericLogger.error("Error reading config file", e)
                }
            }
            val newInstance = McMotdStandaloneConfig()
            newInstance.save()
            return newInstance
        }
    }
}