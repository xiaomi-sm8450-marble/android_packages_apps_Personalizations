/*
 * Copyright (C) 2023-2024 the risingOS Android Project
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
package com.rising.settings.fragments.lockscreen

import android.os.Bundle
import android.provider.Settings

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import com.android.settings.R

class LockscreenShortcuts : PreferenceFragmentCompat() {

    private lateinit var keyguardShortcutLeftPreference: Preference
    private lateinit var keyguardShortcutRightPreference: Preference
    private lateinit var keyguardCustomAppLeftPreference: Preference
    private lateinit var keyguardCustomAppRightPreference: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.rising_settings_lockscreen_shortcuts, rootKey)        
        activity?.title = getString(R.string.lockscreen_shortcut_category)
        keyguardShortcutLeftPreference = findPreference("keyguard_shortcut_left")!!
        keyguardShortcutRightPreference = findPreference("keyguard_shortcut_right")!!
        keyguardCustomAppLeftPreference = findPreference("keyguard_shortcut_custom_app_left")!!
        keyguardCustomAppRightPreference = findPreference("keyguard_shortcut_custom_app_right")!!
        updatePreferenceStates()
        keyguardShortcutLeftPreference.setOnPreferenceChangeListener { _, newValue ->
            handleShortcutChange("keyguard_shortcut_left", newValue as String)
            true
        }

        keyguardShortcutRightPreference.setOnPreferenceChangeListener { _, newValue ->
            handleShortcutChange("keyguard_shortcut_right", newValue as String)
            true
        }
        keyguardCustomAppLeftPreference.setOnPreferenceClickListener {
            showPackagePickerDialog("left")
            true
        }
        keyguardCustomAppRightPreference.setOnPreferenceClickListener {
            showPackagePickerDialog("right")
            true
        }
    }

    private fun handleShortcutChange(key: String, newValue: String) {
        if (key == "keyguard_shortcut_left") {
            keyguardCustomAppLeftPreference.isEnabled = newValue == "custom_app"
        } else if (key == "keyguard_shortcut_right") {
            keyguardCustomAppRightPreference.isEnabled = newValue == "custom_app"
        }
        if (newValue != "custom_app") {
            clearCustomAppPreference(key)
        }
    }

    private fun updatePreferenceStates() {
        val leftShortcut = Settings.System.getString(requireContext().contentResolver, "keyguard_shortcut_left")
        val rightShortcut = Settings.System.getString(requireContext().contentResolver, "keyguard_shortcut_right")

        keyguardCustomAppLeftPreference.isEnabled = leftShortcut == "custom_app"
        keyguardCustomAppRightPreference.isEnabled = rightShortcut == "custom_app"

        updateCustomAppSummaries()
    }

    private fun showPackagePickerDialog(shortcutPosition: String) {
        val titleResId = if (shortcutPosition == "left") {
            R.string.pick_package_left
        } else {
            R.string.pick_package_right
        }
        val dialog = PackagePickerDialogFragment(titleResId) { selectedPackage ->
            val key = if (shortcutPosition == "left") {
                "keyguard_shortcut_custom_app_left"
            } else {
                "keyguard_shortcut_custom_app_right"
            }
            Settings.System.putString(requireContext().contentResolver, key, selectedPackage)
            updateCustomAppSummaries()
        }
        dialog.show(childFragmentManager, "PackagePickerDialog")
    }

    private fun updateCustomAppSummaries() {
        val packageManager = requireContext().packageManager

        val customAppLeftPackage = Settings.System.getString(requireContext().contentResolver, "keyguard_shortcut_custom_app_left")
        if (!customAppLeftPackage.isNullOrEmpty()) {
            val appLabel = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(customAppLeftPackage, 0))
            } catch (e: Exception) {
                null
            }
            keyguardCustomAppLeftPreference.summary = appLabel?.toString() ?: customAppLeftPackage
        } else {
            keyguardCustomAppLeftPreference.summary = getString(R.string.keyguard_shortcut_custom_app_left_summary)
        }

        val customAppRightPackage = Settings.System.getString(requireContext().contentResolver, "keyguard_shortcut_custom_app_right")
        if (!customAppRightPackage.isNullOrEmpty()) {
            val appLabel = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(customAppRightPackage, 0))
            } catch (e: Exception) {
                null
            }
            keyguardCustomAppRightPreference.summary = appLabel?.toString() ?: customAppRightPackage
        } else {
            keyguardCustomAppRightPreference.summary = getString(R.string.keyguard_shortcut_custom_app_right_summary)
        }
    }

    private fun clearCustomAppPreference(key: String) {
        val customAppKey = when (key) {
            "keyguard_shortcut_left" -> "keyguard_shortcut_custom_app_left"
            "keyguard_shortcut_right" -> "keyguard_shortcut_custom_app_right"
            else -> return
        }
        Settings.System.putString(requireContext().contentResolver, customAppKey, null)
        updateCustomAppSummaries()
    }
}
