
package sky4th.dungeon.loadout.repair.animation.impl

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.bukkit.util.Transformation
import sky4th.dungeon.loadout.repair.animation.RepairAnimation
import sky4th.dungeon.loadout.repair.animation.RepairAnimationConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 磨剑动画实现
 */
class GrindSwordAnimation(
    plugin: Plugin,
    player: Player,
    targetItem: ItemStack,
    repairItem: ItemStack,
    config: RepairAnimationConfig
) : RepairAnimation(plugin, player, targetItem, repairItem, config) {

    private var weaponDisplay: ItemDisplay? = null
    private var repairDisplay: ItemDisplay? = null
    private var grindPhase = 0  // 0: 第一面打磨, 1: 翻转中, 2: 第二面打磨
    private var grindProgress = 0.0  // 打磨进度 0.0-1.0
    private var baseLocation: Location? = null

    // 动画阶段时长分配（总时长的百分比）
    private val firstGrindPhaseRatio = 0.5  // 第一面打磨占总时长的50%
    private val secondGrindPhaseRatio = 0.5 // 第二面打磨占总时长的50%

    override fun initializeAnimation() {
        // 获取玩家视线前方的位置
        baseLocation = getPlayerEyeLocation(1.5)
        val playerLoc = player.location

        // 创建武器显示实体
        weaponDisplay = baseLocation?.world?.spawn(baseLocation!!, ItemDisplay::class.java) { display ->
            display.setItemStack(targetItem.clone())
            // 剑保持默认状态，剑面朝向玩家，剑柄在右下角，剑尖在左上角
            val initialRotation = org.joml.Quaternionf().rotationZ(PI.toFloat() / 3 + PI.toFloat()) 
            val transformation = Transformation(
                org.joml.Vector3f(0f, 0f, 0f),
                initialRotation,
                org.joml.Vector3f(1f, 1f, 1f),
                org.joml.Quaternionf()
            )
            display.setTransformation(transformation)
            display.billboard = Display.Billboard.FIXED
            display.isPersistent = false
        }

        // 创建维修物品显示实体
        val repairLoc = baseLocation?.clone()?.add(Vector(0.0, 0.3, 0.0))

        repairDisplay = baseLocation?.world?.spawn(repairLoc!!, ItemDisplay::class.java) { display ->
            display.setItemStack(repairItem.clone())
            val transformation = Transformation(
                org.joml.Vector3f(0f, 0f, 0f),
                org.joml.Quaternionf(),
                org.joml.Vector3f(0.3f, 0.3f, 0.3f),
                org.joml.Quaternionf()
            )
            display.setTransformation(transformation)
            display.billboard = Display.Billboard.FIXED
            display.isPersistent = false
        }
    }

    override fun updateAnimation() {
        val weapon = weaponDisplay ?: return
        val repair = repairDisplay ?: return

        // 更新动画位置，使其始终跟随玩家
        baseLocation = getPlayerEyeLocation(1.5)
        weapon.teleport(baseLocation!!)
        repair.teleport(baseLocation!!)

        // 计算当前动画进度（0.0-1.0）
        val overallProgress = tick.toDouble() / config.durationTicks.toDouble()

        // 根据整体进度确定当前阶段
        when {
            overallProgress < firstGrindPhaseRatio -> {
                // 第一面打磨阶段
                grindPhase = 0
                // 计算当前阶段的进度（0.0-1.0）
                grindProgress = overallProgress / firstGrindPhaseRatio

                // 剑保持默认状态
                val weaponRotation = org.joml.Quaternionf().rotationZ(PI.toFloat() / 3 + PI.toFloat()) 
                weapon.setTransformation(Transformation(
                    org.joml.Vector3f(0f, 0f, 0f),
                    weaponRotation,
                    org.joml.Vector3f(1f, 1f, 1f),
                    org.joml.Quaternionf()
                ))

                // 燧石在剑刃上平移（只打磨剑刃，从剑柄到剑尖）
                // 剑的长度约为0.7格，剑刃从-0.1格开始到0.6格
                // 剑的高低差约为0.17格，剑刃从0.1格开始到0.27格
                // grindProgress从0到1，对应整个阶段时长
                val grindX = (grindProgress * 0.7 - 0.1).toFloat()
                val grindY = (0.1 + grindProgress * 0.17).toFloat()
                val grindZ = -0.1f

                // 计算当前阶段的tick数和总tick数
                val currentPhaseTicks = tick % (config.durationTicks / 2)
                val phaseTotalTicks = config.durationTicks / 2

                // 燧石稍微倾斜，模拟磨刀角度
                val flintRotation = org.joml.Quaternionf().rotationZ(PI.toFloat() / 6)

                repair.setTransformation(Transformation(
                    org.joml.Vector3f(grindX, grindY, grindZ),
                    flintRotation,
                    org.joml.Vector3f(0.2f, 0.2f, 0.2f),
                    org.joml.Quaternionf()
                ))

                // 生成火花粒子
                if (tick % 5 == 0) {
                    val particleLoc = baseLocation?.clone()?.add(Vector(grindX.toDouble(), grindY.toDouble(), grindZ.toDouble()))
                    spawnParticle(particleLoc!!, Particle.FLAME, 3)
                    // 播放磨刀音效
                    playSound(particleLoc, org.bukkit.Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1.0f)
                }
            }

            overallProgress < 1.0 -> {
                // 第二面打磨阶段（剑的下面）
                grindPhase = 1
                // 计算当前阶段的进度（0.0-1.0）
                grindProgress = (overallProgress - firstGrindPhaseRatio) / secondGrindPhaseRatio

                // 剑保持不变
                val weaponRotation = org.joml.Quaternionf().rotationZ(PI.toFloat() / 3 + PI.toFloat())
                weapon.setTransformation(Transformation(
                    org.joml.Vector3f(0f, 0f, 0f),
                    weaponRotation,
                    org.joml.Vector3f(1f, 1f, 1f),
                    org.joml.Quaternionf()
                ))

                // 燧石在剑刃上平移（只打磨剑刃，从剑柄到剑尖）
                // 剑的长度约为0.7格，剑刃从-0.1格开始到0.6格
                // 剑的高低差约为0.17格，剑刃从0.07格开始到-0.1格
                // grindProgress从0到1，对应整个阶段时长
                val grindX = (0.6 - grindProgress * 0.7).toFloat()
                val grindY = (0.07 - grindProgress * 0.17).toFloat()// 在剑的另一面，所以Y坐标为负
                val grindZ = -0.1f

                // 计算当前阶段的tick数和总tick数
                val currentPhaseTicks = tick - (config.durationTicks / 2)
                val phaseTotalTicks = config.durationTicks / 2

                val flintRotation = org.joml.Quaternionf().rotationZ(PI.toFloat() / 6)

                repair.setTransformation(Transformation(
                    org.joml.Vector3f(grindX, grindY, grindZ),
                    flintRotation,
                    org.joml.Vector3f(0.2f, 0.2f, 0.2f),
                    org.joml.Quaternionf()
                ))

                // 生成火花粒子
                if (tick % 5 == 0) {
                    val particleLoc = baseLocation?.clone()?.add(Vector(grindX.toDouble(), grindY.toDouble(), grindZ.toDouble()))
                    spawnParticle(particleLoc!!, Particle.FLAME, 3)
                    // 播放磨刀音效
                    playSound(particleLoc, org.bukkit.Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1.0f)
                }
            }
        }
    }

    override fun completeAnimation() {
        // 播放完成音效（铁砧成功）
        baseLocation?.let {
            playSound(it, org.bukkit.Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f)
        }
        super.completeAnimation()
    }

    override fun cleanupAnimation() {
        weaponDisplay?.remove()
        repairDisplay?.remove()
    }
}
