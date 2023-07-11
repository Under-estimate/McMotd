package org.zrnq.mclient

import org.zrnq.mcmotd.McMotd
import org.zrnq.mcmotd.PluginConfig
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.File

object MClientOptions {
    lateinit var FONT : Font
    var dnsServerList = listOf("223.5.5.5", "8.8.8.8")
    var showTrueAddress = false
    var showServerVersion = false
    var showPlayerList = true

    fun loadPluginConfig() {
        dnsServerList = if(PluginConfig.dnsServerList.isEmpty()) {
            McMotd.logger.warning("配置文件中没有填写DNS服务器地址，正在使用默认的DNS服务器")
            listOf("223.5.5.5", "8.8.8.8")
        } else PluginConfig.dnsServerList
        showTrueAddress = PluginConfig.showTrueAddress
        showServerVersion = PluginConfig.showServerVersion
        showPlayerList = PluginConfig.showPlayerList

        if(PluginConfig.fontPath.isNotBlank()) {
            val fontFile = File(PluginConfig.fontPath)
            if(!fontFile.exists()) {
                McMotd.logger.warning("无法打开指定的字体文件: ${PluginConfig.fontPath}，请检查配置文件")
            } else {
                try {
                    val font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
                    if(!GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)) {
                        McMotd.logger.warning("注册字体文件到LocalGraphicsEnvironment时失败")
                    }
                    FONT = font.deriveFont(20f)
                    McMotd.logger.info("正在使用字体文件: ${PluginConfig.fontPath} (${font.fontName})")
                    return
                } catch (e : Exception) {
                    McMotd.logger.warning("读取字体文件: ${PluginConfig.fontPath}时出错", e)
                }
            }
        }

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
        McMotd.logger.warning("找不到指定的字体 : ${PluginConfig.fontName}，您可以在mcmotd.yml中修改字体名称")
        FONT = if(fontList.isEmpty()) {
            McMotd.logger.error("找不到可用的字体, 请检查您的系统是否安装了中文字体")
            Font(PluginConfig.fontName, Font.PLAIN, 20)
        } else {
            McMotd.logger.info("检测到可用的字体列表: ${fontList.joinToString(",") { it.name }}")
            McMotd.logger.warning("正在使用第一个可用的字体: ${fontList[0].name}")
            fontList[0].deriveFont(20f)
        }
    }
}