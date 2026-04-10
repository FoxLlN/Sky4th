package sky4th.dungeon.loadout.weapon

/**
 * 配装武器效果注册表：按 loadoutId 查找 [LoadoutWeaponEffectHandler]。
 * 新增武器效果时在此注册即可。
 */
object LoadoutWeaponEffectRegistry {
    private val handlers: MutableMap<String, LoadoutWeaponEffectHandler> = mutableMapOf()

    fun register(handler: LoadoutWeaponEffectHandler) {
        handlers[handler.loadoutId] = handler
    }

    fun get(loadoutId: String): LoadoutWeaponEffectHandler? = handlers[loadoutId]
}
