package org.zrnq.mclient

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import org.xbill.DNS.Lookup
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.Type
import java.awt.*
import java.awt.image.BufferedImage
import java.net.InetSocketAddress
import java.net.Socket
import javax.swing.*

const val addressPrefix = "_minecraft._tcp."

fun pingExternal(target : String) : Any {
    var image : BufferedImage? = null
    val errBuilder = StringBuilder("查询失败，以下地址均未能成功获取:\n")
    try {
        val option = target.split(":")
        val addressList = mutableListOf<Pair<String, Int>>()
        if(option.size > 1) {
            addressList.add(option[0] to option[1].toInt())
        } else {
            Lookup("$addressPrefix${option[0]}", Type.SRV).run()?.forEach {
                check(it is SRVRecord)
                addressList.add(it.target.toString(true) to it.port)
            }
            addressList.add(option[0] to 25565)
        }
        for(it in addressList) {
            try {
                image = renderInfoImage(it.first, it.second)
                break
            } catch (ex : Exception) {
                errBuilder.append("${it.first}:${it.second} => ${ex.javaClass.name.substringAfterLast('.')}:${ex.message}\n")
            }
        }
    } catch (e : Exception) {
        e.printStackTrace()
    }
    return image ?: errBuilder.toString()
}

fun renderInfoImage(address : String, port : Int) : BufferedImage {
    val info = getInfo(address, port)
    val border = 20
    val width = 1000
    val height = 200
    val json = JSON.parseObject(info.first)
    val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = result.createGraphics()
    g.font = FONT
    g.setRenderingHints(mapOf(
        RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BICUBIC,
        RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    ))
    if(json.containsKey("favicon"))
        paintBase64Image(json.getString("favicon"), g, border, border, height - 2 * border, height - 2 * border)
    else
        paintString("NO IMAGE", g, border, (height - g.fontMetrics.height) / 2 , height - 2 * border, height - 2 * border) {
            foreground = Color.MAGENTA
            horizontalAlignment = SwingConstants.CENTER
        }
    g.drawRect(border, border, height - 2 * border, height - 2 * border)
    paintDescription(json.getJSONObject("description"), g, height, border, width - border - height, height / 2 - border)
    val playerJson = json.getJSONObject("players")
    var playerDescription = "获取失败"
    if(playerJson != null)
        playerDescription = "${playerJson["online"]}/${playerJson["max"]}  "
    playerDescription +=
        if(playerJson.containsKey("sample")) "玩家列表：${getPlayerList(playerJson.getJSONArray("sample")).limitLength(50)}"
        else "玩家列表：没有信息"
    paintString("""
        访问地址 ${address}:${port}      Ping: ${info.second}
        ${json.getJSONObject("version").getString("name")}
        在线人数: $playerDescription""".trimIndent()
        , g, height, height / 2, width - border - height, height / 2 - border)
    return result
}

fun getPlayerList(list : JSONArray) = list.map { (it as JSONObject).getString("name") }.joinToString(", ")

fun getInfo(address : String, port : Int = 25565) : Pair<String, String> {
    val socket = Socket()
    socket.soTimeout = 3000
    socket.connect(InetSocketAddress(address, port))
    val input = socket.getInputStream().buffered()
    val output = socket.getOutputStream()

    output.write(Packet(0,
        PVarInt(757),
        PString(address),
        PUnsignedShort(port.toUShort()),
        PVarInt(1)).byteArray)
    output.flush()

    output.write(Packet(0).byteArray)
    output.flush()

    val result = Packet(input, PString::class).data[0].value as String

    val latency = try {
        output.write(Packet(1, PLong(System.currentTimeMillis())).byteArray)
        output.flush()
        (System.currentTimeMillis() - Packet(input, PLong::class).data[0].value as Long).toString() + "ms"
    } catch (e : Exception) {
        "Failed"
    }

    socket.close()
    return result to latency
}