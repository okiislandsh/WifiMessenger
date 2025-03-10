package jp.okiislandsh.util.wifitoys.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.pm.PackageInfoCompat;

import jp.okiislandsh.library.android.IContextAccess;
import jp.okiislandsh.util.wifitoys.R;

public class AppNameAndVersionTextView extends AppCompatTextView implements IContextAccess {
    public AppNameAndVersionTextView(@NonNull Context context) {
        super(context);
    }
    public AppNameAndVersionTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AppNameAndVersionTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        try {
            final long version = PackageInfoCompat.getLongVersionCode(requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0));
            setText(getString(R.string.nav_header_title) + " (Revision " + version + ")");
        }catch (Exception e){
            setText(R.string.nav_header_title);
        }
    }

}
