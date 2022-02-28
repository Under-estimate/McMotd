package org.zrnq.mclient

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Toolkit
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JLabel

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
    text = "<html><span style='color:white;'>${str.replace(" ", "&nbsp;").replace("\n", "<br />")}</span></html>"
    foreground = Color.WHITE
    font = g.font
    block()
    paint(g.create(x, y, w, h))
}

fun paintDescription(desc : String, g : Graphics2D, x : Int, y : Int, w : Int, h : Int) = JLabel().apply {
    setSize(w, h)
    text = if(desc.startsWith("{")) jsonStringToHTML(JSON.parseObject(desc))
        else textToHTML(desc)
    font = g.font
    paint(g.create(x, y, w, h))
}

fun textToHTML(str : String)
    = "<html><span style='color:white;'>${str.replace(" ", "&nbsp;").replace("\n", "<br />")}</span></html>"

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

fun jsonStringToHTML(json : JSON) : String{
    val line = JSONArray()
    when (json) {
        is JSONObject -> flatTextJSON(line, json)
        is JSONArray -> for(i in json.indices) flatTextJSON(line, json.getJSONObject(i))
        else -> throw IllegalArgumentException("Description syntax error")
    }
    val builder = StringBuilder("<html>")
    for(i in line.indices) {
        val paragraph = line.getJSONObject(i)
        val color = paragraph.getStringOrDefault("color", "white")
        builder.append("<span style='color: ${if(color.startsWith("#")) color else colorMap[color]};")
        if(paragraph.getBooleanOrDefault("bold"))
            builder.append("font-weight: bold;")
        if(paragraph.getBooleanOrDefault("italic"))
            builder.append("font-style: italic;")
        val strikethrough = paragraph.getBooleanOrDefault("strikethrough")
        val underlined = paragraph.getBooleanOrDefault("underlined")
        if(strikethrough || underlined) {
            builder.append("text-decoration:")
            if(strikethrough) builder.append(" line-through")
            if(underlined) builder.append(" underline")
            builder.append(";")
        }
        builder.append("'>")
            .append(paragraph.getStringOrDefault("text").replace(" ", "&nbsp;").replace("\n", "<br />"))
            .append("<span>")
    }
    builder.append("</html>")
    return builder.toString()
}

fun JSONObject.getStringOrDefault(key : String, default : String = "") : String =
    if(containsKey(key)) getString(key) else default

fun JSONObject.getBooleanOrDefault(key : String, default : Boolean = false) : Boolean =
    if(containsKey(key)) getBoolean(key) else default

fun File.createChild(name : String) =
    if(isDirectory) File("$absolutePath\\$name")
    else throw IllegalArgumentException("$this is not a directory.")

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