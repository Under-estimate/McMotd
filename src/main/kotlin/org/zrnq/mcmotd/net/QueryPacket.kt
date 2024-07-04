package org.zrnq.mcmotd.net

import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.nio.ByteBuffer
import kotlin.random.Random

enum class QueryPacketType(val typeId: Byte) {
    Stat(0),
    Handshake(9)
}

class QuerySession(val address: SocketAddress) : Closeable {
    val sessionId = Random.nextInt() and 0x0F0F0F0F
    val socket = DatagramSocket()
    init {
        socket.soTimeout = 3000
    }
    fun sendPacket(type: QueryPacketType, payload: (ByteBuffer) -> Unit) {
        val packet = QueryClientPacket(type, sessionId)
        payload(packet.data)
        socket.send(packet.toDatagramPacket(address))
    }

    fun receivePacket(expectType: QueryPacketType, contentSlots: List<QueryPacketData<*>>) {
        val packet = QueryServerPacket(contentSlots, socket, 1024)
        if(packet.type != expectType) throw IllegalArgumentException("Received packet with unexpected type ${packet.type}")
        if(packet.sessionId != sessionId) throw IllegalArgumentException("Received packet with unexpected sessionId ${packet.sessionId}")
    }

    override fun close() {
        socket.close()
    }
}

class QueryClientPacket(type: QueryPacketType, sessionId: Int) {
    val data = ByteBuffer.allocate(64)
    init {
        data.putShort(magic)
        data.put(type.typeId)
        data.putInt(sessionId)
    }

    fun toDatagramPacket(address: SocketAddress) : DatagramPacket {
        return DatagramPacket(data.array(), data.position(), address)
    }
    companion object {
        const val magic : Short = -259 // 0xFEFD
    }
}

class QueryServerPacket(contentSlots: List<QueryPacketData<*>>, socket: DatagramSocket, size: Int) {
    val type: QueryPacketType
    val sessionId: Int
    init {
        val packet = DatagramPacket(ByteArray(size), size)
        socket.receive(packet)
        val buffer = ByteBuffer.wrap(packet.data)
        val typeId = buffer.get()
        type = QueryPacketType.values().find { it.typeId == typeId }!!
        sessionId = buffer.getInt()
        contentSlots.forEach { it.parseFrom(buffer) }
    }
}