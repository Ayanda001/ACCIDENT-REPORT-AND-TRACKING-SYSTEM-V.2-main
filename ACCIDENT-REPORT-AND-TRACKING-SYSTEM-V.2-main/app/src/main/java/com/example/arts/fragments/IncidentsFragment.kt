package com.example.arts.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arts.R
import com.example.arts.adapters.IncidentsAdapter
import com.example.arts.utils.QrHelper
import com.example.arts.utils.toast
import com.example.arts.viewmodels.IncidentViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class IncidentsFragment : Fragment() {

    private val incidentViewModel: IncidentViewModel by viewModels()

    private lateinit var etSearch: EditText
    private lateinit var rvIncidents: RecyclerView
    private lateinit var chipAll: TextView
    private lateinit var chipPending: TextView
    private lateinit var chipInProgress: TextView
    private lateinit var chipResolved: TextView
    private lateinit var adapter: IncidentsAdapter

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val content = result.contents
        if (content == null) {
            // User cancelled - nothing to do.
            return@registerForActivityResult
        }
        val incidentId = QrHelper.parseIncidentId(content)
        if (incidentId != null) {
            val action = IncidentsFragmentDirections.actionIncidentsToDetail(incidentId)
            findNavController().navigate(action)
        } else {
            toast("That QR code isn't a recognized incident code")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_incidents, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupRecyclerView()
        observeViewModel()
        setupSearch()
        setupChips()

        view.findViewById<ImageView>(R.id.btn_scan_qr).setOnClickListener {
            launchScanner()
        }
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan an incident QR code")
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }

    private fun bindViews(view: View) {
        etSearch = view.findViewById(R.id.et_search)
        rvIncidents = view.findViewById(R.id.rv_incidents)
        chipAll = view.findViewById(R.id.chip_all)
        chipPending = view.findViewById(R.id.chip_pending)
        chipInProgress = view.findViewById(R.id.chip_in_progress)
        chipResolved = view.findViewById(R.id.chip_resolved)
    }

    private fun setupRecyclerView() {
        adapter = IncidentsAdapter(emptyList()) { incident ->
            val action = IncidentsFragmentDirections.actionIncidentsToDetail(incident.id)
            findNavController().navigate(action)
        }
        rvIncidents.layoutManager = LinearLayoutManager(requireContext())
        rvIncidents.adapter = adapter
    }

    private fun observeViewModel() {
        // Observe filtered incidents from ViewModel (Room DB + filter applied)
        incidentViewModel.filteredIncidents.observe(viewLifecycleOwner) { incidents ->
            adapter.updateList(incidents)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                incidentViewModel.setSearchQuery(s.toString())
            }
        })
    }

    private fun setupChips() {
        chipAll.setOnClickListener { selectChip("All") }
        chipPending.setOnClickListener { selectChip("Pending") }
        chipInProgress.setOnClickListener { selectChip("Forwarded") }
        chipResolved.setOnClickListener { selectChip("Resolved") }
    }

    private fun selectChip(filter: String) {
        incidentViewModel.setStatusFilter(filter)

        val chips = listOf(chipAll, chipPending, chipInProgress, chipResolved)
        val labels = listOf("All", "Pending", "Forwarded", "Resolved")

        chips.forEachIndexed { index, chip ->
            if (labels[index] == filter) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }
}
