package org.zrnq.mclient

import gnu.inet.encoding.IDNA
import org.xbill.DNS.*
import java.net.Inet4Address

const val DefaultPort = 25565

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

abstract class ServerAddress(val originalAddress: String, val port: Int, val queryPort: Int) {
    abstract fun addressList(): List<Pair<String, Int>>
}

class HostnameServerAddress(originalAddress: String,
                            private val hostname : String,
                            private var shouldResolve :Boolean = true,
                            port : Int = DefaultPort,
                            queryPort : Int = -1)
    : ServerAddress(originalAddress, port, queryPort) {
    init {
        if(hostname == "localhost") shouldResolve = false
    }
    private lateinit var cachedDNSResolutionResult : List<Pair<String, Int>>
    override fun addressList(): List<Pair<String, Int>> {
        if(!shouldResolve) return listOf(hostname to port)
        if(this::cachedDNSResolutionResult.isInitialized)
            return cachedDNSResolutionResult
        val encodedDomain = IDNA.toASCII(hostname)
        val addressList = mutableListOf<Pair<String, Int>>()
        val nameSet = mutableSetOf<String>()
        addressList.add(encodedDomain to port)
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
        cachedDNSResolutionResult = addressList
        return cachedDNSResolutionResult
    }
}

class IPServerAddress(originalAddress: String,
                      private val address : String,
                      port : Int = DefaultPort,
                      queryPort : Int = -1)
    : ServerAddress(originalAddress, port, queryPort) {
    override fun addressList() = listOf(address to port)
}