package org.zrnq.mcmotd.data

import org.zrnq.mcmotd.configStorage
import kotlin.math.max

interface McMotdData {
    val relation : MutableMap<Long, MutableList<Pair<String, String>>>
    val history : MutableMap<String, MutableList<Pair<Long, Int>>>
    val peakPlayers: MutableMap<String, Int>

    fun getBoundServer(groupId : Long) : MutableList<Pair<String, String>>? {
        val result = relation[groupId]
            ?: return null
        if(result.isEmpty()) {
            relation.remove(groupId)
            return null
        }
        return result
    }

    fun setBoundServer(groupId : Long, value : MutableList<Pair<String, String>>) {
        if(value.isEmpty())
            relation.remove(groupId)
        else
            relation[groupId] = value
    }

    fun getHistory(address : String) : MutableList<Pair<Long, Int>> {
        val result = history[address]
            ?: return mutableListOf()
        val limit = System.currentTimeMillis() - configStorage.recordLimit * 1000
        result.removeIf { it.first < limit }
        return result
    }

    fun recordHistory(address : String, count : Int) {
        val target = getHistory(address)
        target.add(System.currentTimeMillis() to count)
        history[address] = target
        peakPlayers[address] = max(peakPlayers[address] ?: 0, count)
    }
}