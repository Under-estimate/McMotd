package org.zrnq.mcmotd.net

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*

abstract class QueryPacketData<T> {
    abstract var value: T
    abstract fun parseFrom(byteBuffer: ByteBuffer)
}

class QString : QueryPacketData<String>() {
    override lateinit var value: String
    override fun parseFrom(byteBuffer: ByteBuffer) {
        val initPos = byteBuffer.position()
        while(true) {
            val b = byteBuffer.get()
            if(b == NULL_BYTE)
                break
        }
        val endPos = byteBuffer.position()
        byteBuffer.position(initPos)
        val byteArr = ByteArray(endPos - initPos)
        byteBuffer.get(byteArr)
        value = String(byteArr, 0, byteArr.size - 1, StandardCharsets.US_ASCII)
    }

    companion object {
        const val NULL_BYTE = 0.toByte()
    }
}

class QConstRegion(override var value: ByteArray) : QueryPacketData<ByteArray>() {
    override fun parseFrom(byteBuffer: ByteBuffer) {
        val actual = ByteArray(value.size)
        byteBuffer.get(actual)
        if(!Arrays.equals(value, actual)) {
            throw IllegalArgumentException("Const Region Mismatch! Expected: $value, Actual: $actual")
        }
    }
}

class QMap : QueryPacketData<Map<String, String>>() {
    override lateinit var value: Map<String, String>
    override fun parseFrom(byteBuffer: ByteBuffer) {
        val stringParser = QString()
        val map = mutableMapOf<String, String>()
        while(true) {
            stringParser.parseFrom(byteBuffer)
            if(stringParser.value.isEmpty()) break
            val key = stringParser.value
            stringParser.parseFrom(byteBuffer)
            map[key] = stringParser.value
        }
        value = map
    }
}

class QList : QueryPacketData<List<String>>() {
    override lateinit var value: List<String>
    override fun parseFrom(byteBuffer: ByteBuffer) {
        val stringParser = QString()
        val list = mutableListOf<String>()
        while(true) {
            stringParser.parseFrom(byteBuffer)
            if(stringParser.value.isEmpty()) break
            list.add(stringParser.value)
        }
        value = list
    }
}