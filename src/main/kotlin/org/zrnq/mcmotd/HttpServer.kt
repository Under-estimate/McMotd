package org.zrnq.mcmotd

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zrnq.mclient.output.APIOutputHandler
import org.zrnq.mclient.pingInternal
import org.zrnq.mclient.renderBasicInfoImage
import org.zrnq.mcmotd.ImageUtil.appendPlayerHistory
import org.zrnq.mcmotd.ImageUtil.drawErrorMessage
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.imageio.ImageIO

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

object RateLimiter {
    class AccessRecord {
        var success = 0
        var total = 0
        fun update(success: Boolean) {
            total++
            if(success) this.success++
        }
    }
    private var requestCoolDownRecord = Collections.synchronizedMap(HashMap<String, Long>())
    private var nextCleanup = 0L
    private var requestRecord = Collections.synchronizedMap(HashMap<String, AccessRecord>())
    private var nextRecordRefresh = 0L
    private val format = SimpleDateFormat("MM/dd HH:mm:ss")
    private fun recordRequest(address: String, success: Boolean) {
        if(PluginConfig.httpServerAccessRecordRefresh == 0) return
        val timeNow = System.currentTimeMillis()
        if(timeNow > nextRecordRefresh) {
            requestRecord.clear()
            nextRecordRefresh = timeNow + TimeUnit.SECONDS.toMillis(PluginConfig.httpServerAccessRecordRefresh.toLong())
        }
        requestRecord.getOrPut(address) { AccessRecord() }.update(success)
    }
    fun getRecordData() : String {
        return "%s - %s\n%s".format(
            format.format(Date(nextRecordRefresh - TimeUnit.SECONDS.toMillis(PluginConfig.httpServerAccessRecordRefresh.toLong()))),
                format.format(Date(nextRecordRefresh)),
                if(requestRecord.isEmpty()) "统计时间段内没有访问记录"
                else synchronized(requestRecord) {
                    requestRecord.entries.stream()
                        .sorted { o1, o2 -> o2.value.total - o1.value.total }
                        .limit(10)
                        .map { "${it.key}: ${it.value.total}(${it.value.success})" }
                        .collect(Collectors.joining("\n"))
                })
    }
    fun pass(address : String) : Boolean = run {
        if(PluginConfig.httpServerRequestCoolDown == 0) return@run true // cool down disabled
        val lastAccessRecord = requestCoolDownRecord[address]
        val timeNow = System.currentTimeMillis()
        if(lastAccessRecord == null || lastAccessRecord < timeNow) {
            if(requestCoolDownRecord.size > PluginConfig.httpServerParallelRequest) {
                if(nextCleanup > timeNow) return@run false // reaching parallel request limit
                // Clean up records
                synchronized(requestCoolDownRecord) {
                    val it = requestCoolDownRecord.iterator()
                    while(it.hasNext()) {
                        if(it.next().value < timeNow) it.remove()
                    }
                }
                nextCleanup = timeNow + PluginConfig.httpServerRequestCoolDown
            }
            requestCoolDownRecord[address] = timeNow + PluginConfig.httpServerRequestCoolDown
            return@run true
        } else return@run false // cool down incomplete
    }.also { recordRequest(address, it) }
}

fun Route.configureRouting() {
    route("/info") {
        get("{server?}") {
            if(!RateLimiter.pass(call.request.origin.remoteAddress))
                return@get call.respondText("Too many requests", status = HttpStatusCode.TooManyRequests)
            val servername = call.parameters["server"] ?: return@get respondErrorImage("未指定服务器名")
            val target = PluginConfig.httpServerMapping[servername]
                ?: return@get respondErrorImage("指定的服务器名没有在配置文件中定义")
            var error : String? = null
            var image : BufferedImage? = null
            withContext(Dispatchers.IO) {
                pingInternal(target, APIOutputHandler(McMotd.logger, { error = it }, { image = renderBasicInfoImage(it).appendPlayerHistory(target) }))
            }
            if(image == null) {
                McMotd.logger.warning("Http请求失败:$error")
                return@get respondErrorImage("服务器信息获取失败")
            }
            return@get respondImage(image!!)
        }
    }
}