package sky4th.core.api

import sky4th.core.SkyCore
import sky4th.core.model.PlayerAttributes
import sky4th.core.service.PlayerAttributesService
import java.util.*

/**
 * 玩家属性 API
 * 提供便捷的玩家属性访问接口
 */
object PlayerAttributesAPI {

    /**
     * 获取玩家属性服务
     */
    private fun getService(): PlayerAttributesService? {
        return SkyCore.getPlayerAttributesService()
    }

    /**
     * 获取玩家属性
     * @param uuid 玩家UUID
     * @return 玩家属性
     */
    fun getAttributes(uuid: UUID): PlayerAttributes {
        return getService()?.getAttributes(uuid)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 保存玩家属性
     * @param attributes 玩家属性
     */
    fun saveAttributes(attributes: PlayerAttributes) {
        getService()?.saveAttributes(attributes)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 保存玩家属性（通过UUID）
     * @param uuid 玩家UUID
     */
    fun saveAttributes(uuid: UUID) {
        getService()?.saveAttributes(uuid)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新最大生命值
     * @param uuid 玩家UUID
     * @param value 新值
     */
    fun updateMaxHealth(uuid: UUID, value: Double) {
        getService()?.updateMaxHealth(uuid, value)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新护甲值
     * @param uuid 玩家UUID
     * @param value 新值
     */
    fun updateArmor(uuid: UUID, value: Double) {
        getService()?.updateArmor(uuid, value)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新闪避率
     * @param uuid 玩家UUID
     * @param value 新值 (0.0-1.0)
     */
    fun updateDodge(uuid: UUID, value: Double) {
        getService()?.updateDodge(uuid, value)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新击退抗性
     * @param uuid 玩家UUID
     * @param value 新值 (0.0-1.0)
     */
    fun updateKnockbackResistance(uuid: UUID, value: Double) {
        getService()?.updateKnockbackResistance(uuid, value)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新饥饿消耗倍率
     * @param uuid 玩家UUID
     * @param value 新值
     */
    fun updateHungerConsumptionMultiplier(uuid: UUID, value: Double) {
        getService()?.updateHungerConsumptionMultiplier(uuid, value)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新移动速度倍率
     * @param uuid 玩家UUID
     * @param value 新值
     */
    fun updateMovementSpeedMultiplier(uuid: UUID, value: Double) {
        getService()?.updateMovementSpeedMultiplier(uuid, value)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新经验获取加成
     * @param uuid 玩家UUID
     * @param value 新值
     */
    fun updateExpGainMultiplier(uuid: UUID, value: Double) {
        getService()?.updateExpGainMultiplier(uuid, value)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新交易折扣
     * @param uuid 玩家UUID
     * @param value 新值 (0.0-1.0)
     */
    fun updateTradeDiscount(uuid: UUID, value: Double) {
        getService()?.updateTradeDiscount(uuid, value)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新锻造成功率
     * @param uuid 玩家UUID
     * @param value 新值 (0.0-1.0)
     */
    fun updateForgingSuccessRate(uuid: UUID, value: Double) {
        getService()?.updateForgingSuccessRate(uuid, value)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 更新天赋列表
     * @param uuid 玩家UUID
     * @param talents 天赋列表
     */
    fun updateTalents(uuid: UUID, talents: List<String>) {
        getService()?.updateTalents(uuid, talents)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 添加天赋
     * @param uuid 玩家UUID
     * @param talent 天赋ID
     */
    fun addTalent(uuid: UUID, talent: String) {
        getService()?.addTalent(uuid, talent)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 移除天赋
     * @param uuid 玩家UUID
     * @param talent 天赋ID
     */
    fun removeTalent(uuid: UUID, talent: String) {
        getService()?.removeTalent(uuid, talent)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 从缓存中移除玩家属性
     * @param uuid 玩家UUID
     */
    fun removeFromCache(uuid: UUID) {
        getService()?.removeFromCache(uuid)
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 保存所有缓存中的玩家属性
     */
    fun saveAll() {
        getService()?.saveAll()
            ?: throw IllegalStateException("PlayerAttributesService 未初始化")
    }

    /**
     * 检查玩家属性是否存在
     * @param uuid 玩家UUID
     * @return 是否存在
     */
    fun attributesExists(uuid: UUID): Boolean {
        return getService()?.attributesExists(uuid) ?: false
    }

    /**
     * 检查 PlayerAttributesService 是否可用
     */
    fun isAvailable(): Boolean {
        return getService() != null
    }
}
