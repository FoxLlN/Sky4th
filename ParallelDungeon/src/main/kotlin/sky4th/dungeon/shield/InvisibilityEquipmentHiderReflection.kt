package sky4th.dungeon.shield

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * 处理玩家隐身时装备也隐身的功能
 *
 * 当玩家获得隐身效果时，自动隐藏身上的装备
 * 当玩家失去隐身效果时，自动恢复装备显示
 *
 * 使用反射访问NMS实现，不依赖第三方插件
 *
 * 原理：通过发送ClientboundSetEquipmentPacket数据包来隐藏装备显示，
 * 但不实际修改玩家的装备，因此装备属性、护甲值和附魔效果都保持不变
 */
class InvisibilityEquipmentHiderReflection(private val plugin: JavaPlugin) : Listener {

    // 记录玩家的检查任务
    private val playerCheckTasks = mutableMapOf<UUID, BukkitTask>()

    // 记录玩家装备状态
    private val playerEquipmentState = mutableMapOf<UUID, EquipmentState>()

    // 装备状态数据类
    private data class EquipmentState(
        val isHidden: Boolean
    )

    // 缓存反射类和方法
    private val craftPlayerClass: Class<*> by lazy { Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer") }
    private val handleMethod by lazy { craftPlayerClass.getMethod("getHandle") }
    private val getIdMethod by lazy { 
        val entityPlayerClass = Class.forName("net.minecraft.server.level.EntityPlayer")
        entityPlayerClass.getMethod("getId")
    }
    private val connectionField by lazy { 
        val entityPlayerClass = Class.forName("net.minecraft.server.level.EntityPlayer")
        val field = entityPlayerClass.getDeclaredField("b")
        field.isAccessible = true
        field
    }
    private val sendMethod by lazy { 
        val connectionClass = Class.forName("net.minecraft.network.PlayerConnection")
        connectionClass.getMethod("a", Class.forName("net.minecraft.network.protocol.Packet"))
    }
    private val equipmentSlotClass by lazy { Class.forName("net.minecraft.world.entity.EnumItemSlot") }
    private val itemStackClass by lazy { Class.forName("net.minecraft.world.item.ItemStack") }
    private val emptyItemStack by lazy { 
        val field = itemStackClass.getDeclaredField("b")
        field.isAccessible = true
        field.get(null)
    }
    private val packetClass by lazy { Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment") }
    private val packetConstructor by lazy { packetClass.getConstructor(Int::class.java, List::class.java) }

    /**
     * 处理玩家离开服务器事件
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId

        // 取消检查任务
        playerCheckTasks[uuid]?.cancel()
        playerCheckTasks.remove(uuid)

        // 清除装备状态
        playerEquipmentState.remove(uuid)
    }

    /**
     * 处理玩家使用物品事件（如饮用隐身药水）
     */
    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        val player = event.player

        // 延迟检查玩家状态，确保药水效果已经应用
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            checkPlayerInvisibilityState(player)
        }, 1L)
    }

    /**
     * 检查玩家隐身状态并更新装备显示
     */
    private fun checkPlayerInvisibilityState(player: Player) {
        val uuid = player.uniqueId
        val isInvisible = player.hasPotionEffect(PotionEffectType.INVISIBILITY)

        if (isInvisible) {
            // 如果玩家隐身且装备未隐藏，则隐藏装备
            if (!playerEquipmentState.containsKey(uuid)) {
                hidePlayerEquipment(player)
                startInvisibilityCheck(player)
            }
        } else {
            // 如果玩家不隐身且装备已隐藏，则恢复装备
            if (playerEquipmentState.containsKey(uuid)) {
                showPlayerEquipment(player)
                stopInvisibilityCheck(player)
            }
        }
    }

    /**
     * 隐藏玩家装备
     */
    private fun hidePlayerEquipment(player: Player) {
        val uuid = player.uniqueId

        // 保存当前装备状态
        playerEquipmentState[uuid] = EquipmentState(isHidden = true)

        try {
            // 获取CraftPlayer
            val craftPlayer = craftPlayerClass.cast(player)

            // 获取EntityPlayer
            val entityPlayer = handleMethod.invoke(craftPlayer)

            // 获取实体ID
            val entityId = getIdMethod.invoke(entityPlayer) as Int

            // 创建空的装备列表
            val equipmentSlots = mutableListOf<Pair<Any, Any>>()

            // 隐藏所有装备槽
            equipmentSlotClass.enumConstants.forEach { slot ->
                equipmentSlots.add(Pair(slot, emptyItemStack))
            }

            // 创建数据包
            val packet = packetConstructor.newInstance(entityId, equipmentSlots)

            // 发送数据包给所有其他玩家
            for (target in Bukkit.getOnlinePlayers()) {
                if (target != player) {
                    val targetCraftPlayer = craftPlayerClass.cast(target)
                    val targetEntityPlayer = handleMethod.invoke(targetCraftPlayer)
                    val targetConnection = connectionField.get(targetEntityPlayer)
                    sendMethod.invoke(targetConnection, packet)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 显示玩家装备
     */
    private fun showPlayerEquipment(player: Player) {
        val uuid = player.uniqueId

        try {
            // 获取CraftPlayer
            val craftPlayer = craftPlayerClass.cast(player)

            // 获取EntityPlayer
            val entityPlayer = handleMethod.invoke(craftPlayer)

            // 获取实体ID
            val entityId = getIdMethod.invoke(entityPlayer) as Int

            // 创建装备列表
            val equipmentSlots = mutableListOf<Pair<Any, Any>>()

            // 获取EntityPlayer的getItemBySlot方法
            val getItemBySlotMethod = entityPlayer.javaClass.getMethod("b", equipmentSlotClass)

            // 获取所有装备槽
            val mainHand = equipmentSlotClass.enumConstants[0] // EnumItemSlot.a
            val offHand = equipmentSlotClass.enumConstants[1] // EnumItemSlot.b
            val feet = equipmentSlotClass.enumConstants[2] // EnumItemSlot.c
            val legs = equipmentSlotClass.enumConstants[3] // EnumItemSlot.d
            val chest = equipmentSlotClass.enumConstants[4] // EnumItemSlot.e
            val head = equipmentSlotClass.enumConstants[5] // EnumItemSlot.f

            // 获取玩家当前装备
            equipmentSlots.add(Pair(mainHand, getItemBySlotMethod.invoke(entityPlayer, mainHand)))
            equipmentSlots.add(Pair(offHand, getItemBySlotMethod.invoke(entityPlayer, offHand)))
            equipmentSlots.add(Pair(feet, getItemBySlotMethod.invoke(entityPlayer, feet)))
            equipmentSlots.add(Pair(legs, getItemBySlotMethod.invoke(entityPlayer, legs)))
            equipmentSlots.add(Pair(chest, getItemBySlotMethod.invoke(entityPlayer, chest)))
            equipmentSlots.add(Pair(head, getItemBySlotMethod.invoke(entityPlayer, head)))

            // 创建数据包
            val packet = packetConstructor.newInstance(entityId, equipmentSlots)

            // 发送数据包给所有其他玩家
            for (target in Bukkit.getOnlinePlayers()) {
                if (target != player) {
                    val targetCraftPlayer = craftPlayerClass.cast(target)
                    val targetEntityPlayer = handleMethod.invoke(targetCraftPlayer)
                    val targetConnection = connectionField.get(targetEntityPlayer)
                    sendMethod.invoke(targetConnection, packet)
                }
            }

            // 清除装备状态
            playerEquipmentState.remove(uuid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 开始检查玩家隐身状态
     */
    private fun startInvisibilityCheck(player: Player) {
        val uuid = player.uniqueId

        // 取消之前的检查任务
        playerCheckTasks[uuid]?.cancel()

        // 创建新的检查任务
        val checkTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            checkPlayerInvisibilityState(player)
        }, 20L, 20L) // 每秒检查一次

        playerCheckTasks[uuid] = checkTask
    }

    /**
     * 停止检查玩家隐身状态
     */
    private fun stopInvisibilityCheck(player: Player) {
        val uuid = player.uniqueId

        // 取消检查任务
        playerCheckTasks[uuid]?.cancel()
        playerCheckTasks.remove(uuid)
    }
}
