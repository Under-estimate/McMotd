package org.zrnq.mclient

import org.zrnq.mclient.output.AbstractOutputHandler
import java.net.InetSocketAddress
import java.net.Socket


fun pingInternal(target : ServerAddress, outputHandler : AbstractOutputHandler) {
    try {
        outputHandler.beforePing()
        for(it in target.addressList()) {
            try {
                outputHandler.onAttemptAddress("${it.first}:${it.second}")
                val info = getInfo(it.first, it.second)
                    .let { if(!MClientOptions.showTrueAddress) it.setAddress(target.originalAddress) else it }
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

fun getInfo(address : String, port : Int = 25565) : ServerInfo {
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
        val time = System.currentTimeMillis()
        output.write(Packet(1, PLong(time)).byteArray)
        output.flush()
        // https://wiki.vg/Protocol#Ping : The returned value from server could be any number
        Packet(input, PLong::class)
        (System.currentTimeMillis() - time).toInt()
    } catch (e : Exception) {
        -1
    }

    socket.close()
    return ServerInfo(result, latency).setAddress("$address:$port")
}