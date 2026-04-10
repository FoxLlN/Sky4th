
package com.sky4th.equipment.monitor

/**
 * 性能监控辅助类
 * 使用use块自动记录操作耗时
 */
class PerformanceMonitorHelper(private val operation: String) {

    private val startTime = System.nanoTime()

    /**
     * 结束监控并记录耗时
     */
    fun end() {
        val duration = System.nanoTime() - startTime
        PerformanceMonitor.recordOperation(operation, duration)
    }

    companion object {
        /**
         * 监控操作
         * @param operation 操作名称
         * @param block 要执行的代码块
         * @return 代码块的返回值
         */
        inline fun <T> monitor(operation: String, block: () -> T): T {
            val helper = PerformanceMonitorHelper(operation)
            return try {
                block()
            } finally {
                helper.end()
            }
        }
    }
}
