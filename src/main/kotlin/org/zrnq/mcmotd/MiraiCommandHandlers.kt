package org.zrnq.mcmotd

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.util.sendAnsiMessage
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.zrnq.mcmotd.output.APIOutputHandler
import org.zrnq.mcmotd.ImageUtil.appendPlayerHistory
import org.zrnq.mcmotd.McMotd.permissionId
import org.zrnq.mcmotd.McMotd.reload
import org.zrnq.mcmotd.data.McMotdPluginData
import org.zrnq.mcmotd.data.McMotdPluginConfig
import org.zrnq.mcmotd.http.RateLimiter
import org.zrnq.mcmotd.net.ServerAddress
import org.zrnq.mcmotd.net.parseAddress
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO

@Suppress("unused")
object QueryCommand :  SimpleCommand(McMotd, "mcp", description = "获取指定MC服务器的信息") {

    override val permission: Permission
        get() = strictPermission

    private lateinit var strictPermission: Permission
    private lateinit var relaxedPermission: Permission

    fun preparePermissions() {
        relaxedPermission = PermissionService.INSTANCE.register(
            permissionId("command.mcp"),
            "获取任意MC服务器的信息",
            McMotd.parentPermission
        )
        strictPermission = PermissionService.INSTANCE.register(
            permissionId("command.mcp.strict"),
            "获取指定MC服务器的信息(仅限本群绑定的服务器)",
            relaxedPermission)
    }

    @Handler
    suspend fun MemberCommandSender.handle() {
        val serverList = McMotdPluginData.getBoundServer(this.group.id)
        if(serverList == null) {
            reply("本群未绑定服务器，请使用/mcadd绑定服务器或直接提供服务器地址")
            return
        }
        if(serverList.size == 1)
            doPing(serverList.first().second)
        else
            pingMultiple(serverList.map { it.second })
    }

    @Handler
    suspend fun CommandSender.handle(target : String)  {
        if(this is MemberCommandSender) {
            McMotdPluginData.getBoundServer(this.group.id)
                ?.firstOrNull { it.first == target }
                ?.let { doPing(it.second); return }
        }
        if(!relaxedPermission.testPermission(this)) return
        doPing(target)
    }


    private suspend fun CommandSender.doPing(target : String) = withContext(Dispatchers.IO) {
        var error : String? = null
        var image : BufferedImage? = null
        val address = target.parseAddress()
        if(address == null) {
            reply("服务器地址格式错误，请指定形如: mc.example.com 或者 mc.example.com:25565 的地址")
            return@withContext
        }
        pingInternal(address, APIOutputHandler({ error = it }, { image = renderBasicInfoImage(it).appendPlayerHistory(target) }))
        if(image == null)
            reply(error!!)
        else
            reply(image!!)
    }

    private suspend fun CommandSender.pingMultiple(target: List<String>) = withContext(Dispatchers.IO) {
        val addressList = mutableListOf<ServerAddress>()
        target.forEach {
            val parsed = it.parseAddress()
            if(parsed == null) {
                reply("配置的服务器地址\"$it\"有误，请检查配置文件")
                return@withContext
            }
            addressList.add(parsed)
        }
        val imageList = mutableListOf<BufferedImage>()
        addressList.forEachIndexed { index, addr ->
            var error : String? = null
            var image : BufferedImage? = null
            pingInternal(addr, APIOutputHandler({ error = it }, { image = renderBasicInfoImage(it).appendPlayerHistory(target[index])}))
            if(image == null)
                reply("连接\"${target[index]}\"时出错：$error")
            else
                imageList.add(image!!)
        }
        reply(ImageUtil.combineImages(imageList))
    }
}

