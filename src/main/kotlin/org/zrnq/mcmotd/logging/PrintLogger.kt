package org.zrnq.mcmotd.logging

import java.text.SimpleDateFormat
import java.util.*

class PrintLogger : GenericLogger {
    private val dateFormat = SimpleDateFormat("HH:mm:ss")
    private val prefix get() = "${dateFormat.format(Date())} "
    override fun info(message: String) {
        println("$prefix I $message")
    }

    override fun warning(message: String, exception: Throwable?) {
        println("$prefix W $message")
        exception?.printStackTrace(System.out)
    }

    override fun error(message: String, exception: Throwable?) {
        println("$prefix E $message")
        exception?.printStackTrace(System.out)
    }
}