package jp.okiislandsh.util.wifitoys.preference;

import androidx.annotation.NonNull;

import jp.okiislandsh.library.android.preference.IBooleanPreference;

/** <pre>
 * //Preferenceから値を取得
 * boolean isAutoStartService = PrefBool.AUTO_START_SERVICE.get(context);
 *
 * //Preferenceへ値の保存
 * PrefBool.AUTO_START_SERVICE.set(context, false);</pre>
 * */
public enum PrefBool implements IBooleanPreference.INonNull {
    DEVELOPER_MODE(false),
    ;
    private final boolean defaultValue;
    PrefBool(boolean defaultValue){
        this.defaultValue = defaultValue;
    }

    @Override
    public @NonNull String getKey() {
        return "app_"+ name().toLowerCase();
    }

    @Override
    public @NonNull Boolean getDefaultValue() {
        return defaultValue;
    }

}
