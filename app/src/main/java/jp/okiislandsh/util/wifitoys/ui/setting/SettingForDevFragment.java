package jp.okiislandsh.util.wifitoys.ui.setting;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import jp.okiislandsh.util.wifitoys.R;

public class SettingForDevFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_dev, null);
    }
}