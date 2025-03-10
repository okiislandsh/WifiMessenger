package jp.okiislandsh.util.wifitoys.preference;

import android.content.Context;
import android.text.format.Formatter;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import jp.okiislandsh.library.android.preference.SeekBarIntPreference;

/** MB表示のPreference */
public class SeekBarMBytePreference extends SeekBarIntPreference {
    //memo:全てのコンストラクタでsuperをコールしないと、いろいろバグる、たぶん引数２つのコンストラクタが肝
    // initでTypedArrayから属性を取得し、private int tmpへ代入した後、public final intで公開する

    public SeekBarMBytePreference(Context context) {
        super(context);
    }

    public SeekBarMBytePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekBarMBytePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SeekBarMBytePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected @NonNull String onTextFormat(@NonNull Integer value) {
        return Formatter.formatShortFileSize(getContext(), value*1024*1024L);
    }
}
