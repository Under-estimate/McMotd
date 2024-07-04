package org.zrnq.mcmotd

import org.zrnq.mcmotd.data.McMotdStandaloneData
import org.zrnq.mcmotd.net.parseAddressCached
import org.zrnq.mcmotd.output.APIOutputHandler
import java.util.*

object PlayerHistory {
    lateinit var timer : Timer

    fun startRecord() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                configStorage.recordOnlinePlayer.forEach { address ->
                    val resolvedAddress = address.parseAddressCached()
                    if(resolvedAddress == null) {
                        genericLogger.warning("在线人数记录配置的服务器地址无效: $address")
                        return@forEach
                    }
                    pingInternal(resolvedAddress, APIOutputHandler(
                        { genericLogger.warning("在线人数记录失败 $address: $it") },
                        { if(it.onlinePlayerCount == null) genericLogger.warning("在线人数记录: ($address) 没有提供在线人数数据")
                        else dataStorage.recordHistory(address, it.onlinePlayerCount!!) })
                    )
                }
                (dataStorage as? McMotdStandaloneData)?.save()
            }
        }, configStorage.recordInterval.toLong() * 1000, configStorage.recordInterval.toLong() * 1000)
    }

    fun stopRecord() {
        timer.cancel()
    }
}