package org.zrnq.mcmotd.data

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object McMotdPluginData : AutoSavePluginData("mcmotd_relation"), McMotdData {
    override val relation by value<MutableMap<Long, MutableList<Pair<String, String>>>>()
    override val history by value<MutableMap<String, MutableList<Pair<Long, Int>>>>()
    override val peakPlayers by value<MutableMap<String, Int>>()
}