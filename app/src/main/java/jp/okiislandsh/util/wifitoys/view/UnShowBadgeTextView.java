package jp.okiislandsh.util.wifitoys.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import jp.okiislandsh.library.android.net.service.ipmsg.IPMDataBase;
import jp.okiislandsh.library.android.view.live.LiveBadgeTextView;

public class UnShowBadgeTextView extends FrameLayout {

    public UnShowBadgeTextView(Context context) {
        super(context);
    }

    public UnShowBadgeTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public UnShowBadgeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    { //未読件数のLiveDataを初期化に使用する
        LiveBadgeTextView<Long> textView = new LiveBadgeTextView<>(getContext());
        textView.setLiveData(IPMDataBase.getInstance(getContext()).asyncCountOfUnShowedMessages());
        final int badgeSize = (int)(textView.getTextSize()*1.5f);
        textView.setLayoutParams(new LayoutParams(badgeSize, badgeSize, Gravity.CENTER));
        addView(textView);

    }

}
