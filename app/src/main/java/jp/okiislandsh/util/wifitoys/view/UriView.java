package jp.okiislandsh.util.wifitoys.view;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.io.File;
import java.util.Objects;

import jp.okiislandsh.library.android.BitmapUtil;
import jp.okiislandsh.library.android.drawable.TextDrawable;
import jp.okiislandsh.library.android.live.LiveConverter1;
import jp.okiislandsh.library.android.live.LiveLive;
import jp.okiislandsh.library.android.view.live.LifecycleFrameLayout;
import jp.okiislandsh.util.wifitoys.LiveThumbnailLruCache;

/** Uriをできるだけ表示する */
public class UriView extends LifecycleFrameLayout implements Observer<Drawable> {

    private final @NonNull ImageView imageView;
    private final @NonNull TextView textView;

    /** ImageView設定用 */
    private final @NonNull LiveLive.TruncateEqualData<Drawable> liveLiveDrawable = new LiveLive.TruncateEqualData<>();

    /** Uriから取得したサムネをキャッシュ */
    private static final @NonNull LiveThumbnailLruCache thumbnailCache = LiveThumbnailLruCache.getInstance();

    /** サムネBitmapをDrawableへ変換する */
    private final @NonNull LiveConverter1<Bitmap, Drawable> liveLiveConverter = LiveConverter1.newTruncateInstance(Objects::equals, (bitmap, cancel) -> {
        if(bitmap!=null){ //画面表示の際に適用されるデバイス固有Scaleを考慮してBitmapサイズを変更する
            bitmap = bitmap.copy(bitmap.getConfig(), false); //非メモリコピーでBitmap複製
            bitmap.setDensity((int) (bitmap.getDensity() / BitmapUtil.getDisplayMetricsScale(getResources())));
            return new BitmapDrawable(bitmap);
        }else{
            return new TextDrawable(isJa("プレビュー"+BR+"失敗", "Can not"+BR+"Preview"), 256, 256,
                    newColorPaint(Color.RED), TextDrawable.Align.CENTER, TextDrawable.VAlign.MIDDLE);
        }
    });

    public UriView(@NonNull Context context) {
        super(context);
        addViews(imageView = createImageView(), textView = createTextView());

        imageView.setVisibility(GONE);
        textView.setVisibility(GONE);

        observe(liveLiveDrawable, this);
    }

    @Override
    public void onChanged(@Nullable Drawable drawable) {
        imageView.setImageDrawable(drawable);
    }

    private @Nullable Uri uri;

    @Nullable
    public Uri getUri() {
        return uri;
    }

    public void detachUri(){
        uri = null;
        imageView.setVisibility(GONE);
        textView.setVisibility(GONE);
    }

    public void setMediaUri(@NonNull Uri uri){
        this.uri = uri;

        imageView.setVisibility(VISIBLE);
        textView.setVisibility(GONE);

        liveLiveConverter.setLiveData(thumbnailCache.getLiveData(getContext(), uri));
        liveLiveDrawable.setLiveData(liveLiveConverter);

    }

    public void setDirUri(@Nullable Uri uri){
        setDirUri(uri, "DIR");
    }
    public void setDirUri(@Nullable Uri uri, @NonNull String text){
        this.uri = uri;

        textView.setVisibility(VISIBLE);
        imageView.setVisibility(VISIBLE);
        liveLiveDrawable.setLiveData(new MutableLiveData<>(getDrawable(jp.okiislandsh.library.android.R.drawable.ic_file_dir, false)));
        textView.setText(text);

    }

    /** アプリ内ストレージのFile Uriの時、Fileオブジェクトを返す */
    public static File getFileIfInternal(@NonNull Context context, @Nullable Uri uri) {
        return getFileIfFileUri(context, uri, true);
    }

    /** File Uriの時、Fileオブジェクトを返す */
    public static File getFileIfFileUri(@NonNull Context context, @Nullable Uri uri, boolean requireInternal) {
        if (uri == null) {
            return null;
        }

        // URI のスキームを確認する
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return null;
        }

        // URI のパスを取得する
        final @Nullable String uriPath = uri.getPath();
        if (uriPath == null) {
            return null;
        }

        if (!requireInternal || //アプリ内縛りじゃない
                uriPath.startsWith(context.getFilesDir().getAbsolutePath()) || //アプリ内ディレクトリ
                uriPath.startsWith(context.getCacheDir().getAbsolutePath()) ) { //アプリ内キャッシュディレクトリ
            return new File(uriPath);
        }

        //外部ストレージ
        return null;

    }

    public void setUnknownFileView(@Nullable Uri uri, @NonNull String message){
        this.uri = uri;

        textView.setVisibility(VISIBLE);
        imageView.setVisibility(VISIBLE);
        liveLiveDrawable.setLiveData(new MutableLiveData<>(getDrawable(jp.okiislandsh.library.android.R.drawable.ic_file_empty, false)));
        textView.setText(message);

    }

    public void setText(@NonNull String message){
        this.uri = null;

        textView.setVisibility(VISIBLE);
        imageView.setVisibility(GONE);
        liveLiveDrawable.setLiveData(null);
        textView.setText(message);

    }

    private @NonNull ImageView createImageView(){
        final @NonNull ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(newFrameParamsWW(Gravity.CENTER));
        imageView.setMaxWidth(getMeasuredWidth());
        imageView.setMaxHeight(getMeasuredWidth()); //最大で画面幅の正方形に制限
        return imageView;
    }

    private @NonNull TextView createTextView(){
        @NonNull TextView textView = newTextRelative(null, newFrameParamsWW(Gravity.CENTER), 1.2f);
        setPadding(textView, 40);
        textView.setMaxWidth(getMeasuredWidth());
        textView.setMaxHeight(getMeasuredWidth()); //最大で画面幅の正方形に制限
        return textView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        imageView.setMaxWidth(getMeasuredWidth());
        imageView.setMaxHeight(getMeasuredWidth()); //最大で画面幅の正方形に制限
        textView.setMaxWidth(getMeasuredWidth());
        textView.setMaxHeight(getMeasuredWidth()); //最大で画面幅の正方形に制限

    }
}
