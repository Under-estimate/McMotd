package org.zrnq.mcmotd.output

import org.zrnq.mcmotd.*
import org.zrnq.mcmotd.data.ParsedConfig
import org.zrnq.mcmotd.net.ServerInfo
import org.zrnq.mcmotd.net.parseAddress
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
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
    abstract fun onSuccess(info : ServerInfo)
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
    private val progress = JProgressBar().apply { isIndeterminate = true; isVisible = false; isStringPainted = true; font = ParsedConfig.font }
    private val resultLabel = JLabel().apply { font = ParsedConfig.font; foreground = Color.RED }
    private val textField = JTextField()
    init {
        textField.apply {
            font = ParsedConfig.font
            preferredSize = Dimension(500, preferredSize.height)
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e : KeyEvent) {
                    if(e.keyCode != KeyEvent.VK_ENTER) return
                    val address = text.parseAddress()
                    if(address == null) {
                        resultLabel.icon = null
                        resultLabel.text = "Invalid URL"
                        mainFrame.pack()
                        return
                    }
                    thread {
                        pingInternal(address, this@GUIOutputHandler)
                        progress.isVisible = false
                        isVisible = true
                    }
                }
            })
        }
        val inputPane = JPanel().apply {
            layout = BorderLayout(20, 20)
            add(JLabel("Ping Target:").apply { font = ParsedConfig.font }, BorderLayout.WEST)
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

    override fun onSuccess(info : ServerInfo) {
        resultLabel.icon = ImageIcon(renderBasicInfoImage(info).addBackground())
    }

    override fun afterPing() {
        progress.isVisible = false
        textField.isVisible = true
        mainFrame.pack()
    }
}

class APIOutputHandler(
    private val fail : (String) -> Unit,
    private val success : (ServerInfo) -> Unit
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
            genericLogger.warning("MC Ping Failed", exception)
        errBuilder.append("$address:$message\n")
    }

    override fun onFailure() = fail(errBuilder.toString())

    override fun onSuccess(info : ServerInfo) = success(info)

    override fun afterPing() = Unit
}