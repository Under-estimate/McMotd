package org.zrnq.mcmotd.logging

interface GenericLogger {
    fun info(message: String)
    fun warning(message: String, exception: Throwable? = null)
    fun error(message: String, exception: Throwable? = null)
}