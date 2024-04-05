/*
 * Copyright (C) 2016-2024 crDroid Android Project
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
package com.crdroid.settings.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.util.rising.SystemRestartUtils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class UserInterface extends SettingsPreferenceFragment 
    implements Indexable {

    public static final String TAG = "UserInterface";

    private static final String SETTINGS_STYLES_KEY = "settings_theme_style";

    private ListPreference settingsStylePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.crdroid_settings_ui);

        final int defaultStyle = Settings.System.getInt(getContext().getContentResolver(), SETTINGS_STYLES_KEY, 0);
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        settingsStylePref = (ListPreference) findPreference(SETTINGS_STYLES_KEY);
        final CharSequence[] entryValues = getResources().getTextArray(R.array.settings_style_values);
        final CharSequence[] entries = getResources().getTextArray(R.array.settings_style_entries);
        final int index = Arrays.asList(entryValues).indexOf(String.valueOf(defaultStyle));
        if (index != -1) {
            settingsStylePref.setValue(String.valueOf(defaultStyle));
            settingsStylePref.setSummary(entries[index]);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CRDROID_SETTINGS;
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.crdroid_settings_ui;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
