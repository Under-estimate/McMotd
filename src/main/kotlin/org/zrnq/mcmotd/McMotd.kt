package org.zrnq.mcmotd

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info
import org.zrnq.mclient.FONT
import java.awt.Font
import java.awt.GraphicsEnvironment

object McMotd : KotlinPlugin(
    JvmPluginDescription(
        id = "org.zrnq.mcmotd",
        name = "Minecraft MOTD Fetcher",
        version = "1.1.0",
    ) {
        author("ZRnQ")
        info("""以图片的形式获取指定Minecraft服务器的基本信息""")
    }
) {
    override fun onEnable() {
        logger.info { "McMotd is loading" }
        PluginConfig.reload()
        PluginData.reload()
        QueryCommand.register()
        BindCommand.register()
        DelCommand.register()
        val fontList = mutableListOf<Font>()
        for(f in GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts) {
            if(f.name == PluginConfig.fontName) {
                FONT = f.deriveFont(20f)
                return
            }
            if(f.canDisplay('啊')) {
                fontList.add(f)
            }
        }
        logger.warning("找不到指定的字体 : ${PluginConfig.fontName}，您可以在mcmotd.yml中修改字体名称")
        FONT = if(fontList.isEmpty()) {
            logger.error("找不到可用的字体, 请检查您的系统是否安装了中文字体")
            Font(PluginConfig.fontName, Font.PLAIN, 20)
        } else {
            logger.info("检测到可用的字体列表: ${fontList.joinToString(",") { it.name }}")
            logger.warning("正在使用第一个可用的字体: ${fontList[0].name}")
            fontList[0].deriveFont(20f)
        }
    }

    override fun onDisable() {
        logger.info { "McMotd is unloading" }
        QueryCommand.unregister()
        BindCommand.unregister()
        DelCommand.register()
    }
}

