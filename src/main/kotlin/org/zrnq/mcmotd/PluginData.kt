package org.zrnq.mcmotd

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object PluginData : AutoSavePluginData("mcmotd_relation") {
    var relation by value<MutableMap<Long, MutableList<Pair<String, String>>>>()

    var history by value<MutableMap<String, MutableList<Pair<Long, Int>>>>()

    operator fun get(groupId : Long) : MutableList<Pair<String, String>>? {
        val result = relation[groupId]
            ?: return null
        if(result.isEmpty()) {
            relation.remove(groupId)
            return null
        }
        return result
    }

    operator fun set(groupId : Long, value : MutableList<Pair<String, String>>) {
        if(value.isEmpty())
            relation.remove(groupId)
        else
            relation[groupId] = value
    }

    fun getHistory(address : String) : MutableList<Pair<Long, Int>> {
        val result = history[address]
            ?: return mutableListOf()
        val limit = System.currentTimeMillis() - PluginConfig.recordLimit * 1000
        result.removeIf { it.first < limit }
        return result
    }

    fun recordHistory(address : String, count : Int) {
        val target = getHistory(address)
        target.add(System.currentTimeMillis() to count)
        history[address] = target
    }
}