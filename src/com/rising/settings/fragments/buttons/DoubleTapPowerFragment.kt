/*
 * Copyright (C) 2023-2025 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rising.settings.fragments.buttons

import android.os.Bundle
import android.provider.Settings

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import com.android.settings.R

import com.rising.settings.fragments.lockscreen.PackagePickerDialogFragment

class DoubleTapPowerFragment : PreferenceFragmentCompat() {

    private lateinit var doublePressActionPref: Preference
    private lateinit var doublePressActionCustomAppPref: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.rising_settings_double_tap_power, rootKey)        
        activity?.title = getString(R.string.power_double_press_title)
        doublePressActionPref = findPreference("power_button_action_double_press")!!
        doublePressActionCustomAppPref = findPreference("power_button_action_double_press_custom_app_pkg")!!
        updatePreferenceStates()
        doublePressActionPref.setOnPreferenceChangeListener { _, newValue ->
            handleShortcutChange(newValue as String)
            true
        }
        doublePressActionCustomAppPref.setOnPreferenceClickListener {
            showPackagePickerDialog()
            true
        }
    }

    private fun handleShortcutChange(newValue: String) {
       doublePressActionCustomAppPref.isEnabled = newValue == "custom_app"
        if (newValue != "custom_app") {
            clearCustomAppPreference()
        }
    }

    private fun updatePreferenceStates() {
        val action = Settings.System.getString(requireContext().contentResolver, "power_button_action_double_press")
        doublePressActionCustomAppPref.isEnabled = action == "custom_app"
        updateCustomAppSummaries()
    }

    private fun showPackagePickerDialog() {
        val dialog = PackagePickerDialogFragment(R.string.pick_package_power_double_press) { selectedPackage ->
            Settings.System.putString(requireContext().contentResolver, "power_button_action_double_press_custom_app_pkg", selectedPackage)
            updateCustomAppSummaries()
        }
        dialog.show(childFragmentManager, "PackagePickerDialog")
    }

    private fun updateCustomAppSummaries() {
        val packageManager = requireContext().packageManager
        val customAppPkg = Settings.System.getString(requireContext().contentResolver, "power_button_action_double_press_custom_app_pkg")
        if (!customAppPkg.isNullOrEmpty()) {
            val appLabel = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(customAppPkg, 0))
            } catch (e: Exception) {
                null
            }
            doublePressActionCustomAppPref.summary = appLabel?.toString() ?: customAppPkg
        } else {
            doublePressActionCustomAppPref.summary = getString(R.string.power_double_press_custom_app_select)
        }
    }

    private fun clearCustomAppPreference() {
        Settings.System.putString(requireContext().contentResolver, "power_button_action_double_press_custom_app_pkg", null)
        updateCustomAppSummaries()
    }
}
