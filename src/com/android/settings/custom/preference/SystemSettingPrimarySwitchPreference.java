/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.custom.preference;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;

import androidx.preference.PreferenceDataStore;

import com.android.settingslib.PrimarySwitchPreference;

public class SystemSettingPrimarySwitchPreference extends PrimarySwitchPreference {

    public SystemSettingPrimarySwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setPreferenceDataStore(new DataStore());
    }

    public SystemSettingPrimarySwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setPreferenceDataStore(new DataStore());
    }

    public SystemSettingPrimarySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPreferenceDataStore(new DataStore());
    }

    public SystemSettingPrimarySwitchPreference(Context context) {
        super(context, null);
        setPreferenceDataStore(new DataStore());
    }

    private class DataStore extends PreferenceDataStore {
        @Override
        public void putBoolean(String key, boolean value) {
            Settings.System.putInt(getContext().getContentResolver(), key, value ? 1 : 0);
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return Settings.System.getInt(getContext().getContentResolver(), key,
                    defaultValue ? 1 : 0) != 0;
        }
    }
}
