package sky4th.core.lang

import java.util.*
import java.util.regex.Pattern

/**
 * Sky4th 变量替换工具
 * 
 * 支持多种变量格式：
 * - {变量名} - 单大括号
 * - {{变量名}} - 双大括号
 * - %变量名% - 百分号
 */

/**
 * 变量读取器
 */
data class VariableReader(
    val start: String,
    val end: String
) {
    private val pattern: Pattern by lazy {
        val escapedStart = Pattern.quote(start)
        val escapedEnd = Pattern.quote(end)
        Pattern.compile("$escapedStart([^${Pattern.quote(start)}${Pattern.quote(end)}]+)$escapedEnd")
    }
    
    /**
     * 替换嵌套变量
     */
    fun replaceNested(text: String, replacer: (String) -> String): String {
        var result = text
        var changed = true
        
        while (changed) {
            val matcher = pattern.matcher(result)
            val buffer = StringBuffer()
            changed = false
            
            while (matcher.find()) {
                val variableName = matcher.group(1)
                val replacement = replacer(variableName)
                matcher.appendReplacement(buffer, replacement)
                if (replacement != matcher.group(0)) {
                    changed = true
                }
            }
            matcher.appendTail(buffer)
            result = buffer.toString()
        }
        
        return result
    }
}

/**
 * 预定义的变量读取器
 */
object VariableReaders {
    val BRACES = VariableReader("{", "}")
    val DOUBLE_BRACES = VariableReader("{{", "}}")
    val PERCENT = VariableReader("%", "%")
}

/**
 * 变量函数接口
 */
fun interface VariableFunction {
    /**
     * 转换变量名
     * @param name 变量名
     * @return 替换后的值集合，如果变量不存在则返回 null
     */
    fun transfer(name: String): Collection<String>?
}

/**
 * 批量处理变量替换
 * 
 * @param reader 变量读取器
 * @param func 变量转换函数
 * @return 处理后的文本列表
 */
fun Collection<String>.variables(reader: VariableReader = VariableReaders.BRACES, func: VariableFunction): List<String> {
    return flatMap { context ->
        val result = ArrayList<String>()
        val queued = HashMap<String, Queue<String>>()
        
        // 第一遍扫描：收集所有变量
        reader.replaceNested(context) { variableName ->
            val values = func.transfer(variableName)
            if (values != null) {
                queued[variableName] = LinkedList(values)
            }
            reader.start + variableName + reader.end
        }
        
        // 如果没有变量，直接返回原文本
        if (queued.isEmpty()) {
            return@flatMap listOf(context)
        }
        
        // 生成所有组合
        while (queued.any { (_, queue) -> queue.isNotEmpty() }) {
            val replaced = reader.replaceNested(context) { variableName ->
                if (variableName in queued) {
                    val queue = queued[variableName]!!
                    val value = queue.peek() ?: ""
                    if (queue.isNotEmpty()) {
                        queue.poll()
                    }
                    value
                } else {
                    reader.start + variableName + reader.end
                }
            }
            result += replaced
        }
        
        result
    }
}

/**
 * 单个变量替换
 */
fun Collection<String>.variable(key: String, value: Collection<String>, reader: VariableReader = VariableReaders.BRACES): List<String> {
    return variables(reader) { if (it == key) value else null }
}

/**
 * 简单变量替换（单个值）
 */
fun Collection<String>.variable(key: String, value: String, reader: VariableReader = VariableReaders.BRACES): List<String> {
    return variable(key, listOf(value), reader)
}

/**
 * 批量变量替换（Map 格式）
 */
fun Collection<String>.variables(variables: Map<String, Collection<String>>, reader: VariableReader = VariableReaders.BRACES): List<String> {
    return variables(reader) { variables[it] }
}
