package org.zrnq.mclient

import java.io.InputStream
import kotlin.reflect.KClass

class Packet(var packetId : Int, vararg dataArgs : PacketData<*>) {
    val data = mutableListOf<PacketData<*>>()
    val byteArray : ByteArray
        get() {
            val builder = PacketDataBuilder() + PVarInt(packetId)
            data.forEach { builder + it }
            val byteData = builder.toByteArray()
            return PVarInt(byteData.size).byteArray + byteData
        }
    init {
        data.addAll(dataArgs)
    }
    constructor(stream : InputStream, vararg contentTypes : KClass<out PacketData<*>>) : this(0) {
        val packetLength = PVarInt().also { it.parseFrom(stream) }.value
        var receivedLength = 0
        packetId = PVarInt().also { receivedLength += it.parseFrom(stream) }.value
        for(dataType in contentTypes) {
            val instance = dataType.constructors.find { it.parameters.all { it.isOptional } }!!.callBy(mapOf())
            receivedLength += instance.parseFrom(stream)
            data.add(instance)
        }
        if(receivedLength != packetLength)
            println("Packet length mismatch (Declared : ${packetLength}, Received : ${receivedLength})")
    }
}