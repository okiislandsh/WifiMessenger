package jp.okiislandsh.util.wifitoys.ui.about;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.okiislandsh.util.wifitoys.AbsDestinyFragment;
import jp.okiislandsh.util.wifitoys.databinding.FragmentAboutBinding;

public class AboutFragment extends AbsDestinyFragment {

    private FragmentAboutBinding bind;

    //region ライフサイクル
    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //ViewBindingインフレート
        bind = FragmentAboutBinding.inflate(inflater, container, false);

        return bind.getRoot();
    }

    @Override
    public void onDestroyView() {
        bind = null;

        super.onDestroyView();
    }
}