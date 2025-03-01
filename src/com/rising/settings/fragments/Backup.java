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
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.preferences.ui.AdaptiveListPreference;
import com.android.settings.preferences.CustomSeekBarPreference;
import com.android.settings.preferences.GlobalSettingSwitchPreference;
import com.android.settings.preferences.RisingSystemSettingListPreference;
import com.android.settings.preferences.SystemPropertyListPreference;
import com.android.settings.preferences.SecureSettingListPreference;
import com.android.settings.preferences.SecureSettingSwitchPreference;
import com.android.settings.preferences.SystemSettingListPreference;
import com.android.settings.preferences.SystemSettingSeekBarPreference;
import com.android.settings.preferences.SystemSettingSwitchPreference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SearchIndexable
public class Backup extends SettingsPreferenceFragment {

    private static final String TAG = "Backup";
    private static final String BACKUP_PERSONALIZATION_SETTINGS = "backup_personalization_settings";
    private static final String RESTORE_PERSONALIZATION_SETTINGS = "restore_personalization_settings";

    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rising_settings_backup);

        final PreferenceScreen prefScreen = getPreferenceScreen();
        Context mContext = getActivity().getApplicationContext();

        // Initialize ActivityResultLaunchers for file selection
        backupLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    Log.d(TAG, "Backup URI: " + uri.toString());
                    backupSettings(mContext, uri);
                } else {
                    Log.e(TAG, "Backup URI is null");
                }
            } else {
                Log.e(TAG, "Backup activity result not OK or data is null");
            }
        });

        restoreLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    Log.d(TAG, "Restore URI: " + uri.toString());
                    restoreSettings(mContext, uri);
                } else {
                    Log.e(TAG, "Restore URI is null");
                }
            } else {
                Log.e(TAG, "Restore activity result not OK or data is null");
            }
        });

        // Backup settings
        Preference backupPref = findPreference(BACKUP_PERSONALIZATION_SETTINGS);
        if (backupPref != null) {
                backupPref.setOnPreferenceClickListener(preference -> {
                        Log.d(TAG, "Backup option clicked");
                        chooseFileLocationForBackup();
                        return true;
                });
        }

        // Restore settings
        Preference restorePref = findPreference(RESTORE_PERSONALIZATION_SETTINGS);
        if (restorePref != null) {
                restorePref.setOnPreferenceClickListener(preference -> {
                        Log.d(TAG, "Restore option clicked");
                        chooseFileForRestore();
                        return true;
                });
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    private void chooseFileLocationForBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "personalization_settings_backup.json");
        Log.d(TAG, "Launching file picker for backup");
        backupLauncher.launch(intent);
    }

    private void chooseFileForRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        Log.d(TAG, "Launching file picker for restore");
        restoreLauncher.launch(intent);
    }

    private void backupSettings(Context context, Uri uri) {
        try {
            JSONObject json = new JSONObject();

            // Back up System settings
            backupSettingsProvider(json, Settings.System.class, context.getContentResolver());
            
            // Back up Secure settings
            backupSettingsProvider(json, Settings.Secure.class, context.getContentResolver());
            
            // Back up Global settings
            backupSettingsProvider(json, Settings.Global.class, context.getContentResolver());

            // Write the JSON object to the backup file
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(json.toString().getBytes());
                    Toast.makeText(getActivity(), "Personalization settings backed up successfully!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Failed to backup settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void backupSettingsProvider(JSONObject json, Class<?> settingsClass, ContentResolver resolver) throws JSONException {
        try {
            // Determine which Settings class we're working with
            Uri uri;
            String methodName;
            
            if (settingsClass == Settings.System.class) {
                uri = Settings.System.CONTENT_URI;
                methodName = "System";
            } else if (settingsClass == Settings.Secure.class) {
                uri = Settings.Secure.CONTENT_URI;
                methodName = "Secure";
            } else if (settingsClass == Settings.Global.class) {
                uri = Settings.Global.CONTENT_URI;
                methodName = "Global";
            } else {
                return; // Unsupported settings type
            }
            
            // Query all settings from this provider
            Cursor cursor = resolver.query(uri, null, null, null, null);
            if (cursor != null) {
                // Create a JSON object for this settings type
                JSONObject settingsJson = new JSONObject();
                
                // Get column indices
                int nameIndex = cursor.getColumnIndex("name");
                int valueIndex = cursor.getColumnIndex("value");
                
                // Extract all settings
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIndex);
                    String value = cursor.getString(valueIndex);
                    
                    if (name != null && value != null) {
                        settingsJson.put(name, value);
                    }
                }
                
                cursor.close();
                
                // Add this settings type to the main JSON object
                json.put(methodName, settingsJson);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error backing up " + settingsClass.getSimpleName() + " settings", e);
        }
    }

    private void restoreSettings(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                StringBuilder builder = new StringBuilder();
                int ch;
                while ((ch = inputStream.read()) != -1) {
                    builder.append((char) ch);
                }

                JSONObject json = new JSONObject(builder.toString());
                ContentResolver resolver = context.getContentResolver();
                
                // Restore System settings
                if (json.has("System")) {
                    restoreSettingsProvider(json.getJSONObject("System"), Settings.System.class, resolver);
                }
                
                // Restore Secure settings
                if (json.has("Secure")) {
                    restoreSettingsProvider(json.getJSONObject("Secure"), Settings.Secure.class, resolver);
                }
                
                // Restore Global settings
                if (json.has("Global")) {
                    restoreSettingsProvider(json.getJSONObject("Global"), Settings.Global.class, resolver);
                }

                // Force refresh settings
                context.getContentResolver().notifyChange(Settings.System.CONTENT_URI, null);
                context.getContentResolver().notifyChange(Settings.Secure.CONTENT_URI, null);
                context.getContentResolver().notifyChange(Settings.Global.CONTENT_URI, null);

                Toast.makeText(getActivity(), "Personalization settings restored successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Failed to restore settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreSettingsProvider(JSONObject settingsJson, Class<?> settingsClass, ContentResolver resolver) throws JSONException {
        try {
            Iterator<String> keys = settingsJson.keys();
            
            while (keys.hasNext()) {
                String key = keys.next();
                String value = settingsJson.getString(key);
                
                if (settingsClass == Settings.System.class) {
                    Settings.System.putString(resolver, key, value);
                } else if (settingsClass == Settings.Secure.class) {
                    Settings.Secure.putString(resolver, key, value);
                } else if (settingsClass == Settings.Global.class) {
                    Settings.Global.putString(resolver, key, value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring " + settingsClass.getSimpleName() + " settings", e);
        }
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_backup) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
