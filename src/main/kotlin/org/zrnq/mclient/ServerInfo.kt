package org.zrnq.mclient

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject

class ServerInfo(response : String, val latency : String) {
    /**服务器图标*/
    val favicon : String?
    /**服务器描述*/
    val description : String
    /**服务器版本号*/
    val version : String
    /**服务器在线玩家描述*/
    val playerDescription : String
    /**在线人数*/
    val onlinePlayerCount : Int?
    /**服务器宣称的最大人数*/
    val maxPlayerCount : Int?
    /**服务器提供的部分在线玩家列表*/
    val samplePlayerList : String?
    /**服务器的显示地址*/
    lateinit var serverAddress : String

    init {
        val json = JSON.parseObject(response)
        favicon = json.getString("favicon")
        description = json.getString("description")
        version = json.getJSONObject("version").getString("name")
        val playerJson = json.getJSONObject("players")

        onlinePlayerCount = playerJson?.getIntValue("online")
        maxPlayerCount = playerJson?.getIntValue("max")
        samplePlayerList = playerJson?.getJSONArray("sample")?.toPlayerListString()

        playerDescription = run {
            if(onlinePlayerCount == null) return@run "服务器未提供在线玩家信息"
            val playerCount = "在线人数: $onlinePlayerCount/$maxPlayerCount  玩家列表: "
            if(samplePlayerList == null) return@run playerCount + "没有信息"
            return@run playerCount + samplePlayerList.limitLength(50)
        }
    }

    fun setAddress(address : String) : ServerInfo {
        serverAddress = address
        return this
    }


    companion object {
        fun JSONArray?.toPlayerListString() : String {
            return if(this == null) "没有信息"
            else if(isEmpty()) "空"
            else joinToString(", ") { (it as JSONObject).getString("name") }
        }
    }
}