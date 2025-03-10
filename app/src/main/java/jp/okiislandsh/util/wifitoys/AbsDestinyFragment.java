package jp.okiislandsh.util.wifitoys;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.okiislandsh.library.android.AbsBaseFragment;
import jp.okiislandsh.library.core.Function;

public abstract class AbsDestinyFragment extends AbsBaseFragment {

    protected void invalidateOptionsMenu(){
        try{
            requireActivity().invalidateOptionsMenu();
        }catch (Exception e){
            Log.w("OptionMenuを再構成しようとしてエラー(invalidateOptionsMenu)", e);
        }
    }

    protected void setToolbarTitle(@NonNull String title){
        final @NonNull Activity context = requireActivity();
        if(context instanceof MainActivity){
            try{
                ((MainActivity) context).pleaseTitleChange(title);
            }catch (Exception e){
                Log.w("ツールバーのタイトルを変えようとしてエラー", e);
            }
        }
    }

    protected @NonNull MainActivity requireMainActivity(){
        return (MainActivity) requireActivity();
    }

    protected void requireMainActivity(@NonNull Function.voidNonNull<MainActivity> fn){
        final @Nullable Activity activity = getActivity();
        if(activity!=null) fn.run((MainActivity) activity);
    }

}