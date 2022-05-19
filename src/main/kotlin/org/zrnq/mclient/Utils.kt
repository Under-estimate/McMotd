package org.zrnq.mclient

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Toolkit
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JLabel

fun Exception.translateCommonException()
= when {
    matches<java.net.ConnectException>("Connection timed out: connect") -> "连接服务器超时"
    matches<java.net.ConnectException>("Connection refused: connect") -> "无法连接到服务器"
    matches<java.net.SocketTimeoutException>("Read timed out") -> "连接服务器超时"
    matches<java.net.UnknownHostException>() -> "找不到目标主机"
    else -> "${javaClass.name.substringAfterLast('.')}:$message"
}

inline fun <reified E> Exception.matches(msg : String? = null) = this::class == E::class && (msg == null || message == msg)

fun String.limitLength(max : Int) = if (length > max) this.substring(0, max) + "..." else this

fun paintBase64Image(img : String, g : Graphics2D, x : Int, y : Int, w : Int, h : Int) {
    val imgDescriptor = img.split(",").let {
        it[0].substring(11, it[0].length - 7) to it[1].replace("\n", "")
    }
    val image = ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(imgDescriptor.second)))
    g.drawImage(image, x, y, w, h, null)
}

fun paintString(str : String, g : Graphics2D, x : Int, y : Int, w : Int, h : Int, block : JLabel.() -> Unit = {}) = JLabel().apply {
    setSize(w, h)
    text = "<html><span style='color:white;white-space:nowrap;text-overflow:ellipsis;'>${str.replace(" ", "&nbsp;").replace("\n", "<br />")}</span></html>"
    foreground = Color.WHITE
    font = g.font
    block()
    paint(g.create(x, y, w, h))
}

fun paintDescription(desc : String, g : Graphics2D, x : Int, y : Int, w : Int, h : Int) = JLabel().apply {
    setSize(w, h)
    text = if(desc.startsWith("{")) jsonStringToHTML(JSON.parseObject(desc))
        else jsonStringToHTML(JSON.parseObject("{\"text\":\"$desc\"}"))
    font = g.font
    paint(g.create(x, y, w, h))
}

fun flatTextJSON(result : JSONArray, src : JSONObject) {
    val currentIndex = result.size
    val paragraph = JSONObject()
    for(key in src.keys) {
        if(key == "extra") {
            when (val extra = src[key]) {
                is JSONArray -> for(i in extra.indices) flatTextJSON(result, extra.getJSONObject(i))
                is JSONObject -> flatTextJSON(result, extra)
                else -> throw IllegalArgumentException("Description syntax error")
            }
        } else {
            paragraph[key] = src[key]
        }
    }
    result.add(currentIndex, paragraph)
}

private const val RAW = 8
private const val SEQ = 0
private const val COLOR = 0
private const val BOLD = 1
private const val ITALIC = 2
private const val UNDERLINE = 3
private const val STRIKE = 4

