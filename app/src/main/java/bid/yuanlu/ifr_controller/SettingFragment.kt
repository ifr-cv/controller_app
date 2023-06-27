package bid.yuanlu.ifr_controller

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import bid.yuanlu.ifr_controller.databinding.FragmentSettingBinding


class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var storge: SharedPreferences


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_SettingFragment_to_ControllerFragment)
        }
        binding.btnSave.setOnClickListener {
            val url = binding.editUrl.text.toString()
            (activity as MainActivity).webManager!!.setUrl(url)

            storge.edit { putString("url", url) }

            findNavController().navigate(R.id.action_SettingFragment_to_ControllerFragment)
        }
        binding.editUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                var hasEdit = false
                hasEdit = hasEdit || binding.editUrl.text.toString() != storge.getString("url", "")
                binding.btnCancel.text = getString(if (hasEdit) R.string.cancel else R.string.back)
            }
        })
        binding.vibrateBtn.setOnCheckedChangeListener { _, isChecked ->
            storge.edit { putBoolean("vibrateBtn", isChecked) }
        }
        binding.vibrateJoystick.setOnCheckedChangeListener { _, isChecked ->
            storge.edit { putBoolean("vibrateJoystick", isChecked) }
        }
        binding.vibrateSeek.setOnCheckedChangeListener { _, isChecked ->
            storge.edit { putBoolean("vibrateSeek", isChecked) }
        }
        val act = activity as MainActivity
        storge = act.getSharedPreferences("settings", Context.MODE_PRIVATE)
        binding.editUrl.setText(storge.getString("url", ""))
        binding.vibrateBtn.isChecked = storge.getBoolean("vibrateBtn", true)
        binding.vibrateJoystick.isChecked = storge.getBoolean("vibrateJoystick", true)
        binding.vibrateSeek.isChecked = storge.getBoolean("vibrateSeek", true)
        binding.btnSave.text = getString(if (act.webManager!!.isConnected) R.string.save_and_reconnect else R.string.save_and_connect)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}