package org.zrnq.mclient.output

import net.mamoe.mirai.utils.MiraiLogger
import org.zrnq.mclient.FONT
import org.zrnq.mclient.centerOnScreen
import org.zrnq.mclient.pingInternal
import org.zrnq.mclient.translateCommonException
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.concurrent.thread

abstract class AbstractOutputHandler {
    /**
     * Called before each ping request.
     * */
    abstract fun beforePing()
    /**
     * Called before attempting each available address.
     * */
    abstract fun onAttemptAddress(address : String)
    /**
     * Called after ping failed for an address.
     * */
    abstract fun onAttemptFailure(exception : Exception, address : String)
    /**
     * Called if no server address returns a valid response, and before afterPing().
     * */
    abstract fun onFailure()
    /**
     * Called if one server address returns a valid response, and before afterPing().
     * */
    abstract fun onSuccess(image : BufferedImage)
    /**
     * Called after each ping request, and after onFailure() and onSuccess().
     * */
    abstract fun afterPing()
}
/**
 * Debug use only
 * */
class GUIOutputHandler : AbstractOutputHandler() {
    private val errBuilder = StringBuilder()
    private val mainFrame = JFrame("Ping MC Server")
    private val progress = JProgressBar().apply { isIndeterminate = true; isVisible = false; isStringPainted = true; font = FONT }
    private val resultLabel = JLabel().apply { font = FONT; foreground = Color.RED }
    private val textField = JTextField()
    init {
        textField.apply {
            font = FONT
            preferredSize = Dimension(500, preferredSize.height)
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e : KeyEvent) {
                    if(e.keyCode != KeyEvent.VK_ENTER) return
                    thread {
                        pingInternal(text, this@GUIOutputHandler)
                        progress.isVisible = false
                        isVisible = true
                    }
                }
            })
        }
        val inputPane = JPanel().apply {
            layout = BorderLayout(20, 20)
            add(JLabel("Ping Target:").apply { font = FONT }, BorderLayout.WEST)
            add(JPanel().apply {
                layout = BorderLayout()
                add(progress, BorderLayout.CENTER)
                add(textField, BorderLayout.SOUTH)
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
    override fun beforePing() {
        errBuilder.clear()
        errBuilder.append("<html><span style='color:red;'>")
        progress.isVisible = true
        progress.string = "Performing DNS Lookup..."
        textField.isVisible = false
        resultLabel.icon = null
        resultLabel.text = ""
    }

    override fun onAttemptAddress(address : String) {
        progress.string = address
    }

    override fun onAttemptFailure(exception : Exception, address : String) {
        exception.printStackTrace()
        errBuilder.append("${exception.translateCommonException()}<br />")
    }

    override fun onFailure() {
        resultLabel.text = errBuilder.append("</span></html>").toString()
    }

    override fun onSuccess(image : BufferedImage) {
        resultLabel.icon = ImageIcon(image)
    }

    override fun afterPing() {
        progress.isVisible = false
        textField.isVisible = true
        mainFrame.pack()
    }
}

class APIOutputHandler(
    private val logger : MiraiLogger,
    private val fail : (String) -> Unit,
    private val success : (BufferedImage) -> Unit
) : AbstractOutputHandler() {
    private val errBuilder = StringBuilder()
    override fun beforePing() {
        errBuilder.clear()
        errBuilder.append("查询失败，以下地址均未能成功获取:\n")
    }

    override fun onAttemptAddress(address : String) = Unit

    override fun onAttemptFailure(exception : Exception, address : String) {
        val message = exception.translateCommonException()
        if(message.contains(':'))
            logger.warning("MC Ping Failed", exception)
        errBuilder.append("$message\n")
    }

    override fun onFailure() = fail(errBuilder.toString())

    override fun onSuccess(image : BufferedImage) = success(image)

    override fun afterPing() = Unit
}