fun jsonStringToHTML(json : JSON) : String{
    val line = JSONArray()
    when (json) {
        is JSONObject -> flatTextJSON(line, json)
        is JSONArray -> for(i in json.indices) flatTextJSON(line, json.getJSONObject(i))
        else -> throw IllegalArgumentException("Description syntax error")
    }
    val builder = StringBuilder("<html>")

    val attributes = Array<Any?>(RAW * 2) { null }

    fun color() = (attributes[SEQ + COLOR] ?: attributes[RAW + COLOR] ?: "white") as String
    fun bold() = (attributes[SEQ + BOLD] ?: attributes[RAW + BOLD] ?: false) as Boolean
    fun italic() = (attributes[SEQ + ITALIC] ?: attributes[RAW + ITALIC] ?: false) as Boolean
    fun underline() = (attributes[SEQ + UNDERLINE] ?: attributes[RAW + UNDERLINE] ?: false) as Boolean
    fun strike() = (attributes[SEQ + STRIKE] ?: attributes[RAW + STRIKE] ?: false) as Boolean
    fun styleSpan() : String {
        val sb = StringBuilder("<span style='color: ${color().let{if(it.startsWith("#")) it else colorMap[it]}};")
        if(bold()) sb.append("font-weight: bold;")
        if(italic()) sb.append("font-style: italic;")
        if(strike() || underline()) {
            sb.append("text-decoration:")
            if(strike()) sb.append(" line-through")
            if(underline()) sb.append(" underline")
            sb.append(";")
        }
        sb.append("'>")
        return sb.toString()
    }
    val spanEnd = "</span>"
    var escapeSeq = false

    for(i in line.indices) {
        val paragraph = line.getJSONObject(i)
        attributes[RAW + COLOR] = paragraph["color"]
        attributes[RAW + BOLD] = paragraph["bold"]
        attributes[RAW + ITALIC] = paragraph["italic"]
        attributes[RAW + UNDERLINE] = paragraph["underlined"]
        attributes[RAW + STRIKE] = paragraph["strikethrough"]
        builder.append(styleSpan())
        val text = paragraph.getStringOrDefault("text", "")
        for(c in text) {
            if(escapeSeq) {
                if(c in '0'..'9' || c in 'a'..'f') {
                    attributes[SEQ + COLOR] = colorSequence[c.digitToInt(16)]
                    attributes[SEQ + BOLD] = null
                    attributes[SEQ + STRIKE] = null
                    attributes[SEQ + UNDERLINE] = null
                    attributes[SEQ + ITALIC] = null
                } else if(c in 'l'..'r') {
                    when(c) {
                        'l' -> attributes[SEQ + BOLD] = true
                        'm' -> attributes[SEQ + STRIKE] = true
                        'n' -> attributes[SEQ + UNDERLINE] = true
                        'o' -> attributes[SEQ + ITALIC] = true
                        'r' -> {
                            attributes[SEQ + COLOR] = null
                            attributes[SEQ + BOLD] = null
                            attributes[SEQ + STRIKE] = null
                            attributes[SEQ + UNDERLINE] = null
                            attributes[SEQ + ITALIC] = null
                        }
                    }
                }
                escapeSeq = false
                builder.append(spanEnd)
                    .append(styleSpan())
            } else {
                if(c == '§') escapeSeq = true
                else builder.append(when(c) {
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '&' -> "&amp;"
                    ' ' -> "&nbsp;"
                    '\n' -> "<br />"
                    else -> c
                })
            }
        }
        builder.append(spanEnd)
    }
    builder.append("</html>")
    return builder.toString()
}

fun JSONObject.getStringOrDefault(key : String, default : String = "") : String =
    if(containsKey(key)) getString(key) else default

fun JFrame.centerOnScreen() {
    val screen = Toolkit.getDefaultToolkit().screenSize
    setLocation((screen.width - width) / 2, (screen.height - height) / 2)
}

val colorMap = mapOf(
    "black" to          "#000000",
    "dark_blue" to      "#0000aa",
    "dark_green" to     "#00aa00",
    "dark_aqua" to      "#00aaaa",
    "dark_red" to       "#aa0000",
    "dark_purple" to    "#aa00aa",
    "gold" to           "#ffaa00",
    "gray" to           "#aaaaaa",
    "dark_gray" to      "#555555",
    "blue" to           "#5555ff",
    "green" to          "#55ff55",
    "aqua" to           "#55ffff",
    "red" to            "#ff5555",
    "light_purple" to   "#ff55ff",
    "yellow" to         "#ffff55",
    "white" to          "#ffffff")

val colorSequence = listOf(
    "#000000",
    "#0000aa",
    "#00aa00",
    "#00aaaa",
    "#aa0000",
    "#aa00aa",
    "#ffaa00",
    "#aaaaaa",
    "#555555",
    "#5555ff",
    "#55ff55",
    "#55ffff",
    "#ff5555",
    "#ff55ff",
    "#ffff55",
    "#ffffff"
)