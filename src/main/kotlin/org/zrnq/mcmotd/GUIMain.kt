package org.zrnq.mcmotd

import com.alibaba.fastjson.parser.ParserConfig
import org.zrnq.mcmotd.data.McMotdStandaloneConfig
import org.zrnq.mcmotd.data.McMotdStandaloneData
import org.zrnq.mcmotd.logging.PrintLogger
import org.zrnq.mcmotd.output.GUIOutputHandler

fun main() {
    genericLogger = PrintLogger()
    genericLogger.info("McMotd $mcmotdVersion is running in GUI mode. HTTP server & player history recording is disabled.")

    ParserConfig.getGlobalInstance().isSafeMode = true

    configStorage = McMotdStandaloneConfig.load()
    dataStorage = McMotdStandaloneData.load()
    configStorage.checkConfig()

    GUIOutputHandler()
}