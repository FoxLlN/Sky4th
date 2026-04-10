package sky4th.core.util

/**
 * 颜色工具类
 * 提供颜色格式的转换功能
 */
object ColorUtil {

    private val legacyColorRegex by lazy { "&([0-9a-fklmnor])".toRegex(RegexOption.IGNORE_CASE) }

    /**
     * 将任意格式（旧格式 &、§ 或 MiniMessage）转换为传统 § 颜色代码
     * 支持标准颜色、格式代码和 RGB 颜色（转换为 §x 格式）
     * 
     * @param text 原始文本
     * @return 转换后的文本
     */
    @JvmStatic
    fun convertMiniMessageToLegacy(text: String): String {
        var result = text
        
        // 第一步：直接将 & 转换为 §
        result = result.replace("&", "§")
        
        // 第二步：处理 RGB 颜色格式 <#RRGGBB>
        val hexColorRegex = "<#([0-9a-fA-F]{6})>".toRegex()
        result = hexColorRegex.replace(result) { matchResult ->
            val hex = matchResult.groupValues[1]
            "§x§${hex[0]}§${hex[1]}§${hex[2]}§${hex[3]}§${hex[4]}§${hex[5]}"
        }
        
        // 第三步：处理标准颜色和格式代码
        val colorMap = mapOf(
            // 颜色代码
            "<black>" to "§0",
            "<dark_blue>" to "§1",
            "<dark_green>" to "§2",
            "<dark_aqua>" to "§3",
            "<dark_red>" to "§4",
            "<dark_purple>" to "§5",
            "<gold>" to "§6",
            "<gray>" to "§7",
            "<dark_gray>" to "§8",
            "<blue>" to "§9",
            "<green>" to "§a",
            "<aqua>" to "§b",
            "<red>" to "§c",
            "<light_purple>" to "§d",
            "<yellow>" to "§e",
            "<white>" to "§f",
            // 格式代码
            "<bold>" to "§l",
            "<italic>" to "§o",
            "<underlined>" to "§n",
            "<strikethrough>" to "§m",
            "<obfuscated>" to "§k",
            "<reset>" to "§r"
        )
        
        colorMap.forEach { (mini, legacy) ->
            result = result.replace(mini, legacy)
        }
        
        return result
    }
    
    /**
     * 将传统 & 颜色代码转换为 MiniMessage 标签
     * 同时处理 && 转义（输出单个 &）
     * 
     * @param text 原始文本
     * @return 转换后的文本
     */
    @JvmStatic
    fun convertLegacyToMiniMessage(text: String): String {
        // 第一步：将 && 转义为临时占位符（避免被转换）
        val escaped = text.replace("&&", "\u0000")

        // 第二步：转换 & 颜色代码
        val converted = legacyColorRegex.replace(escaped) { matchResult ->
            val code = matchResult.groupValues[1].lowercase()
            when (code) {
                // 颜色代码
                "0" -> "<black>"
                "1" -> "<dark_blue>"
                "2" -> "<dark_green>"
                "3" -> "<dark_aqua>"
                "4" -> "<dark_red>"
                "5" -> "<dark_purple>"
                "6" -> "<gold>"
                "7" -> "<gray>"
                "8" -> "<dark_gray>"
                "9" -> "<blue>"
                "a" -> "<green>"
                "b" -> "<aqua>"
                "c" -> "<red>"
                "d" -> "<light_purple>"
                "e" -> "<yellow>"
                "f" -> "<white>"
                // 格式代码
                "l" -> "<bold>"
                "o" -> "<italic>"
                "n" -> "<underlined>"
                "m" -> "<strikethrough>"
                "k" -> "<obfuscated>"
                "r" -> "<reset>"
                else -> matchResult.value // 不会发生，但保留以防万一
            }
        }

        // 第三步：还原 && 转义（将占位符替换回 &）
        return converted.replace("\u0000", "&")
    }
}
