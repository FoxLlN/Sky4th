package sky4th.dungeon.monster.head

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import sky4th.dungeon.config.MonsterHeadConfig as ConfigMonsterHeadConfig
import sky4th.dungeon.util.CreditsHeadUtil

/**
 * 怪物头颅工厂
 *
 * 功能：
 * - 为每种怪物类型创建对应的头颅
 * - 对于没有对应头颅的怪物，使用自定义玩家头颅显示特定皮肤
 */
object MonsterHeadFactory {

    /**
     * 根据怪物ID获取对应的头颅物品
     * @param monsterId 怪物ID
     * @param dungeonId 地牢ID（可选，用于优化加载）
     */
    fun getMonsterHead(monsterId: String, dungeonId: String? = null): ItemStack? {
        // 使用 MonsterHeadConfig 从所有地牢中查找
        val config = MonsterHeadConfig.findMonsterHeadConfigPublic(monsterId) ?: return null
        return createHeadFromConfig(config)
    }

    /**
     * 根据配置创建头颅物品
     */
    private fun createHeadFromConfig(config: ConfigMonsterHeadConfig): ItemStack? {
        return when (config.headType.lowercase()) {
            "standard" -> {
                try {
                    val material = Material.valueOf(config.material.uppercase())
                    createStandardHead(material, config.displayName)
                } catch (e: IllegalArgumentException) {
                    // 忽略无效的材质名称
                    null
                }
            }
            "custom" -> {
                CreditsHeadUtil.createCustomHead(config.displayName, config.texture)
            }
            else -> {
                // 忽略未知的头颅类型
                null
            }
        }
    }

    /**
     * 创建标准头颅（如僵尸头、骷髅头等）
     */
    private fun createStandardHead(material: Material, displayName: String): ItemStack {
        val head = ItemStack(material)
        val meta = head.itemMeta
        meta?.displayName(MiniMessage.miniMessage().deserialize(displayName))
        head.itemMeta = meta
        return head
    }
}
