package org.zrnq.mcmotd

import com.alibaba.fastjson.parser.ParserConfig
import net.mamoe.mirai.console.command.Command
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import org.zrnq.mcmotd.data.McMotdPluginConfig
import org.zrnq.mcmotd.data.McMotdPluginData
import org.zrnq.mcmotd.http.HttpServer
import org.zrnq.mcmotd.logging.MiraiLoggerAdapter

object McMotd : KotlinPlugin(
    JvmPluginDescription(
        id = "org.zrnq.mcmotd",
        name = "Minecraft MOTD Fetcher",
        version = mcmotdVersion,
    ) {
        author("ZRnQ")
        info("""以图片的形式获取指定Minecraft服务器的基本信息""")
    }
) {
    private lateinit var commandList : List<Command>
    override fun onEnable() {
        genericLogger = MiraiLoggerAdapter(logger)
        logger.info("McMotd is loading")

        ParserConfig.getGlobalInstance().isSafeMode = true

        McMotdPluginConfig.reload()
        McMotdPluginData.reload()
        configStorage = McMotdPluginConfig
        dataStorage = McMotdPluginData
        configStorage.checkConfig()

        QueryCommand.preparePermissions()

        commandList = listOf(QueryCommand, BindCommand, DelCommand, RecordCommand, HttpServerCommand, ConfigReloadCommand)
        commandList.forEach { it.register() }

        PlayerHistory.startRecord()
        HttpServer.configureHttpServer()
    }

    override fun onDisable() {
        logger.info("McMotd is unloading")
        commandList.forEach { it.unregister() }
        PlayerHistory.stopRecord()
        HttpServer.stopHttpServer()
    }
}