@Suppress("unused")
object BindCommand : SimpleCommand(McMotd, "mcadd", description = "为当前群聊绑定MC服务器") {
    @Handler
    suspend fun MemberCommandSender.handle(name : String, address : String) {
        val serverList = McMotdPluginData.getBoundServer(this.group.id) ?: mutableListOf()
        val existing = serverList.find { it.first == name }
        if(existing != null) {
            reply("服务器名称已存在：$name")
            return
        }
        if(address.parseAddress() == null) {
            reply("服务器地址格式错误，请指定形如: mc.example.com 或者 mc.example.com:25565 的地址")
            return
        }
        serverList.add(name to address)
        McMotdPluginData.setBoundServer(this.group.id, serverList)
        reply("绑定成功：$name -> $address")
    }
}

@Suppress("unused")
object DelCommand : SimpleCommand(McMotd, "mcdel", description = "删除当前群聊绑定的服务器") {
    @Handler
    suspend fun MemberCommandSender.handle(name : String) {
        val serverList = McMotdPluginData.getBoundServer(this.group.id) ?: mutableListOf()
        val existing = serverList.find { it.first == name }
        if(existing == null) {
            reply("本群没有绑定服务器：$name。可用的服务器：${serverList.serverNameList}")
            return
        }
        serverList.remove(existing)
        McMotdPluginData.setBoundServer(this.group.id, serverList)
        reply("删除成功")
    }
}

@Suppress("unused")
object RecordCommand : SimpleCommand(McMotd, "mcrec", description = "指定需要记录在线人数的服务器") {
    @Handler
    suspend fun MemberCommandSender.handle() {
        if(McMotdPluginConfig.recordOnlinePlayer.isEmpty()) {
            reply("没有已启用在线人数记录的服务器，使用\"/mcrec <服务器地址> true\"以开始记录指定服务器的在线人数")
            return
        }
        reply("已启用在线人数记录的服务器:${McMotdPluginConfig.recordOnlinePlayer.joinToString(",")}。每${McMotdPluginConfig.recordInterval.secondToReadableTime()}记录一次在线人数，最多保存${McMotdPluginConfig.recordLimit.secondToReadableTime()}之前的记录。")
    }

    @Handler
    suspend fun MemberCommandSender.handle(address : String) {
        if(McMotdPluginConfig.recordOnlinePlayer.contains(address))
            reply("服务器[$address]已启用在线人数记录，使用\"/mcrec $address false\"禁用此服务器的在线人数记录功能")
        else
            reply("服务器[$address]未启用在线人数记录，使用\"/mcrec $address true\"启用此服务器的在线人数记录功能")
    }

    @Handler
    suspend fun MemberCommandSender.handle(address : String, enable : Boolean) {
        if(enable) {
            if(!McMotdPluginConfig.recordOnlinePlayer.contains(address))
                McMotdPluginConfig.recordOnlinePlayer.add(address)
            reply("已开始记录${address}的在线人数")
        } else {
            McMotdPluginConfig.recordOnlinePlayer.remove(address)
            McMotdPluginData.history.remove(address)
            reply("已停止记录${address}的在线人数")
        }
    }
}

@Suppress("unused")
object HttpServerCommand : SimpleCommand(McMotd, "mcapi", description = "获取Http API访问计数信息") {
    @Handler
    suspend fun CommandSender.handle() {
        if(McMotdPluginConfig.httpServerPort == 0) {
            reply("Http API未开启")
            return
        }
        if(McMotdPluginConfig.httpServerAccessRecordRefresh == 0) {
            reply("Http API访问计数功能未开启")
            return
        }
        reply(RateLimiter.getRecordData())
    }
}

@Suppress("unused")
object ConfigReloadCommand : SimpleCommand(McMotd, "mcreload", description = "重载插件配置") {
    @Handler
    suspend fun CommandSender.handle() {
        // run inside McMotd.timer to avoid ConcurrentModification with player history recording.
        PlayerHistory.timer.schedule(object : TimerTask() {
            override fun run() {
                McMotdPluginConfig.reload()
                configStorage.checkConfig()
                runBlocking {
                    this@handle.reply("配置重载完成")
                }
            }
        }, 0)
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
