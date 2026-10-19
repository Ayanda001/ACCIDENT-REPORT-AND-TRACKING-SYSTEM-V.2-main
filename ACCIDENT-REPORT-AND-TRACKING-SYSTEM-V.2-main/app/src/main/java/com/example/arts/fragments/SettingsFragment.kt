package com.example.arts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.arts.R
import com.example.arts.utils.ImageStorageHelper
import com.example.arts.utils.PrefsHelper
import com.example.arts.utils.toast
import com.example.arts.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by activityViewModels()

    private val pickAvatarLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val context = context ?: return@registerForActivityResult
        val dataUrl = ImageStorageHelper.toDataUrl(context, uri, maxDimension = 300, quality = 80)
        if (dataUrl == null) {
            toast("Couldn't read that image, please try another")
            return@registerForActivityResult
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                profileViewModel.saveAvatar(dataUrl)
                profileViewModel.user.value?.let { showAvatar(dataUrl, it.fullName) }
                toast("Profile photo updated")
            } catch (e: Exception) {
                toast("Couldn't save your photo: ${e.message}")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<View>(R.id.btn_back)
        val etName = view.findViewById<EditText>(R.id.et_full_name)
        val etPhone = view.findViewById<EditText>(R.id.et_phone)
        val etEmail = view.findViewById<EditText>(R.id.et_email_locked)
        val etAiKey = view.findViewById<EditText>(R.id.et_ai_key)
        val btnSave = view.findViewById<View>(R.id.btn_save_profile)
        val btnChangePhoto = view.findViewById<View>(R.id.btn_change_photo)

        btnBack?.setOnClickListener { findNavController().navigateUp() }

        // Load existing AI key from local storage
        context?.let { ctx ->
            etAiKey?.setText(PrefsHelper.getCustomAiKey(ctx))
        }

        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            etName?.setText(user.fullName)
            etPhone?.setText(user.phone)
            etEmail?.setText(user.email)
            showAvatar(user.avatarDataUrl, user.fullName)
        }

        btnChangePhoto?.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        btnSave?.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val name = etName?.text?.toString()?.trim().orEmpty()
            val phone = etPhone?.text?.toString()?.trim().orEmpty()
            val aiKey = etAiKey?.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                toast("Enter your full name")
                return@setOnClickListener
            }
            
            // Save AI Key locally
            PrefsHelper.setCustomAiKey(ctx, aiKey)
            
            // Update profile on Firebase
            profileViewModel.updateUser(name, phone)
            toast("Settings saved")
        }

        setupTheme(view)
        setupLanguage(view)
    }

    private fun showAvatar(dataUrl: String?, fullName: String = "") {
        val root = view ?: return
        val iv = root.findViewById<ImageView>(R.id.iv_avatar) ?: return
        val tvInitials = root.findViewById<TextView>(R.id.tv_avatar_initials) ?: return
        
        val bitmap = ImageStorageHelper.decodeDataUrl(dataUrl)
        if (bitmap != null) {
            iv.setImageBitmap(bitmap)
            iv.visibility = View.VISIBLE
            tvInitials.visibility = View.GONE
        } else {
            iv.visibility = View.GONE
            tvInitials.visibility = View.VISIBLE
            tvInitials.text = fullName.trim().split(" ").filter { it.isNotEmpty() }
                .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        }
    }

    private fun setupTheme(view: View) {
        val context = context ?: return
        val mode = PrefsHelper.getThemeMode(context)
        updateThemeButtons(view, mode)

        view.findViewById<View>(R.id.btn_theme_light)?.setOnClickListener {
            toast("ARTS uses a light theme only")
        }
        view.findViewById<View>(R.id.btn_theme_dark)?.setOnClickListener {
            toast("ARTS uses a light theme only")
        }
    }

    private fun updateThemeButtons(view: View, mode: String) {
        if (!isAdded) return
        val light = view.findViewById<Button>(R.id.btn_theme_light) ?: return
        val dark = view.findViewById<Button>(R.id.btn_theme_dark) ?: return
        
        val activeColor = ContextCompat.getColor(requireContext(), R.color.accent_green)
        val inactiveColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        
        light.setBackgroundColor(if (mode == "light") activeColor else inactiveColor)
        dark.setBackgroundColor(if (mode == "dark") activeColor else inactiveColor)
        
        val white = ContextCompat.getColor(requireContext(), R.color.white)
        light.setTextColor(if (mode == "light") white else activeColor)
        dark.setTextColor(if (mode == "dark") white else activeColor)
    }

    private fun setupLanguage(view: View) {
        view.findViewById<View>(R.id.btn_lang_en)?.setOnClickListener { setLanguage("en") }
        view.findViewById<View>(R.id.btn_lang_af)?.setOnClickListener { setLanguage("af") }
        view.findViewById<View>(R.id.btn_lang_zu)?.setOnClickListener { setLanguage("zu") }
    }

    private fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
