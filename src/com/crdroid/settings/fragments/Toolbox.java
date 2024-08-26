/*
 * Copyright (C) 2016-2022 crDroid Android Project
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
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.crdroid.settings.utils.RisingSettingsConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@SearchIndexable
public class Toolbox extends SettingsPreferenceFragment {

    private static final String TAG = "Toolbox";
    private static final String BACKUP_PERSONALIZATION_SETTINGS = "backup_personalization_settings";
    private static final String RESTORE_PERSONALIZATION_SETTINGS = "restore_personalization_settings";
    private static final String UPLOAD_BACKUP_TO_DRIVE = "upload_backup_to_drive";
    private static final String DOWNLOAD_BACKUP_FROM_DRIVE = "download_backup_from_drive";

    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;
    private ActivityResultLauncher<Intent> uploadLauncher;
    private ActivityResultLauncher<Intent> downloadLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.crdroid_settings_misc);
        final PreferenceScreen prefScreen = getPreferenceScreen();
        Context mContext = getActivity().getApplicationContext();
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
        uploadLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    mContext.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    Log.d(TAG, "Selected file URI: " + uri.toString());
                    uploadFileToDrive(mContext, uri);
                } else {
                    Log.e(TAG, "Selected file URI is null");
                }
            } else {
                Log.e(TAG, "Upload activity result not OK or data is null");
            }
        });
        downloadLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    Log.d(TAG, "Download URI: " + uri.toString());
                    restoreSettings(mContext, uri);
                } else {
                    Log.e(TAG, "Download URI is null");
                }
            } else {
                Log.e(TAG, "Download activity result not OK or data is null");
            }
        });
        Preference backupPref = findPreference(BACKUP_PERSONALIZATION_SETTINGS);
        backupPref.setOnPreferenceClickListener(preference -> {
            Log.d(TAG, "Backup option clicked");
            chooseFileLocationForBackup();
            return true;
        });
        Preference restorePref = findPreference(RESTORE_PERSONALIZATION_SETTINGS);
        restorePref.setOnPreferenceClickListener(preference -> {
            Log.d(TAG, "Restore option clicked");
            chooseFileForRestore();
            return true;
        });
        Preference uploadToDrivePref = findPreference(UPLOAD_BACKUP_TO_DRIVE);
        uploadToDrivePref.setOnPreferenceClickListener(preference -> {
            Log.d(TAG, "Upload to Drive option clicked");
            chooseFileForUpload();
            return true;
        });
        Preference downloadFromDrivePref = findPreference(DOWNLOAD_BACKUP_FROM_DRIVE);
        downloadFromDrivePref.setOnPreferenceClickListener(preference -> {
            Log.d(TAG, "Download from Drive option clicked");
            downloadBackupFromDrive();
            return true;
        });
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

    private void chooseFileForUpload() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        Log.d(TAG, "Launching file picker for upload");
        uploadLauncher.launch(intent);
    }

    private void uploadFileToDrive(Context context, Uri fileUri) {
        try {
            String jsonData = readJsonFromFileUri(context, fileUri);
            if (jsonData.isEmpty()) {
                Toast.makeText(context, getContext().getString(R.string.json_read_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "JSON Data: " + jsonData);
            context.getContentResolver().takePersistableUriPermission(fileUri, 
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Intent uploadIntent = new Intent(Intent.ACTION_SEND);
            uploadIntent.setType("application/json");
            uploadIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            uploadIntent.setPackage("com.google.android.apps.docs");
            uploadIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (uploadIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivityAsUser(uploadIntent, UserHandle.CURRENT);
            } else {
                Toast.makeText(context, getContext().getString(R.string.gdrive_not_installed), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to upload file to Google Drive: " + e.getMessage(), e);
            Toast.makeText(context, getContext().getString(R.string.backup_upload_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadBackupFromDrive() {
        Intent downloadIntent = new Intent(Intent.ACTION_GET_CONTENT);
        downloadIntent.setType("application/json");
        downloadIntent.addCategory(Intent.CATEGORY_OPENABLE);
        downloadIntent.setPackage("com.google.android.apps.docs");
        if (downloadIntent.resolveActivity(getContext().getPackageManager()) != null) {
            downloadLauncher.launch(downloadIntent);
        } else {
            Toast.makeText(getContext(), getContext().getString(R.string.gdrive_not_installed), Toast.LENGTH_SHORT).show();
        }
    }

    private void backupSettings(Context context, Uri uri) {
        try {
            JSONObject json = new JSONObject();
            addSystemSettingKeys(json);
            addSecureSettingKeys(json);
            addGlobalSettingKeys(json);
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
                    File backupFile = new File(context.getCacheDir(), "personalization_settings_backup.json");
                    try (OutputStream os = new FileOutputStream(backupFile)) {
                        os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    Toast.makeText(getActivity(), getContext().getString(R.string.backup_success), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), getContext().getString(R.string.backup_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void addSystemSettingKeys(JSONObject json) throws JSONException {
        for (String key : RisingSettingsConstants.SYSTEM_SETTINGS_KEYS) {
            json.put(key, Settings.System.getString(getContext().getContentResolver(), key));
        }
    }

    private void addSecureSettingKeys(JSONObject json) throws JSONException {
        for (String key : RisingSettingsConstants.SECURE_SETTINGS_KEYS) {
            json.put(key, Settings.Secure.getString(getContext().getContentResolver(), key));
        }
    }

    private void addGlobalSettingKeys(JSONObject json) throws JSONException {
        for (String key : RisingSettingsConstants.GLOBAL_SETTINGS_KEYS) {
            json.put(key, Settings.Global.getString(getContext().getContentResolver(), key));
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
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = json.get(key);
                    if (value instanceof Integer || value instanceof String) {
                        applySetting(context, key, value);
                    }
                }
                context.getContentResolver().notifyChange(Settings.System.CONTENT_URI, null);
                context.getContentResolver().notifyChange(Settings.Secure.CONTENT_URI, null);
                context.getContentResolver().notifyChange(Settings.Global.CONTENT_URI, null);
                Toast.makeText(getActivity(), getContext().getString(R.string.backup_restore_success), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), getContext().getString(R.string.backup_restore_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void applySetting(Context context, String key, Object value) {
        ContentResolver resolver = context.getContentResolver();
        if (isSystemSettingKey(key)) {
            if (value instanceof Integer) {
                Settings.System.putInt(resolver, key, (Integer) value);
            } else if (value instanceof String) {
                Settings.System.putString(resolver, key, (String) value);
            }
        } else if (isSecureSettingKey(key)) {
            if (value instanceof Integer) {
                Settings.Secure.putInt(resolver, key, (Integer) value);
            } else if (value instanceof String) {
                Settings.Secure.putString(resolver, key, (String) value);
            }
        } else if (isGlobalSettingKey(key)) {
            if (value instanceof Integer) {
                Settings.Global.putInt(resolver, key, (Integer) value);
            } else if (value instanceof String) {
                Settings.Global.putString(resolver, key, (String) value);
            }
        }
    }

    private boolean isSystemSettingKey(String key) {
        return Arrays.asList(RisingSettingsConstants.SYSTEM_SETTINGS_KEYS).contains(key);
    }

    private boolean isSecureSettingKey(String key) {
        return Arrays.asList(RisingSettingsConstants.SECURE_SETTINGS_KEYS).contains(key);
    }

    private boolean isGlobalSettingKey(String key) {
        return Arrays.asList(RisingSettingsConstants.GLOBAL_SETTINGS_KEYS).contains(key);
    }
    
    private String readJsonFromFileUri(Context context, Uri fileUri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri)) {
            if (inputStream == null) {
                Log.e(TAG, "InputStream is null");
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON from file: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.crdroid_settings_misc) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
