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
package com.rising.settings.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.utils.SystemRestartUtils;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.util.android.ThemeUtils;

import java.util.List;

@SearchIndexable
public class Themes extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "Themes";
    private static final String KEY_PGB_STYLE = "progress_bar_style";
    private static final String KEY_NOTIF_STYLE = "notification_style";
    
    private ThemeUtils mThemeUtils;
    private Preference mProgressBarPref;
    private Preference mNotificationStylePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rising_settings_themes);
        mThemeUtils = ThemeUtils.getInstance(getActivity());
        mProgressBarPref = (Preference) findPreference(KEY_PGB_STYLE);
        mProgressBarPref.setOnPreferenceChangeListener(this);
        mNotificationStylePref = (Preference) findPreference(KEY_NOTIF_STYLE);
        mNotificationStylePref.setOnPreferenceChangeListener(this);
    }
    
    private void updateNotifStyle() {
        final int notifStyle = Settings.System.getIntForUser(
                getContext().getContentResolver(),
                KEY_NOTIF_STYLE, 
                0, 
                UserHandle.USER_CURRENT
        );
        String notifStyleCategory = "android.theme.customization.notification";
        String overlayThemeTarget = "com.android.systemui";
        String overlayPackage = null;
        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(getContext());
        }
        mThemeUtils.setOverlayEnabled(notifStyleCategory, overlayThemeTarget, overlayThemeTarget);
        if (notifStyle == 0) {
            SystemRestartUtils.restartSystemUI(getContext());
            return;
        }
        switch (notifStyle) {
            case 1:
                overlayPackage = "com.android.theme.notification.cyberpunk";
                break;
            case 2:
                overlayPackage = "com.android.theme.notification.duoline";
                break;
            case 3:
                overlayPackage = "com.android.theme.notification.ios";
                break;
            case 4:
                overlayPackage = "com.android.theme.notification.layers";
                break;
        }
        if (overlayPackage != null) {
            mThemeUtils.setOverlayEnabled(notifStyleCategory, overlayPackage, overlayThemeTarget);
        }
    }

    private void updateProgressBarStyle() {
        final int progressBarStyle = Settings.System.getIntForUser(
                getContext().getContentResolver(),
                KEY_PGB_STYLE, 
                0, 
                UserHandle.USER_CURRENT
        );
        String pgbStyleCategory = "android.theme.customization.progress_bar";
        String overlayThemeTarget = "android";
        String overlayPackage = null;
        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(getContext());
        }
        mThemeUtils.setOverlayEnabled(pgbStyleCategory, overlayThemeTarget, overlayThemeTarget);
        if (progressBarStyle == 0) return;
        switch (progressBarStyle) {
            case 1:
                overlayPackage = "com.android.theme.progressbar.blocky_thumb";
                break;
            case 2:
                overlayPackage = "com.android.theme.progressbar.minimal_thumb";
                break;
            case 3:
                overlayPackage = "com.android.theme.progressbar.outline_thumb";
                break;
        }
        if (overlayPackage != null) {
            mThemeUtils.setOverlayEnabled(pgbStyleCategory, overlayPackage, overlayThemeTarget);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mProgressBarPref) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    KEY_PGB_STYLE, value, UserHandle.USER_CURRENT);
            updateProgressBarStyle();
            return true;
        } else if (preference == mNotificationStylePref) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    KEY_NOTIF_STYLE, value, UserHandle.USER_CURRENT);
            updateNotifStyle();
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_themes) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
