package org.zrnq.mcmotd

import org.zrnq.mcmotd.data.McMotdConfig
import org.zrnq.mcmotd.data.McMotdData
import org.zrnq.mcmotd.logging.GenericLogger

lateinit var configStorage: McMotdConfig
lateinit var dataStorage: McMotdData
lateinit var genericLogger: GenericLogger
val mcmotdVersion by lazy {
    ImageUtil::class.java.getResourceAsStream("/version.txt")?.reader()?.readText() ?: "??"
}