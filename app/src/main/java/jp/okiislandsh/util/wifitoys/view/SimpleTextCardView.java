package jp.okiislandsh.util.wifitoys.view;

import static jp.okiislandsh.library.core.Function.with;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import jp.okiislandsh.library.android.SizeUtil;
import jp.okiislandsh.library.android.view.BadgeTextView;
import jp.okiislandsh.library.android.view.ViewBuilderFunction;

/** 見出しと2つのサブテキストを持つCardView */
public class SimpleTextCardView extends CardView implements ViewBuilderFunction.OnViewGroup {
    /** root直下 */
    final @NonNull LinearLayout container;
    /** root直下 */
    final @NonNull BadgeTextView badgeTextView;

    /** container直下 */
    final @NonNull TextView titleTextView;
    /** container直下 */
    final @NonNull FrameLayout subTitleContainer;

    /** subTitleContainer直下 */
    final @NonNull TextView sub1TextView;
    /** subTitleContainer直下 */
    final @NonNull TextView sub2TextView;

    public static class ResetTuple{
        final @NonNull CharSequence badgeText;
        final @NonNull CharSequence titleText;
        final @NonNull CharSequence sub1Text;
        final @NonNull CharSequence sub2Text;
        final @Nullable OnClickListener onClickListener;
        final @Nullable OnLongClickListener onLongClickListener;

        public ResetTuple(@NonNull CharSequence badgeText, @NonNull CharSequence titleText, @NonNull CharSequence sub1Text, @NonNull CharSequence sub2Text, @Nullable OnClickListener onClickListener, @Nullable OnLongClickListener onLongClickListener) {
            this.badgeText = badgeText;
            this.titleText = titleText;
            this.sub1Text = sub1Text;
            this.sub2Text = sub2Text;
            this.onClickListener = onClickListener;
            this.onLongClickListener = onLongClickListener;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ResetTuple &&
                    badgeText.equals(((ResetTuple) obj).badgeText) &&
                    titleText.equals(((ResetTuple) obj).titleText) &&
                    sub1Text.equals(((ResetTuple) obj).sub1Text) &&
                    sub2Text.equals(((ResetTuple) obj).sub2Text);
        }
    }

    public SimpleTextCardView(Context context) {
        super(context);

        //TextViewの初期文字サイズを基準に諸々の大きさを決定する
        final int charSizePx = SizeUtil.TEXT_SIZE.def.px(requireContext());
        final int char15SizePx = (int)(charSizePx*1.5f);
        final int char2SizePx = charSizePx*2;
        setMinimumHeight(charSizePx*3);

        container = addLinearLayout(LinearLayout.VERTICAL, setMargins(newFrameParamsMW(null), 8, 0, 8, 0));

        badgeTextView = new BadgeTextView(context);
        badgeTextView.setLayoutParams(setMargins(new LayoutParams(char15SizePx, char15SizePx, Gravity.END), 8, 4, 8, 4));
        addView(badgeTextView);


        titleTextView = newTextRelative(null,
                setMargins(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, char2SizePx), 1, 1, 16, 1),
                1.2f);
        titleTextView.setGravity(Gravity.CENTER);
        container.addView(titleTextView);


        container.addView(subTitleContainer =
                newFrameLayout(setMargins(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, char2SizePx), 1))
        );

        subTitleContainer.addView(
                sub1TextView = newText(null,
                        with(newFrameParamsWW(null), p->p.gravity=Gravity.START | Gravity.CENTER_VERTICAL),
                        null)
        );
        subTitleContainer.addView(
                sub2TextView = newText(null,
                        with(newFrameParamsWW(null), p->p.gravity=Gravity.END | Gravity.CENTER_VERTICAL),
                        null)
        );

    }

    public void reset(){
        container.setVisibility(INVISIBLE);
        badgeTextView.setText(null);
    }

    /** 最後にresetがコールされた際のTuple */
    @Nullable ResetTuple lastResetTuple;

    public void reset(@NonNull ResetTuple tuple){
        lastResetTuple = tuple;

        reset(tuple.badgeText, tuple.titleText, tuple.sub1Text, tuple.sub2Text, tuple.onClickListener, tuple.onLongClickListener);
    }

    private void reset(@NonNull CharSequence badgeText, @NonNull CharSequence titleText, @NonNull CharSequence sub1Text, @NonNull CharSequence sub2Text,
                      @Nullable OnClickListener onClickListener, @Nullable OnLongClickListener onLongClickListener){

        container.setVisibility(VISIBLE);

        //再利用して値を書き込む
        badgeTextView.setText(badgeText);
        titleTextView.setText(titleText);
        sub1TextView.setText(sub1Text);
        sub2TextView.setText(sub2Text);

        setOnClickListener(onClickListener);
        setOnLongClickListener(onLongClickListener);

    }

}
