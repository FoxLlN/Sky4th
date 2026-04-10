package sky4th.dungeon.util

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.Dungeon
import sky4th.dungeon.command.DungeonContext

/**
 * 头颅工具类
 * 用于创建和配置自定义头颅
 */
object CreditsHeadUtil {

    /**
     * 创建自定义头颅（通用方法）
     *
     * @param name 头颅显示名称
     * @param texture 头颅皮肤纹理（Base64编码）
     * @return 自定义头颅物品
     */
    fun createCustomHead(name: String, texture: String): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as? org.bukkit.inventory.meta.SkullMeta ?: return head

        // 设置皮肤纹理
        if (texture.isNotEmpty()) {
            try {
                // 创建 GameProfile
                val gameProfileClass = Class.forName("com.mojang.authlib.GameProfile")
                val gameProfile = gameProfileClass.getConstructor(
                    java.util.UUID::class.java,
                    String::class.java
                ).newInstance(java.util.UUID.randomUUID(), "Custom_Head")

                val propertyClass = Class.forName("com.mojang.authlib.properties.Property")
                val property = propertyClass.getConstructor(
                    String::class.java,
                    String::class.java
                ).newInstance("textures", texture)

                val properties = gameProfileClass.getMethod("getProperties").invoke(gameProfile)

                // 使用 putAll 方法添加属性
                val propertyMapClass = Class.forName("com.mojang.authlib.properties.PropertyMap")
                val putAllMethod = propertyMapClass.getMethod("putAll",
                    Class.forName("com.google.common.collect.Multimap"))
                // 创建一个临时的 Multimap
                val multimap = Class.forName("com.google.common.collect.ArrayListMultimap")
                    .getMethod("create").invoke(null)
                val multimapPut = multimap.javaClass.getMethod("put", Any::class.java, Any::class.java)
                multimapPut.invoke(multimap, "textures", property)
                putAllMethod.invoke(properties, multimap)

                // 使用 ResolvableProfile 设置头颅（Paper 1.21+）
                val resolvableProfileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile")
                val resolvableProfile = resolvableProfileClass.getConstructor(gameProfileClass)
                    .newInstance(gameProfile)

                val profileField = meta.javaClass.getDeclaredField("profile")
                profileField.isAccessible = true
                profileField.set(meta, resolvableProfile)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 设置显示名称
        meta.displayName(LanguageAPI.getComponent(Dungeon.instance, name))
        head.itemMeta = meta
        return head
    }
}
