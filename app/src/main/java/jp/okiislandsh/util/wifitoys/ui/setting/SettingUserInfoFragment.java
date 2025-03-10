package jp.okiislandsh.util.wifitoys.ui.setting;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMPrefString;
import jp.okiislandsh.util.wifitoys.R;

public class SettingUserInfoFragment extends PreferenceFragmentCompat {

    /** LogDBラッパー */
    protected static final @NonNull LogDB.ILog<CharSequence> Log = LogDB.getStringInstance();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_user_info, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

            }
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuProvider.super.onPrepareMenu(menu); //一応

                menu.add(0, R.string.action_random_nickname, 0, R.string.action_random_nickname);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                final int itemId = menuItem.getItemId();
                if (itemId == R.string.action_random_nickname) {
                    IPMPrefString.NN.set(requireContext(), IPMPrefString.getRandomNickName(getResources()));
                    try {
                        MyUtil.requireNonNull(findPreference("ipm_string_nn"), pref -> ((EditTextPreference) pref).setText(IPMPrefString.NN.get(requireContext())));
                    } catch (Exception e) {
                        Log.w("PreferenceFragmentのNick Nameの表示更新に失敗", e);
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); //Fragmentでaddする時の作法

    }
}