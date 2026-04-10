package sky4th.dungeon.loadout.equipment

import org.bukkit.Material

/** 手持该材质右键可穿戴到护甲槽（头盔/胸甲/护腿/靴子等） */
fun isEquippableArmor(type: Material): Boolean {
    if (type.isEmpty) return false
    val n = type.name
    return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS") ||
        type == Material.ELYTRA || type == Material.CARVED_PUMPKIN || type == Material.TURTLE_HELMET
}
