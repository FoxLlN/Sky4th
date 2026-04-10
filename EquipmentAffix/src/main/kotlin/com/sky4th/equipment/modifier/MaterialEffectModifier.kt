
package com.sky4th.equipment.modifier

/**
 * 材质效果词条接口
 * 标识这是一个材质效果词条
 * 材质效果词条具有以下特性：
 * 1. 自动添加到装备，不占用普通词条槽位
 * 2. 每个装备只能有一个材质效果词条
 * 3. 由装备的materialEffect属性决定
 * 4. 具体行为由实现类定义
 */
interface MaterialEffectModifier {
    /**
     * 获取材质效果ID
     * @return 材质效果ID，与装备配置中的material_effect对应
     */
    fun getMaterialEffectId(): String
}
