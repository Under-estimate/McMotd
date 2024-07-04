package org.zrnq.mcmotd.data

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object McMotdPluginConfig : AutoSavePluginConfig("mcmotd"), McMotdConfig {
    override val fontPath by value("")
    override val fontName by value("Microsoft YaHei")
    override val showTrueAddress by value(false)
    override val showServerVersion by value(false)
    override val showPlayerList by value(true)
    override val dnsServerList by value(mutableListOf("223.5.5.5", "8.8.8.8"))
    override val recordOnlinePlayer by value(mutableListOf<String>())
    override val recordInterval by value(300)
    override val recordLimit by value(21600)
    override val background by value("#000000")

    override val httpServerPort by value(0)
    override val httpServerMapping by value(mutableMapOf<String, String>())
    override val httpServerParallelRequest by value(32)
    override val httpServerRequestCoolDown by value(3000)
    override val httpServerAccessRecordRefresh by value(0)
}