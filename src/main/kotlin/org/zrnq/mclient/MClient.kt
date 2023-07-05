package org.zrnq.mclient

import gnu.inet.encoding.IDNA
import org.xbill.DNS.*
import org.zrnq.mclient.output.AbstractOutputHandler
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import javax.swing.SwingConstants

const val addressPrefix = "_minecraft._tcp."
private val dnsResolvers by lazy {
    MClientOptions.dnsServerList.map {
        SimpleResolver(Inet4Address.getByName(it))
        .also { resolver -> resolver.timeout = java.time.Duration.ofSeconds(2) }
    }
}

private fun Resolver.query(name : String) : List<Record> {
    return send(Message.newQuery(Record.newRecord(Name.fromString(name), Type.SRV, DClass.IN)))
        .getSection(Section.ANSWER)
}

fun pingInternal(target : String, outputHandler : AbstractOutputHandler) {
    try {
        outputHandler.beforePing()
        val option = target.split(":")
        val encodedDomain = IDNA.toASCII(option[0])
        val addressList = mutableListOf<Pair<String, Int>>()
        val nameSet = mutableSetOf<String>()
        if(option.size > 1) {
            addressList.add(encodedDomain to option[1].toInt())
        } else {
            addressList.add(encodedDomain to 25565)
            for(dnsResolver in dnsResolvers) {
                runCatching {
                    dnsResolver.query("$addressPrefix${encodedDomain}.")
                }.fold({
                    it.forEach { rec ->
                        check(rec is SRVRecord)
                        rec.target.toString(true)
                            .takeIf { addr -> nameSet.add(addr) }
                            ?.also { addr -> addressList.add(addr to rec.port) }
                    }
                }, {
                    System.err.println("SRV解析出错，请检查DNS服务器配置项[${dnsResolver.address}]：${it.message}")
                })
            }
        }
        for(it in addressList) {
            try {
                outputHandler.onAttemptAddress("${it.first}:${it.second}")
                val info = getInfo(it.first, it.second)
                    .let { if(!MClientOptions.showTrueAddress) it.setAddress(target) else it }
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

fun renderBasicInfoImage(info : ServerInfo) : BufferedImage {
    val border = 20
    val width = 1000
    val height = 200
    val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = result.createGraphics()
    g.font = MClientOptions.FONT
    g.setRenderingHints(mapOf(
        RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BICUBIC,
        RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    ))
    if(info.favicon != null)
        paintBase64Image(info.favicon, g, border, border, height - 2 * border, height - 2 * border)
    else
        paintString("NO IMAGE", g, border, (height - g.fontMetrics.height) / 2 , height - 2 * border, height - 2 * border) {
            foreground = Color.MAGENTA
            horizontalAlignment = SwingConstants.CENTER
        }
    g.drawRect(border, border, height - 2 * border, height - 2 * border)
    paintDescription(info.description, g, height, border, width - border - height, height / 2 - border)

    val sb = StringBuilder("访问地址: ${info.serverAddress}      Ping: ${info.latency}")
    if(MClientOptions.showServerVersion) sb.append("\n${info.version.limitLength(50)}")
    sb.append("\n${info.playerDescription}")
    paintDescription(sb.toString(), g, height, height / 2, width - border - height, height / 2 - border)
    return result
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
        (System.currentTimeMillis() - time).toString() + "ms"
    } catch (e : Exception) {
        "Failed"
    }

    socket.close()
    return ServerInfo(result, latency).setAddress("$address:$port")
}