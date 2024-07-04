package org.zrnq.mcmotd.data

import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage

object ParsedConfig {
    lateinit var font: Font
    var backgroundColor = Color.BLACK
    var backgroundImage : BufferedImage? = null
    var isPureColorBackground = true
}