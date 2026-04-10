
package com.sky4th.equipment.monitor

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 性能监控器
 * 用于监控装备系统的性能指标
 */
object PerformanceMonitor {

    // 性能指标存储
    private val metrics = ConcurrentHashMap<String, MetricData>()

    /**
     * 记录操作耗时
     * @param operation 操作名称
     * @param duration 耗时（纳秒）
     */
    fun recordOperation(operation: String, duration: Long) {
        val data = metrics.computeIfAbsent(operation) { MetricData() }
        data.record(duration)
    }

    /**
     * 获取指定操作的性能指标
     * @param operation 操作名称
     * @return 性能指标数据
     */
    fun getMetrics(operation: String): MetricData? {
        return metrics[operation]
    }

    /**
     * 获取所有性能指标
     * @return 所有性能指标的映射
     */
    fun getAllMetrics(): Map<String, MetricData> {
        return metrics.toMap()
    }

    /**
     * 清空所有性能指标
     */
    fun clear() {
        metrics.clear()
    }

    /**
     * 打印性能报告
     */
    fun printReport() {
        println("=== 装备系统性能报告 ===")
        metrics.forEach { (operation, data) ->
            println("$operation:")
            println("  平均耗时: ${data.getAverage()} ns")
            println("  最大耗时: ${data.getMax()} ns")
            println("  最小耗时: ${data.getMin()} ns")
            println("  调用次数: ${data.getCount()}")
            println("  总耗时: ${data.getTotal()} ns")
            println()
        }
        println("=========================")
    }

    /**
     * 性能指标数据
     */
    class MetricData {
        private val samples = mutableListOf<Long>()
        private val count = AtomicLong(0)
        private val total = AtomicLong(0)
        private val min = AtomicLong(Long.MAX_VALUE)
        private val max = AtomicLong(0)

        /**
         * 记录一次操作
         * @param duration 耗时（纳秒）
         */
        fun record(duration: Long) {
            count.incrementAndGet()
            total.addAndGet(duration)

            // 更新最小值
            while (true) {
                val current = min.get()
                if (duration >= current || min.compareAndSet(current, duration)) {
                    break
                }
            }

            // 更新最大值
            while (true) {
                val current = max.get()
                if (duration <= current || max.compareAndSet(current, duration)) {
                    break
                }
            }

            // 保存样本（最多保留1000个）
            synchronized(samples) {
                samples.add(duration)
                if (samples.size > 1000) {
                    samples.removeAt(0)
                }
            }
        }

        /**
         * 获取平均耗时
         * @return 平均耗时（纳秒）
         */
        fun getAverage(): Double {
            val currentCount = count.get()
            return if (currentCount > 0) {
                total.get().toDouble() / currentCount
            } else {
                0.0
            }
        }

        /**
         * 获取最大耗时
         * @return 最大耗时（纳秒）
         */
        fun getMax(): Long {
            return max.get()
        }

        /**
         * 获取最小耗时
         * @return 最小耗时（纳秒）
         */
        fun getMin(): Long {
            val current = min.get()
            return if (current == Long.MAX_VALUE) 0 else current
        }

        /**
         * 获取调用次数
         * @return 调用次数
         */
        fun getCount(): Long {
            return count.get()
        }

        /**
         * 获取总耗时
         * @return 总耗时（纳秒）
         */
        fun getTotal(): Long {
            return total.get()
        }
    }
}
