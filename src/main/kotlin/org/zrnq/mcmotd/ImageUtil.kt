package org.zrnq.mcmotd

import org.zrnq.mclient.MClientOptions
import java.awt.*
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object ImageUtil {
    fun BufferedImage.appendPlayerHistory(address : String) : BufferedImage {
        if(!PluginConfig.recordOnlinePlayer.contains(address)) return this
        val history = PluginData.getHistory(address)
        val result = BufferedImage(1000, 400, BufferedImage.TYPE_INT_RGB)
        val historyImage = renderPlayerHistory(history)

        val g = result.createGraphics()
        g.drawImage(this, 0, 0, null)
        g.drawImage(historyImage, 0, 200, null)
        return result
    }
    fun renderPlayerHistory(history : MutableList<Pair<Long, Int>>) : BufferedImage {
        val height = 200
        val width = 1000
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = result.createGraphics()
        g.color = Color.WHITE
        g.font = MClientOptions.FONT
        g.setRenderingHints(mapOf(
            RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
            RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON))
        g.drawString("在线人数趋势", 20, 20)

        if(history.size <= 1) {
            g.color = Color(254, 71, 81)
            g.paintTextCC("-=没有足够的数据来绘制图表，稍后再来吧=-", width / 2, height / 2)
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

        g.paint = GradientPaint(minX.toFloat(), minY.toFloat(), Color(132, 188, 60, 150),
            minX.toFloat(), maxY.toFloat(), Color(132, 188, 60, 0))
        g.fillPolygon(polygon.map { it.first }.toIntArray(), polygon.map { it.second }.toIntArray(), polygon.size)

        g.color = Color(132, 188, 60)
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
}