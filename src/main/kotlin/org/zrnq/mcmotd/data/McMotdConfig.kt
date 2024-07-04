package org.zrnq.mcmotd.data

import org.zrnq.mcmotd.genericLogger
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO

interface McMotdConfig {
    val fontPath : String
    val fontName : String
    val showTrueAddress : Boolean
    val showServerVersion : Boolean
    val showPlayerList : Boolean
    val dnsServerList : MutableList<String>
    val recordOnlinePlayer : MutableList<String>
    val recordInterval : Int
    val recordLimit : Int
    val background : String
    val showPeakPlayers: Boolean

    val httpServerPort : Int
    val httpServerMapping : MutableMap<String, String>
    val httpServerParallelRequest : Int
    val httpServerRequestCoolDown : Int
    val httpServerAccessRecordRefresh : Int

    fun checkConfig() {
        if(dnsServerList.isEmpty()) {
            genericLogger.warning("配置文件中没有填写DNS服务器地址，正在使用默认的DNS服务器")
            dnsServerList.add("223.5.5.5")
            dnsServerList.add("8.8.8.8")
        }

        if(background.matches(Regex("#[0-9a-fA-F]{6}"))) {
            ParsedConfig.backgroundColor = Color(Integer.parseInt(background.drop(1), 16))
        } else {
            try {
                ParsedConfig.backgroundImage = ImageIO.read(File(background))
                ParsedConfig.isPureColorBackground = false
            } catch (e : Exception) {
                genericLogger.warning("无法打开指定的背景图片: $background")
            }
        }

        if(fontPath.isNotBlank()) {
            val fontFile = File(fontPath)
            if(!fontFile.exists()) {
                genericLogger.warning("无法打开指定的字体文件: $fontPath，请检查配置文件")
            } else {
                try {
                    val font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
                    if(!GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)) {
                        genericLogger.warning("注册字体文件到LocalGraphicsEnvironment时失败")
                    }
                    ParsedConfig.font = font.deriveFont(20f)
                    genericLogger.info("正在使用字体文件: $fontPath")
                    return
                } catch (e : Exception) {
                    genericLogger.warning("读取字体文件: ${fontPath}时出错", e)
                }
            }
        }

        val fontList = mutableListOf<Font>()
        for(f in GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts) {
            if(f.name == fontName) {
                ParsedConfig.font = f.deriveFont(20f)
                return
            }
            if(f.canDisplay('啊')) {
                fontList.add(f)
            }
        }
        genericLogger.warning("找不到指定的字体 : ${fontName}，您可以在mcmotd.yml中修改字体名称")
        ParsedConfig.font = if(fontList.isEmpty()) {
            genericLogger.error("找不到可用的字体, 请检查您的系统是否安装了中文字体")
            Font(fontName, Font.PLAIN, 20)
        } else {
            genericLogger.info("检测到可用的字体列表: ${fontList.joinToString(",") { it.name }}")
            genericLogger.warning("正在使用第一个可用的字体: ${fontList[0].name}")
            fontList[0].deriveFont(20f)
        }
    }
}