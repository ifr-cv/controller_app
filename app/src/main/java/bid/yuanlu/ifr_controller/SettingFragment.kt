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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import bid.yuanlu.ifr_controller.databinding.FragmentSettingBinding
import java.net.InetAddress


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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val act = activity as MainActivity
        storge = act.getSharedPreferences("settings", Context.MODE_PRIVATE)

        binding.apply {
            btnCancel.setOnClickListener { findNavController().navigate(R.id.action_SettingFragment_to_ControllerFragment) }
            btnSave.apply {
                setOnClickListener {
                    val url = editUrl.text.toString()
                    (activity as MainActivity).webManager!!.setUrl(url)

                    storge.edit { putString("url", url) }

                    findNavController().navigate(R.id.action_SettingFragment_to_ControllerFragment)
                }
                text = getString(if (act.webManager!!.isConnected) R.string.save_and_reconnect else R.string.save_and_connect)
            }


            editUrl.apply {
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        var hasEdit = false
                        hasEdit = hasEdit || text.toString() != storge.getString("url", "")
                        btnCancel.text = getString(if (hasEdit) R.string.cancel else R.string.back)
                    }
                })
                setText(storge.getString("url", ""))
            }


            mapOf("vibrateBtn" to vibrateBtn, "vibrateJoystick" to vibrateJoystick, "vibrateSeek" to vibrateSeek)
                .forEach { (key, btn) ->
                    btn.setOnCheckedChangeListener { _, isChecked -> storge.edit { putBoolean(key, isChecked) } }
                    btn.isChecked = storge.getBoolean(key, true)
                }


            switchTeam.apply {
                fun isRedT() = storge.getBoolean("is_red_team", true)
                fun update(isRed: Boolean) {
                    setBackgroundColor(resources.getColor(if (isRed) R.color.team_red else R.color.team_blue, activity?.theme))
                    text = getString(if (isRed) R.string.self_team_red else R.string.self_team_blue)
                }
                setOnClickListener {
                    val isRed = !isRedT()
                    storge.edit { putBoolean("is_red_team", isRed) }
                    update(isRed)
                }
                update(isRedT())
            }

            mapOf("host1" to btnHost1, "host2" to btnHost2, "host3" to btnHost3).forEach { (key, btn) ->
                btn.setOnLongClickListener {
                    findNavController().navigate(R.id.action_SettingFragment_to_HostFragment, bundleOf("toHost" to key))
                    true
                }
                // https://stackoverflow.com/questions/106179/regular-expression-to-match-dns-hostname-or-ip-address
                val rIp = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$".toRegex()
                val rHost = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$".toRegex()
                btn.setOnClickListener {
                    storge.getString(key, null)?.let {
                        if (!it.contains(":")) return@let
                        val (hostname, port) = it.split(":", limit = 2)
                        val address = if (rIp.matches(hostname)) hostname
                        else if (rHost.matches(hostname)) InetAddress.getByName(hostname).hostAddress
                        else null
                        if (address != null && port.toIntOrNull() in 0..65535) editUrl.setText("http://$address:$port")
                    }
                }
                storge.getString(key, null)?.let { btn.text = it }
            }

            txtConnectionStatus.text = getString(if (act.webManager!!.isConnected) R.string.tip_connect else R.string.tip_unconnect)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}