package org.zrnq.mclient

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject

class ServerInfo(response : String, private val latency : Int) {
    /**服务器图标*/
    val favicon : String?
    /**服务器描述*/
    private val description : String
    /**服务器版本号*/
    private val version : String
    /**在线人数*/
    var onlinePlayerCount : Int?
    /**服务器宣称的最大人数*/
    private var maxPlayerCount : Int?
    /**服务器提供的部分在线玩家列表*/
    private var samplePlayerList : String?
    /**服务器的显示地址*/
    private lateinit var serverAddress : String

    init {
        val json = JSON.parseObject(response)
        favicon = json.getString("favicon")
        description = json.getString("description")
        version = json.getJSONObject("version").getString("name")
        val playerJson = json.getJSONObject("players")

        onlinePlayerCount = playerJson?.getIntValue("online")
        maxPlayerCount = playerJson?.getIntValue("max")
        samplePlayerList = playerJson?.getJSONArray("sample")?.toPlayerListString(10)
    }

    fun setAddress(address : String) : ServerInfo {
        serverAddress = address
        return this
    }

    private fun getDescriptionHTML(): String
        = if(description.startsWith("{")) jsonStringToHTML(JSON.parseObject(description))
            else jsonStringToHTML(JSON.parseObject("{\"text\":\"$description\"}"))

    private fun getPingHTML(): String {
        val bars = when(latency) {
            -1 -> "red" to 0
            in 0 until 100 -> "green" to 5
            in 100 until 300 -> "green" to 4
            in 300 until 500 -> "green" to 3
            in 500 until 1000 -> "yellow" to 2
            else -> "red" to 1
        }.let { colorMap[it.first] to it.second }
        if(latency < 0) return "失败 [<span style='color:${bars.first};text-shadow: gray 2px 2px;'>×</span>]"
        return "${latency}ms " +
               "[<span style='color:${bars.first}; font-weight:bold;'>${"|".repeat(bars.second)}</span>" +
               "<span style='color:${colorMap["gray"]}; font-weight:bold;'>${"|".repeat(5 - bars.second)}</span>]"
    }

    private fun getPlayerDescriptionHTML(): String {
        if(onlinePlayerCount == null) return "服务器未提供在线玩家信息"
        var playerCount = "在线人数: $onlinePlayerCount/$maxPlayerCount　"
        if(!MClientOptions.showPlayerList) return playerCount
        playerCount += "玩家列表: "
        if(samplePlayerList == null) return playerCount + "没有信息"
        return playerCount + jsonStringToHTML(JSON.parseObject("{\"text\":\"$samplePlayerList\"}"))
    }


    fun toHTMLString(): String {
        val sb = StringBuilder("<!DOCTYPE html><html><head></head><body><div>")
        sb.append(getDescriptionHTML())
            .append("</div>")
            .append("<div style='color:white;margin-top: 10px;'>访问地址: $serverAddress　Ping: ")
            .append(getPingHTML())
            .append("</div>")
        if(MClientOptions.showServerVersion) {
            sb.append("<div style='color:white;'>")
                .append(version.limitLength(50))
                .append("</div>")
        }
        sb.append("<div style='color:white;'>")
            .append(getPlayerDescriptionHTML())
            .append("</div>")
            .append("</body></html>")
        return sb.toString()
    }

    fun merge(queryInfo: QueryServerInfo) {
        if(onlinePlayerCount == null) {
            onlinePlayerCount = queryInfo.serverProperties["numplayers"]?.toInt()
        }
        if(maxPlayerCount == null) {
            maxPlayerCount = queryInfo.serverProperties["maxplayers"]?.toInt()
        }
        if(samplePlayerList == null || samplePlayerList == emptyPlayerListMsg) {
            samplePlayerList = if(queryInfo.playerList.isEmpty()) emptyPlayerListMsg
            else queryInfo.playerList.joinToString(", ", limit = 10)
        }
    }

    companion object {
        fun JSONArray?.toPlayerListString(limit: Int) : String? {
            return if(this == null) null
            else if(isEmpty()) emptyPlayerListMsg
            else joinToString(", ", limit = limit) { (it as JSONObject).getString("name") }
        }

        const val emptyPlayerListMsg = "空"
    }
}

class QueryServerInfo(val serverProperties: Map<String, String>, val playerList: List<String>)