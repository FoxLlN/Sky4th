
package com.sky4th.equipment.modifier.config

/**
 * 玩家在事件中的角色枚举
 */
enum class PlayerRole {
    /** 攻击者 */
    ATTACKER,

    /** 防御者/受害者 */
    DEFENDER,

    /** 其他角色 */
    OTHER,

    /** 自身（单个玩家事件） */
    SELF
}
