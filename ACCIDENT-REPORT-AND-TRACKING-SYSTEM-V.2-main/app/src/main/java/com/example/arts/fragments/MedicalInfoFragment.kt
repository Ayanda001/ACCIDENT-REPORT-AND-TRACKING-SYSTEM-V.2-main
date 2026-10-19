package com.example.arts.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.arts.R
import com.example.arts.utils.toast
import com.example.arts.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

/**
 * Emergency medical information. Field names match the website's, so
 * what is saved here shows to administrators on the website when this
 * person reports an incident, and the other way round.
 */
class MedicalInfoFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_medical_info, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }

        val etIdNumber = view.findViewById<EditText>(R.id.et_id_number)
        val etAge = view.findViewById<EditText>(R.id.et_age)
        val etGender = view.findViewById<EditText>(R.id.et_gender)
        val etBloodType = view.findViewById<EditText>(R.id.et_blood_type)
        val etHeight = view.findViewById<EditText>(R.id.et_height)
        val etWeight = view.findViewById<EditText>(R.id.et_weight)
        val etAllergies = view.findViewById<EditText>(R.id.et_allergies)
        val etConditions = view.findViewById<EditText>(R.id.et_conditions)
        val etMedications = view.findViewById<EditText>(R.id.et_medications)
        val etHospital = view.findViewById<EditText>(R.id.et_hospital)
        val etAidProvider = view.findViewById<EditText>(R.id.et_aid_provider)
        val etAidNumber = view.findViewById<EditText>(R.id.et_aid_number)
        val etEcName = view.findViewById<EditText>(R.id.et_ec_name)
        val etEcPhone = view.findViewById<EditText>(R.id.et_ec_phone)
        val btnSave = view.findViewById<Button>(R.id.btn_save_medical)

        // Pre-fill with whatever is already saved.
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            etIdNumber.setText(user.idNumber)
            etAge.setText(user.age)
            etGender.setText(user.gender)
            etBloodType.setText(user.bloodType)
            etHeight.setText(user.heightCm)
            etWeight.setText(user.weightKg)
            etAllergies.setText(user.allergiesOrConditions)
            etConditions.setText(user.chronicConditions)
            etMedications.setText(user.currentMedications)
            etHospital.setText(user.preferredHospital)
            etAidProvider.setText(user.medicalAidProvider)
            etAidNumber.setText(user.medicalAidNumber)
            etEcName.setText(user.emergencyContactName)
            etEcPhone.setText(user.emergencyContactPhone)
        }

        fun EditText.value(): String? = text.toString().trim().ifBlank { null }

        btnSave.setOnClickListener {
            val age = etAge.value()
            if (age != null && (age.toIntOrNull() == null || age.toInt() !in 0..120)) {
                toast("Enter a valid age between 0 and 120")
                return@setOnClickListener
            }
            if (profileViewModel.user.value == null) {
                toast("Your profile is still loading. Please wait a moment and try again.")
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    profileViewModel.saveMedical(
                        mapOf(
                            "idNumber" to etIdNumber.value(),
                            "age" to age,
                            "gender" to etGender.value(),
                            "bloodType" to etBloodType.value(),
                            "heightCm" to etHeight.value(),
                            "weightKg" to etWeight.value(),
                            "allergiesOrConditions" to etAllergies.value(),
                            "chronicConditions" to etConditions.value(),
                            "currentMedications" to etMedications.value(),
                            "preferredHospital" to etHospital.value(),
                            "medicalAidProvider" to etAidProvider.value(),
                            "medicalAidNumber" to etAidNumber.value(),
                            "emergencyContactName" to etEcName.value(),
                            "emergencyContactPhone" to etEcPhone.value()
                        )
                    )
                    toast("Medical information saved")
                } catch (e: Exception) {
                    Log.w("ARTS-Medical", "Save failed: ${e.message}")
                    toast("Couldn't save your information: ${e.message}")
                } finally {
                    btnSave.isEnabled = true
                }
            }
        }
    }
}
