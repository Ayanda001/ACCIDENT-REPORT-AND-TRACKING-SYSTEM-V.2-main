package com.example.arts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.arts.R
import com.example.arts.utils.BiometricHelper
import com.example.arts.utils.PrefsHelper
import com.example.arts.utils.TotpHelper
import com.example.arts.utils.toast
import com.example.arts.viewmodels.AuthViewModel
import com.example.arts.viewmodels.ProfileViewModel

class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The signed-in person's profile, loaded from Firestore
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                view.findViewById<TextView>(R.id.tv_name).text = it.fullName
                view.findViewById<TextView>(R.id.tv_email).text = it.email
                val bitmap = com.example.arts.utils.ImageStorageHelper.decodeDataUrl(it.avatarDataUrl)
                if (bitmap != null) {
                    view.findViewById<android.widget.ImageView>(R.id.iv_profile_avatar).apply {
                        setImageBitmap(bitmap)
                        visibility = View.VISIBLE
                    }
                }
                view.findViewById<TextView>(R.id.tv_phone).text = it.phone
            }
        }

        // Report counts, from the person's own incidents in Firestore
        profileViewModel.totalReports.observe(viewLifecycleOwner) { count ->
            view.findViewById<TextView>(R.id.tv_reports_count).text = count.toString()
        }
        profileViewModel.resolvedReports.observe(viewLifecycleOwner) { count ->
            view.findViewById<TextView>(R.id.tv_resolved_count).text = count.toString()
        }

        // Back button
        view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }

        // Settings rows
        view.findViewById<CardView>(R.id.item_security).setOnClickListener {
            showSecurityOptionsDialog()
        }
        view.findViewById<CardView>(R.id.item_my_reports).setOnClickListener {
            findNavController().navigate(R.id.incidentsFragment)
        }
        view.findViewById<CardView>(R.id.item_medical_info).setOnClickListener {
            findNavController().navigate(R.id.medicalInfoFragment)
        }
        view.findViewById<CardView>(R.id.item_settings).setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }
        view.findViewById<CardView>(R.id.item_help).setOnClickListener {
            findNavController().navigate(R.id.helpFragment)
        }

        // Logout option (long press profile name)
        view.findViewById<TextView>(R.id.tv_name).setOnLongClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    authViewModel.logout()
                    findNavController().navigate(R.id.loginFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    private fun showSecurityOptionsDialog() {
        val options = arrayOf("Biometric Unlock", "Two-Factor Authentication (TOTP)")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Security")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBiometricSettingsDialog()
                    1 -> showTotpSettingsDialog()
                }
            }
            .show()
    }

    private fun showTotpSettingsDialog() {
        val context = requireContext()
        val currentUser = profileViewModel.user.value

        if (currentUser?.totpEnabled == true) {
            android.app.AlertDialog.Builder(context)
                .setTitle("Two-Factor Authentication")
                .setMessage("2FA is currently ON for your account. Turn it off?")
                .setPositiveButton("Turn Off") { _, _ ->
                    authViewModel.disableTotp {
                        profileViewModel.loadUser()
                        toast("2FA disabled")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // Step 1: generate a secret and show it for the user to add to
        // their authenticator app (Google Authenticator, Authy, etc).
        val secret = TotpHelper.generateSecret()

        android.app.AlertDialog.Builder(context)
            .setTitle("Set Up 2FA")
            .setMessage(
                "Add this key to your authenticator app (Google Authenticator, Authy, etc):\n\n" +
                    "$secret\n\n" +
                    "Then tap Continue and enter the 6-digit code it shows."
            )
            .setPositiveButton("Continue") { _, _ ->
                showTotpConfirmDialog(secret)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTotpConfirmDialog(secret: String) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "6-digit code"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(6))
        }
        val padding = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirm 2FA Setup")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val code = input.text.toString().trim()
                if (TotpHelper.verifyCode(secret, code)) {
                    authViewModel.enableTotp(secret) {
                        profileViewModel.loadUser()
                        toast("2FA enabled")
                    }
                } else {
                    toast("That code didn't match — 2FA was not enabled")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBiometricSettingsDialog() {
        val context = requireContext()
        val currentlyEnabled = PrefsHelper.isBiometricEnabledFor(context, authViewModel.currentUid())

        if (currentlyEnabled) {
            android.app.AlertDialog.Builder(context)
                .setTitle("Biometric Login")
                .setMessage("Biometric unlock is currently ON for this account on this phone. Turn it off?")
                .setPositiveButton("Turn Off") { _, _ ->
                    PrefsHelper.disableBiometric(context)
                    toast("Biometric unlock disabled")
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        when (BiometricHelper.checkAvailability(this)) {
            BiometricHelper.Availability.AVAILABLE -> {
                android.app.AlertDialog.Builder(context)
                    .setTitle("Enable Biometric Unlock")
                    .setMessage("Use your fingerprint or face to open ARTS next time you launch it?")
                    .setPositiveButton("Enable") { _, _ ->
                        // Confirm the sensor actually works before turning the setting on.
                        BiometricHelper.authenticate(
                            fragment = this,
                            title = "Confirm Biometric Unlock",
                            subtitle = "Verify it's you to enable this feature",
                            onSuccess = {
                                val uid = authViewModel.currentUid()
                                if (uid != null) {
                                    PrefsHelper.enableBiometric(context, uid)
                                    toast("Biometric unlock enabled")
                                } else {
                                    toast("Couldn't identify your account, please try again")
                                }
                            },
                            onError = { message -> toast(message) }
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            BiometricHelper.Availability.NONE_ENROLLED -> {
                toast("No fingerprint/face set up on this device yet. Add one in your phone's settings first.")
            }
            else -> {
                toast("This device doesn't support biometric login")
            }
        }
    }
}
