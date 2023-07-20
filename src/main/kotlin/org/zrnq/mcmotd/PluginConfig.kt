package org.zrnq.mcmotd

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object PluginConfig : AutoSavePluginConfig("mcmotd") {
    val fontPath by value("")
    val fontName by value("Microsoft YaHei")
    val showTrueAddress by value(false)
    val showServerVersion by value(false)
    val showPlayerList by value(true)
    val dnsServerList by value(mutableListOf("223.5.5.5", "8.8.8.8"))
    val recordOnlinePlayer by value(mutableListOf<String>())
    val recordInterval by value(300)
    val recordLimit by value(21600)
    val background by value("#000000")

    val httpServerPort by value(0)
    val httpServerMapping by value(mutableMapOf<String, String>())
}