package bid.yuanlu.ifr_controller

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import bid.yuanlu.ifr_controller.databinding.FragmentControllerBinding
import java.util.Timer
import java.util.TimerTask
import kotlin.math.pow
import kotlin.math.sqrt


class ControllerFragment : Fragment() {
    private var _binding: FragmentControllerBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var storge: SharedPreferences
    private lateinit var controllerStatus: SharedPreferences
    private var handler: Handler = Handler(Looper.getMainLooper())
    private val timer = Timer()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControllerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storge = (activity as MainActivity).getSharedPreferences("settings", Context.MODE_PRIVATE)
        controllerStatus = (activity as MainActivity).getSharedPreferences("controller_status", Context.MODE_PRIVATE)

        binding.apply {

            btn1.setTag(R.id.btn_extra_data, BtnExtraData(enableColor = R.color.red))
            btn2.setTag(R.id.btn_extra_data, BtnExtraData())
            btn3.setTag(R.id.btn_extra_data, BtnExtraData())
            btn4.setTag(R.id.btn_extra_data, BtnExtraData(update = { isPress, _ ->
                if (isPress) {
                    btn6.visibility = View.VISIBLE
                    btn7.visibility = View.VISIBLE
                    btn8.visibility = View.INVISIBLE
                    (btn6.getTag(R.id.btn_setter) as? SetterBtn)?.set(false)
                    (btn7.getTag(R.id.btn_setter) as? SetterBtn)?.set(false)
                }
            }))
            btn5.setTag(R.id.btn_extra_data, BtnExtraData(update = { isPress, _ ->
                if (isPress) {
                    btn6.visibility = View.INVISIBLE
                    btn7.visibility = View.INVISIBLE
                    btn8.visibility = View.VISIBLE
                    (btn8.getTag(R.id.btn_setter) as? SetterBtn)?.set(false)
                }
            }))
            btn6.setTag(R.id.btn_extra_data, BtnExtraData(bounce = -1))
            btn7.setTag(R.id.btn_extra_data, BtnExtraData(bounce = -1, update = { isPress, _ ->
                btn7.text = getString(if (isPress) R.string.ctrl_btn_zhuaqu_jiang else R.string.ctrl_btn_zhuaqu_sheng)
            }))
            btn8.setTag(R.id.btn_extra_data, BtnExtraData(bounce = 500, disableBg = R.drawable.red_btn))

            addJoystick(5, rightContainer, joystickPan2, joystickCore2)
            addJoystick(8, leftContainer, joystickPan1, joystickCore1)

            //addSeek(4, seek1)
            //addSeek(5, seek2)

            addBtnGroup(0, btn1, btn2, btn3)
            addBtnGroup(1, btn4, btn5)
            addBtn(2, btn6)
            addBtn(3, btn7)
            addBtn(3, btn8)
            //addBtn(0, btn1)
            //addBtn(1, btn2)
            //addBtn(2, btn3)
            //addBtn(3, btn4)
            //addBtn(4, btn5)

            addMap(4, map, realMap)

            settingBtn.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    findNavController().navigate(R.id.action_ControllerFragment_to_SettingFragment)
                }
                true
            }
        }

        timer.schedule(object : TimerTask() {
            override fun run() {
                _binding?.apply {
                    val x = ((btn6.x + btn6.width / 2) + (btn7.x + btn7.width / 2)) / 2
                    val y = ((btn6.y + btn6.height / 2) + (btn7.y + btn7.height / 2)) / 2
                    btn8.x = x - btn8.width / 2
                    btn8.y = y - btn8.height / 2
                }
            }
        }, 10, 100)

        (activity as MainActivity).webManager!!.setStatusCallback { connected, error ->
            binding.settingBtn.apply {
                if (error != null) {
                    setImageResource(R.drawable.gray_light)
                } else if (connected) {
                    setImageResource(R.drawable.green_light)
                } else {
                    setImageResource(R.drawable.red_light)
                }
            }
        }
        (activity as MainActivity).webManager!!.doStatusCallback()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (activity as MainActivity).webManager!!.setStatusCallback(null)
    }


    private fun vibrate(milliseconds: Long, vt: VibrateType) {
        if (!storge.getBoolean(vt.field, false)) return
        (requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(milliseconds)
    }

    enum class VibrateType(val field: String) {
        BTN("vibrateBtn"),
        JOYSTICK("vibrateJoystick"),
        SEEK("vibrateSeek")
    }

    /**
     * 添加摇杆事件
     * @param container 包含摇杆的容器
     * @param joystick 摇杆盘
     * @param core 摇杆核
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun addJoystick(type: Int, container: View?, joystick: View?, core: View?) {
        if (container == null || joystick == null || core == null) return
        var panOriginalX = 0f//大盘原始位置
        var panOriginalY = 0f//大盘原始位置
        var coreOriginalX = 0f//摇杆原始位置
        var coreOriginalY = 0f//摇杆原始位置
        var isRelease = true//是否释放

        var panDownX = 0f//大盘按下时中心位置
        var panDownY = 0f//大盘按下时中心位置

        fun setCore(r: Float, x: Float, y: Float) {
            var dx = x - panDownX
            var dy = y - panDownY
            val disS = dx.pow(2) + dy.pow(2)
            if (disS > r.pow(2)) {
                val s = r / sqrt(disS)
                dx *= s
                dy *= s
            }
            core.x = panDownX + dx - core.width / 2
            core.y = panDownY + dy - core.width / 2
            (activity as MainActivity).webManager!!.dataPack.setCH(type, dx / r, dy / r)
        }
        container.setOnTouchListener { _, event ->
            val r = joystick.width / 2.0f
            // 获取触摸事件的坐标
            val x = event.x + container.x
            val y = event.y + container.y
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isRelease) return@setOnTouchListener true
                    panOriginalX = joystick.x
                    panOriginalY = joystick.y
                    coreOriginalX = core.x
                    coreOriginalY = core.y

                    panDownX = if (x - (joystick.width / 2.0f + core.width / 2.0f) < container.x) container.x + core.width / 2.0f
                    else if (x + joystick.width / 2.0f + core.width / 2.0f > container.x + container.width) container.x + container.width - joystick.width - core.width / 2.0f
                    else x - joystick.width / 2

                    panDownY = if (y - (joystick.height / 2.0f + core.height / 2.0f) / 2 < container.y) container.y + core.height / 2.0f
                    else if (y + joystick.height / 2.0f + core.height / 2.0f > container.y + container.height) container.y + container.height - joystick.height - core.height / 2.0f
                    else y - joystick.height / 2

                    joystick.x = panDownX
                    joystick.y = panDownY

                    panDownX += joystick.width / 2
                    panDownY += joystick.height / 2

                    setCore(r, x, y)

                    isRelease = false
                    vibrate(5, VibrateType.JOYSTICK)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isRelease) return@setOnTouchListener true
                    setCore(r, x, y)
                    vibrate(5, VibrateType.JOYSTICK)
                }

                MotionEvent.ACTION_UP -> {
                    if (isRelease) return@setOnTouchListener true
                    joystick.x = panOriginalX
                    joystick.y = panOriginalY
                    core.x = coreOriginalX
                    core.y = coreOriginalY
                    (activity as MainActivity).webManager!!.dataPack.setCH(type, 0f, 0f)
                    isRelease = true
                }
            }
            true
        }

        (activity as MainActivity).webManager!!.dataPack.setCH(type, 0f, 0f)
    }

    /**
     * 添加拨杆
     */
    private fun addSeek(type: Int, seek: SeekBar?) {
        if (seek == null) return
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                (activity as MainActivity).webManager!!.dataPack.setSW(type, progress + 1)
                controllerStatus.edit { putInt("seek_$type", progress) }
                vibrate(10, VibrateType.SEEK)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                vibrate(10, VibrateType.SEEK)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                vibrate(10, VibrateType.SEEK)
            }
        })
        val progress = controllerStatus.getInt("seek_$type", 0)
        controllerStatus.edit { putInt("seek_$type", progress) }
        seek.setProgress(progress, false)
        (activity as MainActivity).webManager!!.dataPack.setSW(type, progress + 1)
    }

    /**
     * 添加按钮
     */
    private fun addBtn(type: Int, btn: com.google.android.material.button.MaterialButton?) {
        if (btn == null) return
        val bed = btn.getTag(R.id.btn_extra_data) as? BtnExtraData ?: BtnExtraData()
        btn.setTag(R.id.btn_extra_data, bed)
        var isPress = controllerStatus.getBoolean("btn_$type", false)
        fun handlerClick(newState: Boolean, vib: Boolean = false) {
            (activity as MainActivity).webManager!!.dataPack.setBTN(type, newState)
            controllerStatus.edit { putBoolean("btn_$type", bed.bounce < 0 && newState) }
            btn.apply {
                setBackgroundResource(if (newState) bed.enableBg else bed.disableBg)
                setTextColor(resources.getColor(if (newState) bed.enableColor else bed.disableColor, activity?.theme))
            }
            bed.update(newState, isPress)
            isPress = newState
            if (vib) vibrate(10, VibrateType.BTN)
            if (bed.bounce > 0 && newState) handler.postDelayed({ handlerClick(false) }, bed.bounce)
        }
        btn.setOnClickListener { handlerClick(!isPress, true) }
        btn.setTag(R.id.btn_setter, object : SetterBtn {
            override fun set(isPressed: Boolean) {
                handlerClick(isPressed, false)
            }
        })
        handlerClick(isPress)
    }

    /**
     * 添加按钮组, 按钮组中只有一个能够启用
     */
    private fun addBtnGroup(type: Int, vararg btns: com.google.android.material.button.MaterialButton?) {
        for (btn in btns) if (btn == null) return
        var btnIndex = -1
        val beds = Array(btns.size) { btns[it]!!.getTag(R.id.btn_extra_data) as? BtnExtraData ?: BtnExtraData() }
        for (i in btns.indices) btns[i]!!.setTag(R.id.btn_extra_data, beds[i])

        fun handlerClick(index: Int, newState: Boolean, oldState: Boolean, vib: Boolean = false) {
            val bed = beds[index]
            if (newState && !oldState) {
                (activity as MainActivity).webManager!!.dataPack.setRAW(type, index)
                controllerStatus.edit { putInt("btnG_$type", index) }
                for (i in btns.indices) if (i != index) handlerClick(i, false, i == btnIndex, false)
                btnIndex = index
                if (bed.bounce > 0) {
                    val to = bed.bounceTo
                    handler.postDelayed({ handlerClick(to, true, btnIndex == to) }, bed.bounce)
                }
            }
            bed.update(newState, oldState)
            val btn = btns[index]!!
            btn.setBackgroundResource(if (newState) bed.enableBg else bed.disableBg)
            btn.setTextColor(resources.getColor(if (newState) bed.enableColor else bed.disableColor, activity?.theme))
            if (vib) vibrate(10, VibrateType.BTN)
        }

        for (index in btns.indices) {
            val btn = btns[index]!!
            btn.setOnClickListener { handlerClick(index, true, btnIndex == index, true) }
            btn.setTag(R.id.btn_setter, object : SetterBGroup {
                override fun set() {
                    handlerClick(index, true, btnIndex == index, false)
                }
            })
        }
        handlerClick(controllerStatus.getInt("btnG_$type", 0), newState = true, oldState = false)
    }

    /**
     * 添加地图
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun addMap(type: Int, map: ImageView?, map_real: ImageView?) {
        if (map == null || map_real == null) return
        val isRed = storge.getBoolean("is_red_team", true)
        val mc = MapClicker.getRobocon2023()
        var select: Int
        map_real.setImageResource(if (isRed) R.drawable.robocon_2023_place_red else R.drawable.robocon_2023_place_blue)
        map.setOnTouchListener { _, event ->
            if (event.actionMasked != MotionEvent.ACTION_UP) return@setOnTouchListener true
            val x = event.x.toDouble() / map.width
            val y = event.y.toDouble() / map.height
            select = mc.getClosest(x, y)
            (activity as MainActivity).webManager!!.dataPack.setRAW(type, select)

            true
        }
    }


}
