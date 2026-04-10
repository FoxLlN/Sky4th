package com.sky4th.equipment.util

import com.sky4th.equipment.modifier.AffixConfig

/**
 * 描述评估工具类
 * 用于处理词条描述中的表达式计算
 */
object DescriptionEvaluator {

    /**
     * 处理描述中的表达式，将{expression}替换为计算结果
     * 
     * @param rawDescription 原始描述
     * @param maxLevel 最大等级
     * @return 处理后的描述
     */
    fun evaluateDescription(rawDescription: String, maxLevel: Int): String {
        return rawDescription.replace(Regex("""\{([^}]+)\}""")) { matchResult ->
            val expression = matchResult.groupValues[1]
            // 第一步：将level替换为maxLevel
            val replacedExpression = expression.replace("level", maxLevel.toString())
            // 第二步：移除百分号
            val withoutPercent = replacedExpression.replace("%", "")
            // 第三步：计算表达式（使用正则表达式解析）
            try {
                val result = evaluateMathExpression(withoutPercent)
                // 格式化结果，如果是整数则不显示小数
                if (result == result.toLong().toFloat()) {
                    result.toLong().toString()
                } else {
                    String.format("%.1f", result)
                }
            } catch (e: Exception) {
                matchResult.value // 如果计算失败，保持原样
            }
        }
    }

    /**
     * 获取处理后的描述（优先使用详细描述）
     * 
     * @param affixConfig 词条配置
     * @return 处理后的描述
     */
    fun getEvaluatedDescription(affixConfig: AffixConfig, level: Int): String {
        // 获取原始描述
        val rawDescription = if (affixConfig.simpleDescription.isNotEmpty()) {
            affixConfig.simpleDescription
        } else {
            // 如果没有详细描述，剔除描述的前9个字符（颜色代码），替换为灰色
            if (affixConfig.description.length >= 9 && affixConfig.description.startsWith("<#")) {
                "&7${affixConfig.description.substring(9)}"
            } else {
                "&7${affixConfig.description}"
            }
        }

        // 计算表达式
        return evaluateDescription(rawDescription, level)
    }

    /**
     * 计算数学表达式，支持加减乘除和运算符优先级
     */
    private fun evaluateMathExpression(expression: String): Float {
        // 移除所有空格
        var expr = expression.replace(" ", "")

        // 处理括号
        while (expr.contains("(")) {
            val start = expr.lastIndexOf("(")
            val end = expr.indexOf(")", start)
            if (end == -1) break

            val subExpr = expr.substring(start + 1, end)
            val result = evaluateSimpleMathExpression(subExpr)
            expr = expr.substring(0, start) + result + expr.substring(end + 1)
        }

        return evaluateSimpleMathExpression(expr)
    }

    /**
     * 计算简单数学表达式（无括号），支持加减乘除和运算符优先级
     */
    private fun evaluateSimpleMathExpression(expression: String): Float {
        var expr = expression

        // 先处理乘除
        val mulDivPattern = Regex("""(-?\d+\.?\d*)([*/])(-?\d+\.?\d*)""")
        while (mulDivPattern.containsMatchIn(expr)) {
            expr = mulDivPattern.replace(expr) { match ->
                val num1 = match.groupValues[1].toFloat()
                val op = match.groupValues[2]
                val num2 = match.groupValues[3].toFloat()
                when (op) {
                    "*" -> (num1 * num2).toString()
                    "/" -> (num1 / num2).toString()
                    else -> match.value
                }
            }
        }

        // 再处理加减
        val addSubPattern = Regex("""(-?\d+\.?\d*)([+-])(-?\d+\.?\d*)""")
        while (addSubPattern.containsMatchIn(expr)) {
            expr = addSubPattern.replace(expr) { match ->
                val num1 = match.groupValues[1].toFloat()
                val op = match.groupValues[2]
                val num2 = match.groupValues[3].toFloat()
                when (op) {
                    "+" -> (num1 + num2).toString()
                    "-" -> (num1 - num2).toString()
                    else -> match.value
                }
            }
        }

        return expr.toFloat()
    }
}
