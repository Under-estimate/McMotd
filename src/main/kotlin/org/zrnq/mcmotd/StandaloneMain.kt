package org.zrnq.mcmotd

import com.alibaba.fastjson.parser.ParserConfig
import org.zrnq.mcmotd.data.McMotdStandaloneConfig
import org.zrnq.mcmotd.data.McMotdStandaloneData
import org.zrnq.mcmotd.http.HttpServer
import org.zrnq.mcmotd.logging.PrintLogger

fun main() {
    genericLogger = PrintLogger()
    genericLogger.info("McMotd $mcmotdVersion is running in Standalone mode.")

    ParserConfig.getGlobalInstance().isSafeMode = true

    configStorage = McMotdStandaloneConfig.load()
    dataStorage = McMotdStandaloneData.load()
    configStorage.checkConfig()

    PlayerHistory.startRecord()
    HttpServer.configureHttpServer()
}