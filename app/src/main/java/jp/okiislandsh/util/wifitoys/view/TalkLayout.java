package jp.okiislandsh.util.wifitoys.view;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import jp.okiislandsh.library.android.SizeUtil;
import jp.okiislandsh.library.android.live.LiveLive;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Message;
import jp.okiislandsh.library.android.view.BadgeTextView;
import jp.okiislandsh.library.android.view.live.LifecycleLinearLayout;
import jp.okiislandsh.library.core.MyUtil;

public class TalkLayout extends LifecycleLinearLayout {

    protected final @NonNull LiveLive<IPMEntity_Address> liveAddress = new LiveLive<>();
    protected final @NonNull LiveLive<IPMEntity_Message> liveMessage = new LiveLive<>();

    /** root直下 */
    public final @NonNull BadgeTextView nameTextView;
    /** root直下 */
    public final @NonNull LinearLayout contentContainer;

    /** contentContainer直下ヘッダテキスト */
    public final @NonNull TextView headerTextView;

    /** カレントのレイアウト向き、左に名前ならtrue、右に名前ならfalse */
    private boolean layoutL2R = true;

    public TalkLayout(@NonNull Context context) {
        super(context);

        setOrientation(HORIZONTAL);

        //TextViewの初期文字サイズを基準に諸々の大きさを決定する
        final int charSizePx = SizeUtil.TEXT_SIZE.def.px(requireContext());
        final int char3SizePx = charSizePx*3;
        setMinimumHeight(char3SizePx);

        //名前View
        nameTextView = new BadgeTextView(requireContext());
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, charSizePx*1.2f);
        nameTextView.setLayoutParams(setMargins(new LayoutParams(char3SizePx, char3SizePx, 0), 4));
        nameTextView.setSingleLine(false);

        //コンテンツView
        contentContainer = newLinearLayout(VERTICAL, setMargins(newParamsWW(null), 8, 0, 8, 0));

        //ヘッダテキスト(既読表示)
        headerTextView = newTextRelative(null, null, .8f);
        contentContainer.addView(headerTextView);

        //L2Rに基づき配置
        addView(layoutL2R ? nameTextView : contentContainer);
        addView(layoutL2R ? contentContainer : nameTextView);
        headerTextView.setGravity(layoutL2R ? Gravity.START : Gravity.END);
        setHorizontalGravity(layoutL2R ? Gravity.START : Gravity.END);
        //setLayoutL2R(layoutL2R); 継承クラスに配慮、ここで呼び出すとオーバーライドしにくい

        //LiveData
        liveAddress.observe(this, a -> onChanged(a, liveMessage.getValue()));
        liveMessage.observe(this, m -> onChanged(liveAddress.getValue(), m));

    }

    public void setLiveData(@NonNull LiveData<IPMEntity_Address> liveAddress, @NonNull LiveData<IPMEntity_Message> liveMessage){
        this.liveAddress.setLiveData(liveAddress);
        this.liveMessage.setLiveData(liveMessage);
    }

    public void clearLiveData(){
        liveAddress.setLiveData(null);
        liveMessage.setLiveData(null);
    }

    protected void onChanged(@Nullable IPMEntity_Address a, @Nullable IPMEntity_Message m){

        if(m==null) {
            setAlpha(0);
        }else {
            setAlpha(1);

            if(layoutL2R != (!m.owner)){
                layoutL2R = !m.owner;

                //再配置のため既存削除
                removeAllViews();
                //
                addView(layoutL2R ? nameTextView : contentContainer);
                addView(layoutL2R ? contentContainer : nameTextView);
                headerTextView.setGravity(layoutL2R ? Gravity.START : Gravity.END);
                setHorizontalGravity(layoutL2R ? Gravity.START : Gravity.END);
            }

            final @NonNull String name = m.owner ? "me" : a==null ? "" : MyUtil.substring(a.getDisplayName(), 4).trim();
            if(!name.contentEquals(nameTextView.getText())) { //変更時のみ書き換える
                if(m.owner){
                    nameTextView.setTextColor(Color.BLACK);
                    nameTextView.setBackColor(Color.WHITE);
                }else{
                    nameTextView.setTextColor(Color.WHITE);
                    nameTextView.setBackColor(a==null ? Color.BLACK : a.hashColor(255, 0.6f, 0.8f));
                }
                nameTextView.setText(name);
            }

        }

    }

}
