package org.zrnq.mcmotd

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.sendAnsiMessage
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO

object QueryCommand :  SimpleCommand(McMotd, "mcp", description = "获取指定MC服务器的信息") {
    @Handler
    suspend fun CommandSender.handle(target : String) = withContext(Dispatchers.Default) {
        if(!target.matches(Regex("^[a-zA-Z0-9\\-_]+\\.[a-zA-Z0-9\\-_.]+[a-zA-Z0-9\\-_](:[0-9]{1,5})?$"))) {
            reply("服务器地址格式错误，请指定形如: mc.example.com 或者 mc.example.com:25565 的地址")
            return@withContext
        }
        launch {
            val result = withContext(Dispatchers.IO) {
                org.zrnq.mclient.pingExternal(target)
            }
            if(result is String) {
                reply(result)
            } else {
                reply(result as BufferedImage)
            }
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
}