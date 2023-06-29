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
import bid.yuanlu.ifr_controller.databinding.FragmentHostBinding


class HostFragment : Fragment() {

    private var _binding: FragmentHostBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var storge: SharedPreferences


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val act = activity as MainActivity
        storge = act.getSharedPreferences("settings", Context.MODE_PRIVATE)

        fun setHost(host: String) = arguments?.getString("toHost")?.apply { storge.edit { putString(this@apply, host) } }
        fun getHost() = arguments?.getString("toHost")?.let { storge.getString(it, "") } ?: ""
        
        binding.apply {
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
                setText(getHost())
            }
            btnCancel.setOnClickListener { findNavController().navigate(R.id.action_HostFragment_to_SettingFragment) }
            btnSave.setOnClickListener {
                setHost(editUrl.text.toString())
                findNavController().navigate(R.id.action_HostFragment_to_SettingFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}