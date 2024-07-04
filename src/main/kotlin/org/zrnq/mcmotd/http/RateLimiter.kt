package org.zrnq.mcmotd.http

import org.zrnq.mcmotd.configStorage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

object RateLimiter {
    class AccessRecord {
        var success = 0
        var total = 0
        fun update(success: Boolean) {
            total++
            if(success) this.success++
        }
    }
    private var requestCoolDownRecord = Collections.synchronizedMap(HashMap<String, Long>())
    private var nextCleanup = 0L
    private var requestRecord = Collections.synchronizedMap(HashMap<String, AccessRecord>())
    private var nextRecordRefresh = 0L
    private val format = SimpleDateFormat("MM/dd HH:mm:ss")
    private fun recordRequest(address: String, success: Boolean) {
        if(configStorage.httpServerAccessRecordRefresh == 0) return
        val timeNow = System.currentTimeMillis()
        if(timeNow > nextRecordRefresh) {
            requestRecord.clear()
            nextRecordRefresh = timeNow + TimeUnit.SECONDS.toMillis(configStorage.httpServerAccessRecordRefresh.toLong())
        }
        requestRecord.getOrPut(address) { AccessRecord() }.update(success)
    }
    fun getRecordData() : String {
        return "%s - %s\n%s".format(
            format.format(Date(nextRecordRefresh - TimeUnit.SECONDS.toMillis(configStorage.httpServerAccessRecordRefresh.toLong()))),
            format.format(Date(nextRecordRefresh)),
            if(requestRecord.isEmpty()) "统计时间段内没有访问记录"
            else synchronized(requestRecord) {
                requestRecord.entries.stream()
                    .sorted { o1, o2 -> o2.value.total - o1.value.total }
                    .limit(10)
                    .map { "${it.key}: ${it.value.total}(${it.value.success})" }
                    .collect(Collectors.joining("\n"))
            })
    }
    fun pass(address : String) : Boolean = run {
        if(configStorage.httpServerRequestCoolDown == 0) return@run true // cool down disabled
        val lastAccessRecord = requestCoolDownRecord[address]
        val timeNow = System.currentTimeMillis()
        if(lastAccessRecord == null || lastAccessRecord < timeNow) {
            if(requestCoolDownRecord.size > configStorage.httpServerParallelRequest) {
                if(nextCleanup > timeNow) return@run false // reaching parallel request limit
                // Clean up records
                synchronized(requestCoolDownRecord) {
                    val it = requestCoolDownRecord.iterator()
                    while(it.hasNext()) {
                        if(it.next().value < timeNow) it.remove()
                    }
                }
                nextCleanup = timeNow + configStorage.httpServerRequestCoolDown
            }
            requestCoolDownRecord[address] = timeNow + configStorage.httpServerRequestCoolDown
            return@run true
        } else return@run false // cool down incomplete
    }.also { recordRequest(address, it) }
}