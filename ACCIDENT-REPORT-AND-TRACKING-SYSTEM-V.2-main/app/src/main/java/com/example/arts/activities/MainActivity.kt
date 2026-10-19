package com.example.arts.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.arts.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabReport: View

    // Fragments where bottom nav and central FAB should be HIDDEN.
    // This includes auth screens and any detailed "sub-screens" to avoid
    // UI overlapping and ensure clear focus on the feature.
    private val hideNavFragments = setOf(
        R.id.loginFragment,
        R.id.registerFragment,
        R.id.aiAssistantFragment,
        R.id.reportAccidentFragment,
        R.id.incidentDetailFragment,
        R.id.medicalInfoFragment,
        R.id.settingsFragment,
        R.id.helpFragment,
        R.id.voiceReportFragment,
        R.id.weatherAlertFragment,
        R.id.nearbyUnitsFragment,
        R.id.safetyScoreFragment,
        R.id.helplinesFragment,
        R.id.communityFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        fabReport = findViewById(R.id.fab_report)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Link bottom nav with nav controller
        bottomNav.setupWithNavController(navController)

        // Show/hide bottom nav based on current fragment
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in hideNavFragments) {
                bottomNav.visibility = View.GONE
                fabReport.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
                fabReport.visibility = View.VISIBLE
            }
        }

        // FAB opens Report Accident screen
        fabReport.setOnClickListener {
            navController.navigate(R.id.reportAccidentFragment)
        }

        // Override bottom nav item selection
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.incidentsFragment -> {
                    navController.navigate(R.id.incidentsFragment)
                    true
                }
                R.id.alertsFragment -> {
                    navController.navigate(R.id.alertsFragment)
                    true
                }
                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
