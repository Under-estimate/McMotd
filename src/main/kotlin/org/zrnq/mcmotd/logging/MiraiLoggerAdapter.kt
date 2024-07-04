package org.zrnq.mcmotd.logging

import net.mamoe.mirai.utils.MiraiLogger

class MiraiLoggerAdapter(private val logger: MiraiLogger) : GenericLogger {
    override fun info(message: String) = logger.info(message)
    override fun warning(message: String, exception: Throwable?) = logger.warning(message, exception)
    override fun error(message: String, exception: Throwable?) = logger.error(message, exception)
}