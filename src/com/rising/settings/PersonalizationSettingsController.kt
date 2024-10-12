/*
 * Copyright (C) 2023-2024 the RisingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rising.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.widget.LayoutPreference

class PersonalizationSettingsController(context: Context) : AbstractPreferenceController(context) {

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        screen.findPreference<LayoutPreference>(KEY_PERSONALIZATIONS)?.let { personalizationPref ->
            setupPersonalizationClickListeners(personalizationPref)
        }
    }

    private fun setupPersonalizationClickListeners(preference: LayoutPreference) {
        val personalizationsClickMap = mapOf(
            R.id.themes_card to "com.android.settings.Settings\$PersonalizationsLockscreenActivity",
            R.id.system_themes to "com.android.settings.Settings\$PersonalizationsThemesActivity",
            R.id.toolbox to "com.android.settings.Settings\$PersonalizationsToolboxActivity"
        )
        personalizationsClickMap.forEach { (viewId, activityName) ->
            preference.findViewById<View>(viewId)?.setOnClickListener {
                mContext.startActivity(createIntent(activityName))
            }
        }
    }

    private fun createIntent(activityName: String): Intent {
        return Intent().setComponent(ComponentName("com.android.settings", activityName))
    }

    override fun isAvailable(): Boolean = true

    override fun getPreferenceKey(): String = KEY_PERSONALIZATIONS

    companion object {
        private const val KEY_PERSONALIZATIONS = "personalization_dashboard_quick_access"
    }
}
