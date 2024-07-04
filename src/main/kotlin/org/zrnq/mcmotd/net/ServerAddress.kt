package org.zrnq.mcmotd.net

import gnu.inet.encoding.IDNA
import org.xbill.DNS.*
import org.zrnq.mcmotd.configStorage
import java.net.Inet4Address
import java.util.*

const val DefaultPort = 25565

const val addressPrefix = "_minecraft._tcp."
private val dnsResolvers by lazy {
    configStorage.dnsServerList.map {
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
                org.zrnq.mcmotd.genericLogger.error("SRV解析出错，请检查DNS服务器配置项[${dnsResolver.address}]：${it.message}")
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

object AddressRegexes {
    val ipv4addr = Regex("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$")
    val ipv6addr = Regex(
        "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|" +
                "([0-9a-fA-F]{1,4}:){1,7}:|" +
                "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|" +
                "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|" +
                "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|" +
                "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|" +
                "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|" +
                "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|" +
                ":((:[0-9a-fA-F]{1,4}){1,7}|:)|" +
                "fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|" +
                "::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|" +
                "([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))$")
    val addrWithPort = Regex("^[^:]*?(:((6553[0-5])|(655[0-2][0-9])|(65[0-4][0-9]{2})|(6[0-4][0-9]{3})|([1-5][0-9]{4})|([0-5]{0,5})|([0-9]{1,4}))){1,2}$")
    val ipv6addrWithPort = Regex("^\\[[^\\[\\]]*?](:((6553[0-5])|(655[0-2][0-9])|(65[0-4][0-9]{2})|(6[0-4][0-9]{3})|([1-5][0-9]{4})|([0-5]{0,5})|([0-9]{1,4}))){1,2}$")
    val genericHostname = Regex("^(localhost)|([^:.]+(\\.[^:.]+)+)$")
}

private val addressCache = Collections.synchronizedMap(WeakHashMap<String, ServerAddress>())

fun String.parseAddressCached(): ServerAddress? {
    var result = addressCache[this]
    if(result == null) {
        result = parseAddress()
        if(result != null) addressCache[this] = result
    }
    return result
}

fun String.parseAddress(): ServerAddress? {
    if(matches(AddressRegexes.addrWithPort)) {
        val segments = split(':')
        val serverAddress = segments[0]
        val serverPort = segments[1].toInt()
        val queryPort = if(segments.size >= 3) segments[2].toInt() else -1
        val serverOriginalAddress = if(segments.size >= 3) "$serverAddress:$serverPort" else this
        if(serverAddress.matches(AddressRegexes.ipv4addr))
            return IPServerAddress(serverOriginalAddress, serverAddress, serverPort, queryPort)
        if(serverAddress.matches(AddressRegexes.genericHostname))
            return HostnameServerAddress(serverOriginalAddress, serverAddress, false, serverPort, queryPort)
        return null
    }
    if(matches(AddressRegexes.ipv4addr))
        return IPServerAddress(this, this)
    if(matches(AddressRegexes.genericHostname))
        return HostnameServerAddress(this, this)
    if(matches(AddressRegexes.ipv6addrWithPort)) {
        val segments = split("]:")
        val serverAddress = segments[0].substring(1)
        val portSegments = segments[1].split(':')
        val serverPort = portSegments[0].toInt()
        val queryPort = if(portSegments.size >= 2) segments[1].toInt() else -1
        val serverOriginalAddress = if(portSegments.size >= 2) "[$serverAddress]:$serverPort" else this
        if(serverAddress.matches(AddressRegexes.ipv6addr))
            return IPServerAddress(serverOriginalAddress, serverAddress, serverPort, queryPort)
    }
    if(matches(AddressRegexes.ipv6addr))
        return IPServerAddress(this, this)
    return null
}