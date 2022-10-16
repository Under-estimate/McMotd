package org.zrnq.mclient

import org.zrnq.mcmotd.McMotd
import org.zrnq.mcmotd.PluginConfig
import java.awt.Font
import java.awt.GraphicsEnvironment

object MClientOptions {
    lateinit var FONT : Font
    var dnsServerList = listOf("223.5.5.5", "8.8.8.8")
    var showTrueAddress = false
    var recordInterval = 300

    fun loadPluginConfig() {
        dnsServerList = if(PluginConfig.dnsServerList.isEmpty()) {
            McMotd.logger.warning("配置文件中没有填写DNS服务器地址，正在使用默认的DNS服务器")
            listOf("223.5.5.5", "8.8.8.8")
        } else PluginConfig.dnsServerList
        showTrueAddress = PluginConfig.showTrueAddress
        recordInterval = PluginConfig.recordInterval

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