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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.android.OmniJawsClient;
import com.android.internal.util.android.Utils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

import lineageos.providers.LineageSettings;

import com.android.settings.preferences.ui.PreferenceUtils;

import com.android.internal.util.android.SystemRestartUtils;

@SearchIndexable
public class LockScreen extends SettingsPreferenceFragment
            implements Preference.OnPreferenceChangeListener  {

    public static final String TAG = "LockScreen";

    private static final String LOCKSCREEN_INTERFACE_CATEGORY = "lockscreen_interface_category";
    private static final String LOCKSCREEN_GESTURES_CATEGORY = "lockscreen_gestures_category";
    private static final String LOCKSCREEN_FP_CATEGORY = "lockscreen_fp_category";
    private static final String LOCKSCREEN_UDFPS_CATEGORY = "lockscreen_udfps_category";
    private static final String KEY_RIPPLE_EFFECT = "enable_ripple_effect";
    private static final String KEY_WEATHER = "lockscreen_weather_enabled";
    private static final String KEY_UDFPS_ANIMATIONS = "udfps_recognizing_animation_preview";
    private static final String KEY_UDFPS_ICONS = "udfps_icon_picker";
    private static final String SCREEN_OFF_UDFPS_ENABLED = "screen_off_udfps_enabled";
    private static final String KEY_FP_SUCCESS = "fp_success_vibrate";
    private static final String KEY_FP_FAIL = "fp_error_vibrate";

    private Preference mUdfpsIcons;
    private Preference mUdfpsAnimation;
    private Preference mRippleEffect;
    private Preference mWeather;
    private Preference mScreenOffUdfps;
    private Preference mFpSuccess;
    private Preference mFpFail;
    
    private OmniJawsClient mWeatherClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rising_settings_lockscreen);

        PreferenceCategory udfpsCategory = (PreferenceCategory) findPreference(LOCKSCREEN_UDFPS_CATEGORY);
        PreferenceCategory fpCategory = (PreferenceCategory) findPreference(LOCKSCREEN_FP_CATEGORY);

        FingerprintManager mFingerprintManager = (FingerprintManager)
                getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        mUdfpsIcons = (Preference) findPreference(KEY_UDFPS_ICONS);
        mUdfpsAnimation = (Preference) findPreference(KEY_UDFPS_ANIMATIONS);
        mRippleEffect = (Preference) findPreference(KEY_RIPPLE_EFFECT);
        mScreenOffUdfps = (Preference) findPreference(SCREEN_OFF_UDFPS_ENABLED);
        mFpSuccess = (Preference) findPreference(KEY_FP_SUCCESS);
        mFpFail = (Preference) findPreference(KEY_FP_FAIL);

        if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected()) {
            if (udfpsCategory != null) {
                if (mUdfpsAnimation != null) udfpsCategory.removePreference(mUdfpsAnimation);
                if (mUdfpsIcons != null) udfpsCategory.removePreference(mUdfpsIcons);
                if (mScreenOffUdfps != null) udfpsCategory.removePreference(mScreenOffUdfps);
            }
            if (fpCategory != null) {
                if (mRippleEffect != null) fpCategory.removePreference(mRippleEffect);
                if (mFpSuccess != null) fpCategory.removePreference(mFpSuccess);
                if (mFpFail != null) fpCategory.removePreference(mFpFail);
            }
        } else {
            final boolean udfpsAnimationInstalled = Utils.isPackageInstalled(getContext(), "com.crdroid.udfps.animations");
            final boolean udfpsIconsInstalled = Utils.isPackageInstalled(getContext(), "com.crdroid.udfps.icons");
            if (!udfpsAnimationInstalled && udfpsCategory != null && mUdfpsAnimation != null) {
                udfpsCategory.removePreference(mUdfpsAnimation);
            }
            if (!udfpsIconsInstalled && udfpsCategory != null && mUdfpsIcons != null) {
                udfpsCategory.removePreference(mUdfpsIcons);
            }
            if (!udfpsAnimationInstalled && !udfpsIconsInstalled && udfpsCategory != null && mScreenOffUdfps != null) {
                udfpsCategory.removePreference(mScreenOffUdfps);
            }
        }

        mWeather = (Preference) findPreference(KEY_WEATHER);
        mWeatherClient = new OmniJawsClient(getContext());
        updateWeatherSettings();
        
        PreferenceScreen screen = getPreferenceScreen();
        PreferenceUtils.hideEmptyCategory(udfpsCategory, screen);
        PreferenceUtils.hideEmptyCategory(fpCategory, screen);
        com.android.settingslib.widget.LayoutPreference lockHighlightPref = screen.findPreference("lockscreen_highlight_dashboard");
        if (lockHighlightPref != null) {
            java.util.Map<Integer, String> lockHighlightClickMap = new java.util.HashMap<>();
            lockHighlightClickMap.put(R.id.lockscreen_widgets_tile, "PersonalizationsWidgetsActivity");
            lockHighlightClickMap.put(R.id.peek_display_tile, "PersonalizationsPDActivity");
            lockHighlightClickMap.put(R.id.aod_tile, "PersonalizationsAODActivity");
            lockHighlightClickMap.put(R.id.dw_tile, "PersonalizationsDWActivity");
            com.android.settings.utils.HighlightPrefUtils.Companion.setupHighlightPref(getContext(), lockHighlightPref, lockHighlightClickMap);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    private void updateWeatherSettings() {
        if (mWeatherClient == null || mWeather == null) return;

        boolean weatherEnabled = mWeatherClient.isOmniJawsEnabled();
        mWeather.setEnabled(weatherEnabled);
        mWeather.setSummary(weatherEnabled ? R.string.lockscreen_weather_summary :
            R.string.lockscreen_weather_enabled_info);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateWeatherSettings();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_lockscreen) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    FingerprintManager mFingerprintManager = (FingerprintManager)
                            context.getSystemService(Context.FINGERPRINT_SERVICE);
                    if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected()) {
                        keys.add(KEY_UDFPS_ANIMATIONS);
                        keys.add(KEY_UDFPS_ICONS);
                        keys.add(KEY_RIPPLE_EFFECT);
                        keys.add(SCREEN_OFF_UDFPS_ENABLED);
                    } else {
                        if (!Utils.isPackageInstalled(context, "com.crdroid.udfps.animations")) {
                            keys.add(KEY_UDFPS_ANIMATIONS);
                        }
                        if (!Utils.isPackageInstalled(context, "com.crdroid.udfps.icons")) {
                            keys.add(KEY_UDFPS_ICONS);
                        }
                        Resources resources = context.getResources();
                        boolean screenOffUdfpsAvailable = resources.getBoolean(
                            com.android.internal.R.bool.config_supportScreenOffUdfps) ||
                            !TextUtils.isEmpty(resources.getString(
                                com.android.internal.R.string.config_dozeUdfpsLongPressSensorType));
                        if (!screenOffUdfpsAvailable)
                            keys.add(SCREEN_OFF_UDFPS_ENABLED);
                        }
                    return keys;
                }
            };
}
