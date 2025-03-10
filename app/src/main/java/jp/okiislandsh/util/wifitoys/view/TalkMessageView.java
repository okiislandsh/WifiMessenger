package jp.okiislandsh.util.wifitoys.view;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.text.HtmlCompat;
import androidx.core.view.GestureDetectorCompat;

import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Message;
import jp.okiislandsh.library.core.DateUtil;

/** テキストメッセージを表示するView */
public class TalkMessageView extends TalkLayout {

    /** contentContainer直下コンテンツCardView */
    public final @NonNull CardView cardView;

    /** cardView直下テキスト */
    public final @NonNull TextView messageTextView;

    public TalkMessageView(@NonNull Context context) {
        super(context);

        cardView = new CardView(context);
        contentContainer.addView(cardView);

        messageTextView = newText(null, null);
        cardView.addView(messageTextView);

        //SelectableなTextViewでクリックを処理するために必要
        final @NonNull GestureDetectorCompat mDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener());
        mDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                performClick();
                return false;
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return false;
            }
            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        });
        messageTextView.setOnTouchListener((v, event) -> {
            mDetector.onTouchEvent(event);
            return false;
        });

    }

    protected @Nullable IPMEntity_Message lastLiveData;
    public @Nullable IPMEntity_Message getLastLiveData(){
        return lastLiveData;
    }

    protected void onChanged(@Nullable IPMEntity_Address a, @Nullable IPMEntity_Message m) {
        super.onChanged(a, m);

        lastLiveData = m;

        //ヘッダテキストに関する変更があるか
        final @NonNull String newHeaderText = makeHeaderText(m);
        if(!newHeaderText.contentEquals(headerTextView.getText())){
            headerTextView.setText(newHeaderText);
        }

        //メッセージテキストに関する変更があるか
        final @NonNull String newMessageText = (m == null ? "" : m.msg);
        if(!newMessageText.contentEquals(messageTextView.getText())){
            if(Patterns.WEB_URL.matcher(newMessageText).matches()) { //テキストがURLを表す
                //HTML化して遷移可能に
                messageTextView.setMovementMethod(LinkMovementMethod.getInstance()); //リンクをクリック可能にする
                messageTextView.setText(HtmlCompat.fromHtml("<a href=\""+newMessageText+"\">"+newMessageText+"</a>", HtmlCompat.FROM_HTML_MODE_COMPACT)); //シングルクォートはURLに使用可能な文字なので、href属性はダブルクォートでくくる
            }else{ //それ以外のテキスト
                messageTextView.setTextIsSelectable(true);
                //messageTextView.setMovementMethod(null); selectableに応じてMovementMethodが上書きされるため、LinkMovementMethodの解除は不要
                messageTextView.setText(newMessageText);
            }
        }

    }

    private static @NonNull String makeHeaderText(@Nullable IPMEntity_Message entity){
        if(entity==null) {
            return "";
        }else{
            final @NonNull StringBuilder buf = new StringBuilder();
            buf.append(DateUtil.toEasyString(entity.date)).append(" ");
            if(entity.isRead()) {
                buf.append("read");
            }else if(!entity.isCommitted()){
                buf.append("sending");
            }
            return buf.toString();
        }
    }

}
