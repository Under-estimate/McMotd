package org.zrnq.mcmotd.http

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
import org.zrnq.mcmotd.*
import org.zrnq.mcmotd.output.APIOutputHandler
import org.zrnq.mcmotd.ImageUtil.appendPlayerHistory
import org.zrnq.mcmotd.ImageUtil.drawErrorMessage
import org.zrnq.mcmotd.net.parseAddressCached
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
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

fun Route.configureRouting() {
    route("/info") {
        get("{server?}") {
            if(!RateLimiter.pass(call.request.origin.remoteAddress))
                return@get call.respondText("Too many requests", status = HttpStatusCode.TooManyRequests)
            val servername = call.parameters["server"] ?: return@get respondErrorImage("未指定服务器名")
            val target = configStorage.httpServerMapping[servername]
                ?: return@get respondErrorImage("指定的服务器名没有在配置文件中定义")
            var error : String? = null
            var image : BufferedImage? = null
            val address = target.parseAddressCached()
            if(address == null) {
                genericLogger.error("Http服务器中配置的服务器地址无效:$target")
                return@get
            }
            withContext(Dispatchers.IO) {
                pingInternal(address, APIOutputHandler({ error = it }, { image = renderBasicInfoImage(it).appendPlayerHistory(target) }))
            }
            if(image == null) {
                genericLogger.error("Http请求失败:$error")
                return@get respondErrorImage("服务器信息获取失败")
            }
            return@get respondImage(image!!)
        }
    }
}