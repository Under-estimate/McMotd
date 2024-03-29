package org.zrnq.mclient

import com.alibaba.fastjson.parser.ParserConfig
import org.zrnq.mclient.output.GUIOutputHandler
import java.awt.Font

fun main() {
    MClientOptions.FONT = Font("Microsoft YaHei UI", Font.PLAIN, 20)
    ParserConfig.getGlobalInstance().isSafeMode = true
    GUIOutputHandler()
}