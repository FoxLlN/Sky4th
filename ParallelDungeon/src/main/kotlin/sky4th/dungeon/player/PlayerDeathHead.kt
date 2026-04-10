
package sky4th.dungeon.player

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.Dungeon
import sky4th.core.api.LanguageAPI
import java.util.UUID

/**
 * 玩家死亡头颅工具类
 * 用于创建玩家死亡时掉落的头颅
 */
object PlayerDeathHead {
    private val deathHeadKey: NamespacedKey by lazy {
        NamespacedKey(sky4th.dungeon.Dungeon.instance, "player_death_head")
    }
    private val playerUuidKey: NamespacedKey by lazy {
        NamespacedKey(sky4th.dungeon.Dungeon.instance, "player_uuid")
    }
    private val playerNameKey: NamespacedKey by lazy {
        NamespacedKey(sky4th.dungeon.Dungeon.instance, "player_name")
    }


    /**
     * 创建玩家死亡头颅
     * @param playerName 玩家名称
     * @param playerUuid 玩家UUID
     * @return 玩家死亡头颅物品
     */
    fun create(playerName: String, playerUuid: UUID): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as? SkullMeta ?: return head

        // 设置头颅拥有者
        meta.owningPlayer = Bukkit.getOfflinePlayer(playerUuid)

        // 设置显示名称
        meta.displayName(LanguageAPI.getComponent(Dungeon.instance, "death.head.name", "player" to playerName))

        // 设置PDC标记
        meta.persistentDataContainer.set(deathHeadKey, PersistentDataType.BYTE, 1)
        meta.persistentDataContainer.set(playerUuidKey, PersistentDataType.STRING, playerUuid.toString())
        meta.persistentDataContainer.set(playerNameKey, PersistentDataType.STRING, playerName)

        head.itemMeta = meta
        return head
    }

    /**
     * 检查物品是否为玩家死亡头颅
     */
    fun isDeathHead(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.PLAYER_HEAD) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(deathHeadKey, PersistentDataType.BYTE)
    }

    /**
     * 从死亡头颅获取玩家UUID
     */
    fun getPlayerUuid(item: ItemStack): UUID? {
        if (!isDeathHead(item)) return null
        val meta = item.itemMeta ?: return null
        val uuidStr = meta.persistentDataContainer.get(playerUuidKey, PersistentDataType.STRING) ?: return null
        return try {
            UUID.fromString(uuidStr)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从死亡头颅获取玩家名称
     */
    fun getPlayerName(item: ItemStack): String? {
        if (!isDeathHead(item)) return null
        val meta = item.itemMeta ?: return null
        return meta.persistentDataContainer.get(playerNameKey, PersistentDataType.STRING)
    }
}
