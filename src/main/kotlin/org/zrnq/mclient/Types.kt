package org.zrnq.mclient

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.experimental.and

class PacketDataBuilder {
    private val data = mutableListOf<Byte>()
    operator fun plus(append : PacketData<*>) =
        data.addAll(append.byteArray.asIterable()).let{ this }
    fun toByteArray() = data.toByteArray()
}

abstract class PacketData<T> {
    abstract var value : T
    abstract val byteArray : ByteArray
    abstract fun parseFrom(stream : InputStream) : Int
}

class PLong(override var value : Long = 0L) : PacketData<Long>() {
    override val byteArray : ByteArray
        get() = ByteBuffer.allocate(8).let {
            it.order(ByteOrder.BIG_ENDIAN)
            it.putLong(value)
            it.array()
        }

    override fun parseFrom(stream : InputStream) : Int =
        ByteBuffer.allocate(8).let {
            for(i in 0..7) it.put(stream.readChecked().toByte())
            value = it.getLong(0)
            8
        }
}

class PUnsignedShort(override var value : UShort = 0u) : PacketData<UShort>() {
    override val byteArray : ByteArray
        get() = ByteBuffer.allocate(2).let {
            it.order(ByteOrder.BIG_ENDIAN)
            it.putShort(value.toShort())
            it.array()
        }

    override fun parseFrom(stream : InputStream) : Int {
        value = (stream.readChecked() shl 8 or stream.readChecked()).toUShort()
        return 2
    }
}

class PString(override var value : String = "") : PacketData<String>() {
    companion object {
        const val MAX_LENGTH = 32767
    }
    override val byteArray: ByteArray
        get() {
            if(value.isEmpty()) throw IllegalArgumentException("String is empty!")
            if(value.length > MAX_LENGTH) throw IllegalArgumentException("String too long (${value.length}>$MAX_LENGTH)")
            return PVarInt(value.length).byteArray + value.toByteArray(StandardCharsets.UTF_8)
        }

    override fun parseFrom(stream : InputStream) : Int {
        var fieldLength : Int
        val length = PVarInt().also { fieldLength = it.parseFrom(stream) }
        if(length.value > MAX_LENGTH) throw IllegalArgumentException("String too long (${length.value}>$MAX_LENGTH)")
        val buf = ByteArray(length.value)
        var readLen = 0
        while(readLen < length.value) {
            val thisRead = stream.read(buf, readLen, buf.size - readLen)
            if(thisRead < 0)
                throw IllegalArgumentException("Unexpected end of String (${readLen}<${length.value}).")
            readLen += thisRead
        }
        value = String(buf, StandardCharsets.UTF_8)
        return fieldLength + length.value
    }

}

class PVarInt(override var value : Int = 0) : PacketData<Int>() {
    override val byteArray : ByteArray
        get() {
            var tmp = value
            val result = mutableListOf<Byte>()
            while (true) {
                if (tmp and 0x7F.inv() == 0) {
                    result.add(tmp.toByte())
                    return result.toByteArray()
                }
                result.add((tmp and 0x7F or 0x80).toByte())
                tmp = tmp ushr 7
            }
        }
    override fun parseFrom(stream : InputStream) : Int {
        var value = 0
        var length = 0

        while(true) {
            val currentByte = stream.readChecked().toByte()
            value = value or ((currentByte and 0x7F.toByte()).toInt() shl length * 7)
            length += 1
            if (length > 5) {
                throw IllegalArgumentException("VarInt is too big")
            }
            if (currentByte and 0x80.toByte() != 0x80.toByte()) {
                break
            }
        }
        this.value = value
        return length
    }
}

fun InputStream.readChecked() = read().also { if(it < 0) throw IllegalStateException("Unexpected end of stream.") }

