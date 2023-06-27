package bid.yuanlu.ifr_controller

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import bid.yuanlu.ifr_controller.databinding.FragmentControllerBinding
import kotlin.math.pow
import kotlin.math.sqrt


class ControllerFragment : Fragment() {

    private var _binding: FragmentControllerBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var storge: SharedPreferences
    private lateinit var controllerStatus: SharedPreferences

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

        addJoystick(0, binding.leftContainer, binding.joystickPan1, binding.joystickCore1)
        addJoystick(2, binding.rightContainer, binding.joystickPan2, binding.joystickCore2)

//        addSeek(4, binding.seek1)
//        addSeek(5, binding.seek2)

        addBtn(4, binding.btn1)
        addBtn(5, binding.btn2)
        addBtn(6, binding.btn3)
        addBtn(7, binding.btn4)
        addBtn(8, binding.btn5)

        if (binding.settingBtn != null) binding.settingBtn!!.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                findNavController().navigate(R.id.action_ControllerFragment_to_SettingFragment)
            }
            true
        }

        if (binding.map != null) binding.map!!.rotation = if (storge.getBoolean("is_red_team", true)) -90f else 90f

        (activity as MainActivity).webManager!!.setStatusCallback { connected, error ->
            if (binding.settingBtn != null) {
                if (error != null) {
                    binding.settingBtn!!.setImageResource(R.drawable.gray_light)
                } else if (connected) {
                    binding.settingBtn!!.setImageResource(R.drawable.green_light)
                } else {
                    binding.settingBtn!!.setImageResource(R.drawable.red_light)
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
        if (joystick.visibility != View.VISIBLE) return
        var panOriginalX = 0f//大盘原始位置
        var panOriginalY = 0f//大盘原始位置
        var coreOriginalX = 0f//摇杆原始位置
        var coreOriginalY = 0f//摇杆原始位置
        var isRelease = true//是否释放

        var panDownX = 0f//大盘按下时中心位置
        var panDownY = 0f//大盘按下时中心位置

        val setCore = fun(r: Float, x: Float, y: Float) {
            var dx = x - panDownX
            var dy = y - panDownY
            if (dx.pow(2) + dy.pow(2) > r.pow(2)) {
                val s = r / sqrt(dx.pow(2) + dy.pow(2))
                dx *= s
                dy *= s
            }
            core.x = panDownX + dx - core.width / 2
            core.y = panDownY + dy - core.width / 2
            (activity as MainActivity).webManager!!.dataPack.setCH(type, dx / r, dy / r)
        }
        container.setOnTouchListener { _, event ->
            val r = joystick.width - core.width / 2f
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

                    panDownX = if (x - joystick.width / 2 < container.x) container.x
                    else if (x + joystick.width / 2 > container.x + container.width) container.x + container.width - joystick.width
                    else x - joystick.width / 2

                    panDownY = if (y - joystick.height / 2 < container.y) container.y
                    else if (y + joystick.height / 2 > container.y + container.height) container.y + container.height - joystick.height
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
        if (seek.visibility != View.VISIBLE) return
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
        if (btn.visibility != View.VISIBLE) return
        var isPress = controllerStatus.getBoolean("btn_$type", false)
        btn.setOnClickListener {
            isPress = !isPress
            (activity as MainActivity).webManager!!.dataPack.setBTN(type, isPress)
            controllerStatus.edit { putBoolean("btn_$type", isPress) }
            btn.setBackgroundResource(if (isPress) R.drawable.green_btn else R.drawable.gray_btn)
            vibrate(10, VibrateType.BTN)
        }
        (activity as MainActivity).webManager!!.dataPack.setBTN(type, isPress)
        controllerStatus.edit { putBoolean("btn_$type", isPress) }
        btn.setBackgroundResource(if (isPress) R.drawable.green_btn else R.drawable.gray_btn)
    }
}