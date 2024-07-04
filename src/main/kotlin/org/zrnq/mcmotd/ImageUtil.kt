package org.zrnq.mcmotd

import org.zrnq.mcmotd.ColorScheme.toTransparent
import org.zrnq.mcmotd.data.ParsedConfig
import java.awt.*
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.tan
import kotlin.random.Random

object ImageUtil {
    fun BufferedImage.appendPlayerHistory(address : String) : BufferedImage {
        val playerHistoryHeight = 200
        if(!configStorage.recordOnlinePlayer.contains(address)) return this.addBackground()
        val history = dataStorage.getHistory(address)
        val result = createTransparentImage(1000, height + playerHistoryHeight)
        val historyImage = renderPlayerHistory(history)

        val g = result.createGraphics()
        g.drawImage(this, 0, 0, null)
        g.drawImage(historyImage, 0, height, null)
        return result.addBackground()
    }
    fun renderPlayerHistory(history : MutableList<Pair<Long, Int>>) : BufferedImage {
        val height = 200
        val width = 1000
        val result = createTransparentImage(width, height)
        val g = result.createGraphics()
        g.color = Color.WHITE
        g.font = ParsedConfig.font
        g.setRenderingHints(mapOf(
            RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
            RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON))
        g.drawString("在线人数趋势", 20, 20)

        if(history.size <= 1) {
            g.drawErrorMessage("没有足够的数据来绘制图表，稍后再来吧", 0, 0, width, height)
            return result
        }

        val minTime = history.first().first
        val maxTime = history.last().first
        val timeRange = maxTime - minTime
        val maxCount = history.maxOf { it.second }.coerceAtLeast(1)

        val minX = max(40, 20 + g.fontMetrics.stringWidth(maxCount.toString()))
        val maxX = width - 40
        val minY = 20 + g.fontMetrics.height
        val maxY = height - 30
        val xRange = maxX - minX
        val yRange = maxY - minY

        g.color = Color.GRAY
        g.drawLine(minX, minY, minX, maxY)
        (1..4).map {
            it * yRange / 4 + minY
        }.forEach {
            g.drawLine(minX, it, maxX, it)
        }

        g.color = Color.WHITE

        g.paintTextRB("0", minX, maxY)
        g.paintTextRC(maxCount.toString(), minX, minY)

        val format = SimpleDateFormat("HH:mm")

        // x-axis ticks
        (0 .. 4).map {
            format.format(Date(it * timeRange / 4 + minTime)) to
            it * xRange / 4 + minX
        }.forEach {
            g.paintTextCT(it.first, it.second, maxY)
        }

        val plot = history.map {
            ((it.first - minTime) * xRange / timeRange).toInt() to
            (it.second * yRange / maxCount)
        }.map {
            minX + it.first to
            maxY - it.second
        }

        val polygon = plot.toMutableList().also {
            it.add(maxX to maxY)
            it.add(minX to maxY)
        }

        g.paint = GradientPaint(minX.toFloat(), minY.toFloat(), ColorScheme.darkGreen,
            minX.toFloat(), maxY.toFloat(), ColorScheme.darkGreen.toTransparent())
        g.fillPolygon(polygon.map { it.first }.toIntArray(), polygon.map { it.second }.toIntArray(), polygon.size)

        g.color = ColorScheme.brightGreen
        g.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        g.drawPolyline(plot.map { it.first }.toIntArray(), plot.map { it.second }.toIntArray(), plot.size)

        return result
    }
    /**指定字符串的右侧中心坐标来绘制*/
    fun Graphics2D.paintTextRC(str : String, x : Int, y : Int) {
        drawString(str, x - fontMetrics.stringWidth(str), y + fontMetrics.ascent - fontMetrics.height / 2)
    }
    fun Graphics2D.paintTextRB(str : String, x : Int, y : Int) {
        drawString(str, x - fontMetrics.stringWidth(str), y - fontMetrics.descent)
    }
    /**指定字符串的中心顶部坐标来绘制*/
    fun Graphics2D.paintTextCT(str : String, x : Int, y : Int) {
        drawString(str, x - fontMetrics.stringWidth(str) / 2, y + fontMetrics.ascent)
    }
    fun Graphics2D.paintTextCC(str : String, x : Int, y : Int) {
        drawString(str, x - fontMetrics.stringWidth(str) / 2, y + fontMetrics.ascent - fontMetrics.height / 2)
    }

    fun Graphics2D.fillStripedRect(color1 : Color, color2 : Color, x : Int, y : Int, width : Int, height : Int) {
        val angle = 45.0
        val stripeWidth = 20
        val offset = Random.Default.nextInt(0, stripeWidth * 2)
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        color = color1
        fillRect(x, y, width, height)
        color = color2
        val deviation = height / tan(Math.toRadians(angle))
        val limit = ceil((width + deviation) / stripeWidth).toInt()
        for(i in 0..limit step 2) {
            val base = x + i * stripeWidth - offset
            fillPolygon(
                intArrayOf(base, base + stripeWidth, base + stripeWidth - deviation.toInt(), base - deviation.toInt()),
                intArrayOf(y, y, y + height, y + height),
                4
            )
        }
    }

    fun Graphics2D.drawErrorMessage(msg : String, x : Int, y : Int, width: Int, height: Int) {
        font = ParsedConfig.font
        fillStripedRect(Color.black, ColorScheme.darkRed, x, y, width, height)
        val msgRectWidth = fontMetrics.stringWidth(msg) + 30
        val msgRectHeight = fontMetrics.height + 20
        color = Color.black
        fillRect(x + (width - msgRectWidth) / 2, y + (height - msgRectHeight) / 2, msgRectWidth, msgRectHeight)
        color = ColorScheme.darkRed
        drawRect(x + (width - msgRectWidth) / 2, y + (height - msgRectHeight) / 2, msgRectWidth, msgRectHeight)
        color = ColorScheme.brightRed
        paintTextCC(msg, x + width / 2, y + height / 2)
    }
}

object ColorScheme {
    val brightRed = Color(254, 71, 81)
    val darkRed = Color(201, 42, 42)
    val brightGreen = Color(132, 188, 60)
    val darkGreen = Color(132, 188, 60, 150)

    fun Color.toTransparent()
        = Color(red, green, blue, 0)
}