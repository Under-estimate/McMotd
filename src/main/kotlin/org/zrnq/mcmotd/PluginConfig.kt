package org.zrnq.mcmotd

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object PluginConfig : AutoSavePluginConfig("mcmotd") {
    val fontName by value("Microsoft YaHei")
}