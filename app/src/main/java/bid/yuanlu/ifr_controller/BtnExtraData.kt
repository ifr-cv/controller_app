package bid.yuanlu.ifr_controller

class BtnExtraData(
    bounce: Long = -1,
    bounceTo: Int = 0,
    enable_color: Int = R.color.black,
    disable_color: Int = R.color.white,
    enable_bg: Int = R.drawable.green_btn,
    disable_bg: Int = R.drawable.gray_btn,
    update: (isPressed: Boolean, lastIsPressed: Boolean) -> Unit = { _, _ -> }
) {
    /**
     * 自动弹起时长(ms), 小于等于0则不自动弹起
     */
    var bounce: Long

    /**
     * 按钮组自动弹起后切换目标
     */
    var bounceTo: Int

    /**
     * 启动的颜色
     */
    var enableColor: Int

    /**
     * 关闭的颜色
     */
    var disableColor: Int

    /**
     * 启动的背景
     */
    var enableBg: Int

    /**
     * 关闭的背景
     */
    var disableBg: Int

    /**
     * 更新函数
     */
    var update: (isPressed: Boolean, lastIsPressed: Boolean) -> Unit

    init {
        this.bounce = bounce
        this.bounceTo = bounceTo
        this.enableColor = enable_color
        this.disableColor = disable_color
        this.update = update
        this.enableBg = enable_bg
        this.disableBg = disable_bg
    }
}

interface SetterBtn {
    fun set(isPressed: Boolean)
}

interface SetterBGroup {
    fun set()
}
