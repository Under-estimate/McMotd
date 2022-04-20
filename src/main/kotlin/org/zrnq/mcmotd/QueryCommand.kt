package org.zrnq.mcmotd

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.sendAnsiMessage
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.zrnq.mclient.output.APIOutputHandler
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO

@Suppress("unused")
object QueryCommand :  SimpleCommand(McMotd, "mcp", description = "获取指定MC服务器的信息") {
    @Handler
    suspend fun MemberCommandSender.handle() {
        val serverList = PluginData[this.group.id]
        if(serverList == null) {
            reply("本群未绑定服务器，请使用/mcadd绑定服务器或直接提供服务器地址")
            return
        }
        if(serverList.size == 1)
            doPing(serverList.first().second)
        else
            reply("本群绑定了多个服务器，请指定服务器名称。可用的服务器：${serverList.serverNameList}")
    }

    @Handler
    suspend fun CommandSender.handle(target : String)  {
        if(this is MemberCommandSender) {
            val serverList = PluginData[this.group.id]
            if(serverList != null) {
                val server = serverList.firstOrNull { it.first == target }
                if(server != null) {
                    doPing(server.second)
                    return
                }
            }
        }
        if(target.matches(Regex("^[a-zA-Z0-9\\-_]+\\.[a-zA-Z0-9\\-_.]+[a-zA-Z0-9\\-_](:[0-9]{1,5})?$")))
            doPing(target)
        else
            reply("服务器地址格式错误，请指定形如: mc.example.com 或者 mc.example.com:25565 的地址")
    }


    private suspend fun CommandSender.doPing(target : String) = withContext(Dispatchers.IO) {
        var error : String? = null
        var image : BufferedImage? = null
        org.zrnq.mclient.pingInternal(target, APIOutputHandler(McMotd.logger, { error = it }, { image = it }), PluginConfig.showTrueAddress)
        if(image == null)
            reply(error!!)
        else
            reply(image!!)
    }
}

@Suppress("unused")
object BindCommand : SimpleCommand(McMotd, "mcadd", description = "为当前群聊绑定MC服务器") {
    @Handler
    suspend fun MemberCommandSender.handle(name : String, address : String) {
        val serverList = PluginData[this.group.id] ?: mutableListOf()
        val existing = serverList.find { it.first == name }
        if(existing != null) {
            reply("服务器名称已存在：$name")
            return
        }
        if(!address.matches(Regex("^[a-zA-Z0-9\\-_]+\\.[a-zA-Z0-9\\-_.]+[a-zA-Z0-9\\-_](:[0-9]{1,5})?$"))) {
            reply("服务器地址格式错误，请指定形如: mc.example.com 或者 mc.example.com:25565 的地址")
            return
        }
        serverList.add(name to address)
        PluginData[this.group.id] = serverList
        reply("绑定成功：$name -> $address")
    }
}

@Suppress("unused")
object DelCommand : SimpleCommand(McMotd, "mcdel", description = "删除当前群聊绑定的服务器") {
    @Handler
    suspend fun MemberCommandSender.handle(name : String) {
        val serverList = PluginData[this.group.id] ?: mutableListOf()
        val existing = serverList.find { it.first == name }
        if(existing == null) {
            reply("本群没有绑定服务器：$name。可用的服务器：${serverList.serverNameList}")
            return
        }
        serverList.remove(existing)
        PluginData[this.group.id] = serverList
        reply("删除成功")
    }
}

private suspend fun CommandSender.reply(message : String) {
    if(user == null) sendAnsiMessage { lightPurple().append(message) }
    else sendMessage(At(user!!.id) + message)
}

private suspend fun CommandSender.reply(image : BufferedImage) {
    if(user == null) {
        val savePath = File("${UUID.randomUUID()}.png")
        withContext(Dispatchers.IO) { ImageIO.write(image, "png", savePath) }
        reply("查询结果已保存至${savePath.absolutePath}")
    } else {
        val bis = ByteArrayOutputStream()
        withContext(Dispatchers.IO) { ImageIO.write(image, "png", bis) }
        ByteArrayInputStream(bis.toByteArray()).toExternalResource("png").use {
            sendMessage(At(user!!.id) + user!!.uploadImage(it))
        }
    }
}

private val List<Pair<String, String>>.serverNameList get() = joinToString(", ") { it.first }