package sky4th.core.model

/**
 * 玩家经济信息
 */
data class PlayerEconomy(
    var credits: Double = 100.0,      // 当前信用点余额
    
    // 每日限制与统计
    var dailyEarned: Double = 0.0,    // 今日已获得信用点
    var dailySpent: Double = 0.0,     // 今日已消费信用点
    var dailyLimit: Double = 100000000.0,  // 每日获取上限
    
    // 历史统计
    var totalEarned: Double = 0.0,    // 历史总获得
    var totalSpent: Double = 0.0      // 历史总消费
) {
    /**
     * 检查是否可以获得更多信用点（未超过每日上限）
     */
    fun canEarnMore(amount: Double): Boolean {
        return dailyEarned + amount <= dailyLimit
    }
    
    /**
     * 添加信用点（会检查每日上限）
     * @param amount 要添加的金额
     * @return 实际添加的金额（可能因为上限而减少）
     */
    fun addCredits(amount: Double): Double {
        val remaining = dailyLimit - dailyEarned
        val actualAmount = amount.coerceAtMost(remaining)
        
        credits += actualAmount
        dailyEarned += actualAmount
        totalEarned += actualAmount
        
        return actualAmount
    }
    
    /**
     * 扣除信用点
     * @param amount 要扣除的金额
     * @return 是否成功扣除（余额不足返回 false）
     */
    fun removeCredits(amount: Double): Boolean {
        if (credits < amount) {
            return false
        }
        
        credits -= amount
        dailySpent += amount
        totalSpent += amount
        
        return true
    }
    
    /**
     * 检查是否有足够的信用点
     */
    fun hasEnough(amount: Double): Boolean {
        return credits >= amount
    }
    
    /**
     * 重置每日数据（应在每日重置时调用）
     */
    fun resetDaily() {
        dailyEarned = 0.0
        dailySpent = 0.0
    }
}
