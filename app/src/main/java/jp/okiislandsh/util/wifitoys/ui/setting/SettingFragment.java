package jp.okiislandsh.util.wifitoys.ui.setting;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceFragmentCompat;

import jp.okiislandsh.library.android.live.NonNullLiveData;
import jp.okiislandsh.util.wifitoys.MyApp;
import jp.okiislandsh.util.wifitoys.R;
import jp.okiislandsh.util.wifitoys.preference.PrefBool;

public class SettingFragment extends PreferenceFragmentCompat {
    protected Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    //強参照で保持しないと破棄される
    private final @NonNull NonNullLiveData<Boolean> liveDeveloperMode = PrefBool.DEVELOPER_MODE.getLive(MyApp.app);

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public @NonNull View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View ret = super.onCreateView(inflater, container, savedInstanceState);

        liveDeveloperMode.observe(getViewLifecycleOwner(), bool->{
            if(context instanceof Activity) ((Activity) context).invalidateOptionsMenu();
        });

        return ret;
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
                MenuProvider.super.onPrepareMenu(menu);
                if(PrefBool.DEVELOPER_MODE.get(context)){
                    menu.add(0, R.string.preference_category_developer, 0, R.string.preference_category_developer);
                }
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.string.preference_category_developer) {
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.nav_setting_dev);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); //Fragmentでaddする時の作法

    }
}