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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import bid.yuanlu.ifr_controller.databinding.FragmentControllerBinding
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ControllerFragment : Fragment() {

    private var _binding: FragmentControllerBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var storge: SharedPreferences

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

        if (binding.leftContainer != null) addJoystick(0, binding.leftContainer!!, binding.joystickPan1!!, binding.joystickCore1!!)
        if (binding.rightContainer != null) addJoystick(1, binding.rightContainer!!, binding.joystickPan2!!, binding.joystickCore2!!)

        if (binding.seek1 != null) addSeek(4, binding.seek1!!)
        if (binding.seek2 != null) addSeek(5, binding.seek2!!)

        if (binding.settingBtn != null) binding.settingBtn!!.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                findNavController().navigate(R.id.action_ControllerFragment_to_SettingFragment)
            }
            true
        }
        (activity as MainActivity).webManager!!.statusCallback = { connected: Boolean, error: Throwable? ->
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
        (activity as MainActivity).webManager!!.statusCallback = null
    }

    private fun controlCallback(type: Int, x: Int) {
        (activity as MainActivity).webManager!!.dataPack.setSW(type, x)
    }

    private fun controlCallback(type: Int, x: Float, y: Float) {
        val wm = (activity as MainActivity).webManager!!
        wm.dataPack.setCH(type * 2, x)
        wm.dataPack.setCH(type * 2 + 1, y)
    }

    private fun vibrate(milliseconds: Long) {
        if (!storge.getBoolean("vibrate", false)) return
        (requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(milliseconds)
    }

    /**
     * 添加摇杆事件
     * @param container 包含摇杆的容器
     * @param joystick 摇杆盘
     * @param core 摇杆核
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun addJoystick(type: Int, container: View, joystick: View, core: View) {
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
            controlCallback(type, dx / r, dy / r)
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
                    vibrate(5)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isRelease) return@setOnTouchListener true
                    setCore(r, x, y)
                    vibrate(5)
                }

                MotionEvent.ACTION_UP -> {
                    if (isRelease) return@setOnTouchListener true
                    joystick.x = panOriginalX
                    joystick.y = panOriginalY
                    core.x = coreOriginalX
                    core.y = coreOriginalY
                    controlCallback(type, 0f, 0f)
                    isRelease = true
                }
            }
            true
        }

        controlCallback(type, 0f, 0f)
    }

    /**
     * 添加拨杆
     */
    private fun addSeek(type: Int, seek: SeekBar) {
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                controlCallback(type, progress + 1)
                vibrate(10)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                vibrate(10)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                vibrate(10)
            }
        })
        seek.setProgress(0, false)
    }
}