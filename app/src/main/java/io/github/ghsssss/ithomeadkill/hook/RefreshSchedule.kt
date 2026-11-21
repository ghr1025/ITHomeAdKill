package io.github.ghsssss.ithomeadkill.hook

/**
 * 仅维护刷新时间，由调用方串行调用，不读取时钟或执行刷新。
 *
 * 所有 nowMillis 均须由调用方注入非负 uptimeMillis；负值会被拒绝且不改变状态。
 * column 只能是稳定且不持有 Activity/View 的 token（例如资源 ID），按 equals 比较；
 * null 表示无可见栏目。截止时间溢出时饱和为 Long.MAX_VALUE，该端点视为到期。
 */
class RefreshSchedule {
    private var deadline: Long? = null
    private var column: Any? = null
    private var intervalMillis = 0L

    /** 0/非法分钟、非前台或无栏目立即停止；相同有效状态保留已有截止时间。 */
    fun update(minutes: Int, active: Boolean, column: Any?, nowMillis: Long): Long? {
        require(nowMillis >= 0L) { "nowMillis must be non-negative uptimeMillis" }
        val interval = when (minutes) {
            1, 5, 10 -> minutes * 60_000L
            else -> 0L
        }
        if (!active || column == null || interval == 0L) {
            stop()
            return null
        }
        if (deadline == null || this.column != column || intervalMillis != interval) {
            deadline = deadlineAfter(nowMillis, interval)
        }
        this.column = column
        intervalMillis = interval
        return deadline
    }

    /** 只查询，不消耗到期事件。 */
    fun due(nowMillis: Long): Boolean {
        require(nowMillis >= 0L) { "nowMillis must be non-negative uptimeMillis" }
        return deadline?.let { nowMillis >= it } ?: false
    }

    /** 仍启用时从此刻重计完整间隔，不补跑错过的 tick；已停止则不重新启用。 */
    fun consumed(nowMillis: Long): Long? {
        require(nowMillis >= 0L) { "nowMillis must be non-negative uptimeMillis" }
        if (deadline != null) {
            deadline = deadlineAfter(nowMillis, intervalMillis)
        }
        return deadline
    }

    fun stop() {
        deadline = null
        column = null
        intervalMillis = 0L
    }

    private fun deadlineAfter(nowMillis: Long, interval: Long): Long =
        if (nowMillis > Long.MAX_VALUE - interval) Long.MAX_VALUE else nowMillis + interval
}
