package bid.yuanlu.ifr_controller


class BtnExtraData(
    /**
     * 自动弹起时长(ms), 小于等于0则不自动弹起
     */
    var bounce: Long = -1,
    /**
     * 按钮组自动弹起后切换目标
     */
    var bounceTo: Int = 0,
    /**
     * 启动的颜色
     */
    var enableColor: Int = R.color.black,
    /**
     * 关闭的颜色
     */
    var disableColor: Int = R.color.white,

    /**
     * 启动的背景
     */
    var enableBg: Int = R.drawable.green_btn,
    /**
     * 关闭的背景
     */
    var disableBg: Int = R.drawable.gray_btn,

    /**
     * 更新函数
     */
    var update: (isPressed: Boolean, lastIsPressed: Boolean) -> Unit = { _, _ -> }
)

interface SetterBtn {
    fun set(isPressed: Boolean)
}

interface SetterBGroup {
    fun set()
}
