package org.zrnq.mcmotd.http

import com.alibaba.fastjson.JSON
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zrnq.mcmotd.ImageUtil.appendPlayerHistory
import org.zrnq.mcmotd.ImageUtil.drawErrorMessage
import org.zrnq.mcmotd.configStorage
import org.zrnq.mcmotd.genericLogger
import org.zrnq.mcmotd.net.ServerInfo
import org.zrnq.mcmotd.net.parseAddress
import org.zrnq.mcmotd.net.parseAddressCached
import org.zrnq.mcmotd.output.APIOutputHandler
import org.zrnq.mcmotd.pingInternal
import org.zrnq.mcmotd.renderBasicInfoImage
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

object HttpServer {

    private var httpServer : ApplicationEngine? = null

    fun configureHttpServer() {
        if(configStorage.httpServerPort == 0) return
        genericLogger.info("Starting embedded http server on http://localhost:${configStorage.httpServerPort}")
        httpServer = embeddedServer(Netty, configStorage.httpServerPort, module = Application::mcmotdHttpServer).start(false)
    }

    fun stopHttpServer() {
        if(httpServer != null)
            httpServer!!.stop()
    }
}
fun Application.mcmotdHttpServer() {
    routing {
        configureRouting()
    }
}

suspend fun PipelineContext<*, ApplicationCall>.respondImage(image : BufferedImage)
        = call.respondBytes(ContentType.Image.PNG, HttpStatusCode.OK) {
    ByteArrayOutputStream().also { stream ->
        ImageIO.write(image, "png", stream)
    }.toByteArray()
}

suspend fun PipelineContext<*, ApplicationCall>.respondErrorImage(msg : String)
        = respondImage(BufferedImage(1000, 200, BufferedImage.TYPE_INT_RGB).also {
    it.createGraphics().drawErrorMessage(msg, 0, 0, 1000, 200)
})

suspend fun PipelineContext<*, ApplicationCall>.checkAddressAndPing(handler: APIOutputHandler) {
    if(!RateLimiter.pass(call.request.origin.remoteAddress))
        return call.respondText("Too many requests", status = HttpStatusCode.TooManyRequests)
    val servername = call.parameters["server"] ?: return respondErrorImage("未指定服务器名")
    val address =
        if(configStorage.httpServerAllowAnyAddress) {
            servername.parseAddress()
                ?: return handler.onFailure("指定的服务器地址无效:$servername")
        } else {
            val target = configStorage.httpServerMapping[servername]
                ?: return handler.onFailure("指定的服务器名没有在配置文件中定义")
            val tmpAddr = target.parseAddressCached()
            if(tmpAddr == null) {
                genericLogger.error("Http服务器中配置的服务器地址无效:$target")
                return handler.onFailure("指定的服务器地址无效:$target")
            }
            tmpAddr
        }
    withContext(Dispatchers.IO) {
        pingInternal(address, handler)
    }
}

suspend fun PipelineContext<*, ApplicationCall>.respondJson(content : Any)
        = call.respondText(JSON.toJSONString(content), ContentType.Application.Json, HttpStatusCode.OK)

@Suppress("unused")
sealed class RawApiResult(val online: Boolean)
@Suppress("unused")
class RawApiFailureResult(val message: String?): RawApiResult(false)
@Suppress("unused")
class RawApiPlayerInfo(
    val max: Int?,
    val online: Int?,
    val list: List<String>?
)
@Suppress("unused")
class RawApiSuccessResult(
    val latency: Int,
    val motd: String,
    val favicon: String?,
    val version: String,
    maxPlayer: Int?,
    onlinePlayer: Int?,
    playerList: List<String>?
): RawApiResult(true) {
    val players = RawApiPlayerInfo(maxPlayer, onlinePlayer, playerList)
}

fun Route.configureRouting() {
    route("/info") {
        get("{server?}") {
            var error : String? = null
            var image : BufferedImage? = null
            checkAddressAndPing(APIOutputHandler({ error = it }, { image = renderBasicInfoImage(it).appendPlayerHistory(it.originalAddress) }))
            if(image == null) {
                genericLogger.error("Http请求失败:$error")
                return@get respondErrorImage(error!!.substringBefore("，"))
            }
            return@get respondImage(image!!)
        }
    }
    if(configStorage.httpServerEnableDeveloperApi) {
        route("/raw") {
            get("{server}") {
                var error : String? = null
                var info : ServerInfo? = null
                checkAddressAndPing(APIOutputHandler({ error = it }, { info = it }))
                val infoFinal = info
                if(infoFinal == null) {
                    genericLogger.error("Http请求失败:$error")
                    return@get respondJson(RawApiFailureResult(error))
                }
                return@get respondJson(infoFinal.toRawApiResult())
            }
        }
        route("/icon") {
            get("{server}") {
                var error : String? = null
                var info : ServerInfo? = null
                checkAddressAndPing(APIOutputHandler({ error = it }, { info = it }))
                val infoFinal = info
                if(infoFinal == null) {
                    genericLogger.error("Http请求失败:$error")
                    return@get respondImage(BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB))
                }
                val favicon = infoFinal.favicon
                    ?: return@get respondImage(BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB))
                val imgDescriptor = favicon.substringAfter(',').replace("\n", "")
                val image = ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(imgDescriptor)))
                return@get respondImage(image)
            }
        }
    }
}