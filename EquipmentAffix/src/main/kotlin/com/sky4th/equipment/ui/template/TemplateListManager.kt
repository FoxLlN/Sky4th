
package com.sky4th.equipment.ui.template

import com.sky4th.equipment.loader.AffixTemplateLoader
import org.bukkit.entity.Player
import java.util.UUID

/**
 * 模板列表管理器
 * 负责管理所有词条模板的列表显示、筛选和排序
 */
object TemplateListManager {
    // 缓存所有模板列表（列表ID -> 模板ID列表）
    private val templateCache = mutableMapOf<String, List<String>>()

    // 玩家状态管理（玩家ID -> 列表ID -> 状态）
    private val playerStates = mutableMapOf<String, MutableMap<String, TemplateListState>>()

    // 列表配置（列表ID -> 配置）
    private val listConfigs = mutableMapOf<String, TemplateListConfig>()

    /**
     * 注册模板列表配置
     */
    fun registerListConfig(config: TemplateListConfig) {
        listConfigs[config.id] = config
    }

    /**
     * 获取玩家的模板列表状态
     */
    fun getPlayerState(player: Player, listId: String): TemplateListState {
        return playerStates.getOrPut(player.uniqueId.toString()) { mutableMapOf() }
            .getOrPut(listId) { TemplateListState() }
    }

    /**
     * 获取筛选后的模板列表
     */
    fun getFilteredTemplates(player: Player, listId: String): List<String> {
        val config = listConfigs[listId] ?: return emptyList()
        val state = getPlayerState(player, listId)

        var templates = templateCache[listId] ?: AffixTemplateLoader.getAllTemplateIds()

        // 应用筛选器
        config.filters.forEach { filter ->
            templates = filter.apply(templates, state, player)
        }

        // 应用排序器
        config.sorter?.let { sorter ->
            templates = sorter.sort(templates, state)
        }

        return templates
    }

    /**
     * 获取当前页的模板
     * @param player 玩家
     * @param listId 列表ID
     * @param pageSize 每页显示数量（由UI shape决定）
     * @return 当前页的模板ID列表
     */
    fun getCurrentPageTemplates(player: Player, listId: String, pageSize: Int): List<String> {
        val state = getPlayerState(player, listId)
        val filtered = getFilteredTemplates(player, listId)

        val startIndex = state.currentPage * pageSize
        val endIndex = minOf(startIndex + pageSize, filtered.size)

        return if (startIndex < filtered.size) {
            filtered.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    /**
     * 下一页
     */
    fun nextPage(player: Player, listId: String, pageSize: Int): Boolean {
        val state = getPlayerState(player, listId)
        val filtered = getFilteredTemplates(player, listId)
        val maxPage = (filtered.size - 1) / pageSize

        if (state.currentPage < maxPage) {
            state.currentPage++
            return true
        }
        return false
    }

    /**
     * 上一页
     */
    fun previousPage(player: Player, listId: String): Boolean {
        val state = getPlayerState(player, listId)
        if (state.currentPage > 0) {
            state.currentPage--
            return true
        }
        return false
    }

    /**
     * 跳转到指定页
     */
    fun jumpToPage(player: Player, listId: String, page: Int, pageSize: Int): Boolean {
        val state = getPlayerState(player, listId)
        val filtered = getFilteredTemplates(player, listId)
        val maxPage = (filtered.size - 1) / pageSize

        if (page in 0..maxPage) {
            state.currentPage = page
            return true
        }
        return false
    }

    /**
     * 获取总页数
     */
    fun getTotalPages(player: Player, listId: String, pageSize: Int): Int {
        val filtered = getFilteredTemplates(player, listId)
        return if (filtered.isEmpty()) 0 else (filtered.size - 1) / pageSize + 1
    }

    /**
     * 清除玩家状态
     */
    fun clearPlayerState(player: Player) {
        playerStates.remove(player.uniqueId.toString())
    }

    /**
     * 清除指定玩家的指定列表状态
     */
    fun clearPlayerListState(player: Player, listId: String) {
        playerStates[player.uniqueId.toString()]?.remove(listId)
    }

    /**
     * 清除所有玩家状态
     */
    fun clearAllStates() {
        playerStates.clear()
    }

    /**
     * 清除指定玩家的所有状态
     */
    fun clearPlayerStates(playerId: UUID) {
        playerStates.remove(playerId.toString())
    }
}
