package org.zrnq.mclient

import org.xbill.DNS.Lookup
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.Type
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.concurrent.thread

lateinit var FONT : Font

fun main() {
    FONT = Font("Microsoft YaHei UI", Font.PLAIN, 20)
    val mainFrame = JFrame("Ping MC Server")
    val progress = JProgressBar().apply { isIndeterminate = true; isVisible = false; isStringPainted = true; font = FONT }
    val resultLabel = JLabel().apply { font = FONT; foreground = Color.RED }

    fun ping(target : String) {
        var image : BufferedImage? = null
        val errBuilder = StringBuilder("<html><span style='color:red;'>")
        try {
            val option = target.split(":")
            val addressList = mutableListOf<Pair<String, Int>>()
            if(option.size > 1) {
                addressList.add(option[0] to option[1].toInt())
            } else {
                Lookup("$addressPrefix${option[0]}", Type.SRV).run()?.forEach {
                    check(it is SRVRecord)
                    addressList.add(it.target.toString(true) to it.port)
                }
                addressList.add(option[0] to 25565)
            }
            for(it in addressList) {
                progress.string = "${it.first}:${it.second}"
                try {
                    image = renderInfoImage(it.first, it.second)
                    break
                } catch (ex : Exception) {
                    ex.printStackTrace()
                    errBuilder.append("${it.first}:${it.second} => $ex<br />")
                }
            }
            if(image == null) {
                resultLabel.text = errBuilder.append("</span></html>").toString()
            } else {
                resultLabel.icon = ImageIcon(image)
            }
        } catch (e : Exception) {
            resultLabel.text = e.toString()
            e.printStackTrace()
        }
        mainFrame.pack()
    }
    val inputPane = JPanel().apply {
        layout = BorderLayout(20, 20)
        add(JLabel("Ping Target:").apply { font = FONT }, BorderLayout.WEST)
        add(JPanel().apply {
            layout = BorderLayout()
            add(progress, BorderLayout.CENTER)
            add(JTextField().apply {
                font = FONT
                preferredSize = Dimension(500, preferredSize.height)
                addKeyListener(object : KeyAdapter() {
                    override fun keyPressed(e : KeyEvent) {
                        if(e.keyCode != KeyEvent.VK_ENTER) return
                        progress.isVisible = true
                        progress.string = "Performing DNS Lookup..."
                        isVisible = false
                        resultLabel.icon = null
                        resultLabel.text = ""
                        thread {
                            ping(text)
                            progress.isVisible = false
                            isVisible = true
                        }
                    }
                })
            }, BorderLayout.SOUTH)
        }, BorderLayout.CENTER)
    }

    mainFrame.apply {
        layout = BorderLayout(20, 20)
        add(inputPane, BorderLayout.NORTH)
        add(resultLabel, BorderLayout.CENTER)
        (contentPane as JPanel).border = EmptyBorder(20, 20, 20, 20)
        isVisible = true
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        pack()
        centerOnScreen()
    }
}