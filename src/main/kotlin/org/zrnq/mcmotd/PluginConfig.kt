package org.zrnq.mcmotd

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object PluginConfig : AutoSavePluginConfig("mcmotd") {
    val fontName by value("Microsoft YaHei")
    val showTrueAddress by value(false)
    val dnsServerList by value(mutableListOf("223.5.5.5", "8.8.8.8", "114.114.114.114"))
}