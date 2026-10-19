package com.example.arts.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.arts.R
import com.example.arts.utils.hide
import com.example.arts.utils.show
import com.example.arts.utils.toast
import com.example.arts.viewmodels.AuthState
import com.example.arts.viewmodels.AuthViewModel

class RegisterFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var btnCreateAccount: Button
    private lateinit var tvSignIn: TextView
    private lateinit var progressBar: ProgressBar

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        observeViewModel()
        setupClickListeners()
    }

    private fun bindViews(view: View) {
        etFullName = view.findViewById(R.id.et_full_name)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        etPassword = view.findViewById(R.id.et_password)
        ivTogglePassword = view.findViewById(R.id.iv_toggle_password)
        btnCreateAccount = view.findViewById(R.id.btn_create_account)
        tvSignIn = view.findViewById(R.id.tv_sign_in)
        progressBar = view.findViewById(R.id.progress_bar)
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    progressBar.show()
                    btnCreateAccount.isEnabled = false
                }
                is AuthState.Success -> {
                    progressBar.hide()
                    btnCreateAccount.isEnabled = true
                    toast("Welcome, ${state.user.fullName.split(" ").first()}!")
                    findNavController().navigate(R.id.action_register_to_home)
                    authViewModel.resetState()
                }
                is AuthState.Error -> {
                    progressBar.hide()
                    btnCreateAccount.isEnabled = true
                    toast(state.message)
                    authViewModel.resetState()
                }
                is AuthState.Idle -> {
                    progressBar.hide()
                    btnCreateAccount.isEnabled = true
                }
                else -> Unit
            }
        }
    }

    private fun setupClickListeners() {
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType = if (isPasswordVisible)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            etPassword.setSelection(etPassword.text.length)
        }

        btnCreateAccount.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            authViewModel.register(name, email, phone, password)
        }

        tvSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }
}
