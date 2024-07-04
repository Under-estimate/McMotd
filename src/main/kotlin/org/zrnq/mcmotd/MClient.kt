package org.zrnq.mcmotd

import org.zrnq.mcmotd.output.AbstractOutputHandler
import org.zrnq.mcmotd.net.*
import java.net.InetSocketAddress
import java.net.Socket


fun pingInternal(target : ServerAddress, outputHandler : AbstractOutputHandler) {
    try {
        outputHandler.beforePing()
        for(it in target.addressList()) {
            try {
                outputHandler.onAttemptAddress("${it.first}:${it.second}")
                val info = getInfo(it.first, it.second, target.queryPort)
                    .let { if(!configStorage.showTrueAddress) it.setAddress(target.originalAddress) else it }
                outputHandler.onSuccess(info)
                outputHandler.afterPing()
                return
            } catch (ex : Exception) {
                outputHandler.onAttemptFailure(ex, "${it.first}:${it.second}")
            }
        }
        outputHandler.onFailure()
        outputHandler.afterPing()
    } catch (e : Exception) {
        e.printStackTrace()
    }
}

fun getInfo(address : String, port : Int = 25565, queryPort: Int = -1) : ServerInfo {
    val serverInfo = Socket().use { socket ->
        socket.soTimeout = 3000
        socket.connect(InetSocketAddress(address, port))
        val input = socket.getInputStream().buffered()
        val output = socket.getOutputStream()

        output.write(
            ProtocolPacket(0,
            PVarInt(757),
            PString(address),
            PUnsignedShort(port.toUShort()),
            PVarInt(1)).byteArray)
        output.flush()

        output.write(ProtocolPacket(0).byteArray)
        output.flush()

        val result = ProtocolPacket(input, PString::class).data[0].value as String

        val latency = try {
            val time = System.currentTimeMillis()
            output.write(ProtocolPacket(1, PLong(time)).byteArray)
            output.flush()
            // https://wiki.vg/Protocol#Ping : The returned value from server could be any number
            ProtocolPacket(input, PLong::class)
            (System.currentTimeMillis() - time).toInt()
        } catch (e : Exception) {
            -1
        }

        ServerInfo(result, latency).setAddress("$address:$port")
    }
    if(queryPort < 0) return serverInfo
    val queryInfo = try {
        getQueryInfo(address, queryPort)
    } catch (e: Exception) {
        e.printStackTrace()
        return serverInfo
    }
    serverInfo.merge(queryInfo)
    return serverInfo
}

// See https://wiki.vg/Query for these magic bytes
private val splitNum = byteArrayOf(0x73, 0x70, 0x6C, 0x69, 0x74, 0x6E, 0x75, 0x6D, 0x00, -128, 0x00)
private val player_ = byteArrayOf(0x01, 0x70, 0x6C, 0x61, 0x79, 0x65, 0x72, 0x5F, 0x00, 0x00)

fun getQueryInfo(address: String, port: Int) = QuerySession(InetSocketAddress(address, port)).use { session ->
    session.sendPacket(QueryPacketType.Handshake) {}
    val handshakeResponse = listOf(QString())
    session.receivePacket(QueryPacketType.Handshake, handshakeResponse)
    val token = handshakeResponse[0].value.toInt()
    session.sendPacket(QueryPacketType.Stat) { payload ->
        payload.putInt(token)
        payload.putInt(0) // padding
    }
    val serverProperties = QMap()
    val playerList = QList()
    session.receivePacket(
        QueryPacketType.Stat, listOf(
        QConstRegion(splitNum),
        serverProperties,
        QConstRegion(player_),
        playerList
    ))
    QueryServerInfo(serverProperties.value, playerList.value)
}