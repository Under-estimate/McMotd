package org.zrnq.mclient

import com.alibaba.fastjson.parser.ParserConfig
import org.zrnq.mclient.output.GUIOutputHandler
import java.awt.Font

lateinit var FONT : Font
lateinit var dnsServerList : List<String>

fun main() {
    FONT = Font("Microsoft YaHei UI", Font.PLAIN, 20)
    dnsServerList = listOf("223.5.5.5", "8.8.8.8", "114.114.114.114")
    ParserConfig.getGlobalInstance().isSafeMode = true
    GUIOutputHandler()
}