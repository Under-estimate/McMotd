package org.zrnq.mclient

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import gnu.inet.encoding.IDNA
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JEditorPane
import javax.swing.JFrame

fun Int.secondToReadableTime() : String {
    return when {
        this < 60 -> "${this}s"
        this < 3600 -> String.format("%.2fmin", this.toFloat() / 60)
        else -> String.format("%.2fh", this.toFloat() / 3600)
    }
}

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

fun renderBasicInfoImage(info: ServerInfo) : BufferedImage {
    val margin = 20
    val width = 1000
    val iconSize = 160
    val iconCenter = 100
    val textWidth = width - iconSize - 3 * margin
    val textX = iconSize + 2 * margin

    val textContent = info.toHTMLString()
    // Creating a new JEditorPane every time since swing objects are not thread safe
    val textRenderer = JEditorPane("text/html", textContent)
    textRenderer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    textRenderer.text = textContent
    textRenderer.background = Color(0,0,0,0)
    textRenderer.font = MClientOptions.FONT
    textRenderer.setSize(textWidth, Short.MAX_VALUE.toInt())

    val textSize = textRenderer.preferredSize
    val imageHeight = (textSize.height + 2 * margin).coerceAtLeast(iconSize + 2 * margin)
    val result = createTransparentImage(width, imageHeight)

    val g = result.createGraphics()
    g.font = MClientOptions.FONT
    g.setRenderingHints(mapOf(
        RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BICUBIC,
        RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    ))
    if(info.favicon != null)
        paintBase64Image(info.favicon, g, margin, margin, iconSize, iconSize)
    else
        paintStringWithBackground("NO IMAGE", g, iconCenter, iconCenter, Color.WHITE, Color(0xaa0000), 15, 10)
    g.color = Color.WHITE
    g.drawRect(margin, margin, iconSize, iconSize)

    textRenderer.paint(g.create(textX, margin, textWidth, textSize.height))
    return result
}

fun paintStringWithBackground(str : String, g : Graphics2D, x : Int, y : Int, fg : Color, bg : Color, horizontalPadding : Int, verticalPadding : Int) {
    val fontMetrics = g.fontMetrics
    val textWidth = fontMetrics.stringWidth(str)
    val textX = x - textWidth / 2
    val textY = y + fontMetrics.ascent - fontMetrics.height / 2
    val rectX = textX - horizontalPadding
    val rectY = y - fontMetrics.height / 2 - verticalPadding
    g.color = bg
    g.fillRect(rectX, rectY, textWidth + 2 * horizontalPadding, fontMetrics.height + 2 * verticalPadding)
    g.color = fg
    g.drawString(str, textX, textY)
}

fun paintBase64Image(img : String, g : Graphics2D, x : Int, y : Int, w : Int, h : Int) {
    val imgDescriptor = img.split(",").let {
        it[0].substring(11, it[0].length - 7) to it[1].replace("\n", "")
    }
    val image = ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(imgDescriptor.second)))
    g.drawImage(image, x, y, w, h, null)
}

fun createTransparentImage(width: Int, height: Int) : BufferedImage {
    val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = result.createGraphics()
    g.composite = AlphaComposite.Clear
    g.fillRect(0, 0, width, height)
    g.composite = AlphaComposite.Src
    return result
}

fun generateBackgroundImage(width : Int, height : Int) : BufferedImage {
    val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = result.createGraphics()
    if(MClientOptions.isPureColorBackground) {
        g.color = MClientOptions.backgroundColor
        g.fillRect(0, 0, width, height)
        return result
    }
    val backgroundImage = MClientOptions.backgroundImage!!
    for(h in 0 until height step backgroundImage.height) {
        for(w in 0 until width step backgroundImage.width) {
            g.drawImage(backgroundImage, w, h, null)
        }
    }
    return result
}

fun BufferedImage.addBackground() : BufferedImage {
    val background = generateBackgroundImage(width, height)
    val g  = background.createGraphics()
    g.drawImage(this, 0, 0, null)
    return background
}

fun flatJsonEntity(result: JSONArray, entity: Any) {
    when(entity) {
        is String -> result.add(JSONObject().apply { put("text", entity) })
        is JSONObject -> {
            val flatObj = JSONObject()
            val currentIndex = result.size
            for(key in entity.keys) {
                if(key == "extra") flatJsonEntity(result, entity[key]!!)
                flatObj[key] = entity[key]
            }
            result.add(currentIndex, flatObj)
        }
        is JSONArray -> {
            for(element in entity) flatJsonEntity(result, element)
        }
    }

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
    flatJsonEntity(line, json)
    val builder = StringBuilder()

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


fun String.isValidURL() : Boolean {
    if(!this.matches(Regex("^[^:.]+(\\.[^:.]+)+(:[0-9]{1,5})?$"))) return false
    val segment = this.split(":")
    return try {
        IDNA.toASCII(segment[0])
        true
    } catch (e : Exception) {
        false
    }
}