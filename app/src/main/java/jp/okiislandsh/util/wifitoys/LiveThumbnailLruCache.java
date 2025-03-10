package jp.okiislandsh.util.wifitoys;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.LruCache;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Objects;

import jp.okiislandsh.library.android.BitmapUtil;
import jp.okiislandsh.library.android.MimeUtil;
import jp.okiislandsh.library.android.livelrucache.AbsLiveLruCacheAndroid;
import jp.okiislandsh.library.android.mediastore.MediaUtil;

public class LiveThumbnailLruCache extends AbsLiveLruCacheAndroid<Uri, Bitmap> {

    //初期化順に注意すること。staticは上から評価される
    private static final int BITMAP_OUTER_SIZE_MINI = 512;
    private static final int BITMAP_OUTER_SIZE_FULL = 1024;
    private static final int BITMAP_OUTER_SIZE_MICRO = 96;
    private static final int BITMAP_OUTER_SIZE = BITMAP_OUTER_SIZE_MINI;
    private static final int BITMAP_OUTER_SIZE_HALF = BITMAP_OUTER_SIZE/2;

    private static final long RUNTIME_MAX_MEMORY_KB = Runtime.getRuntime().maxMemory() / 1024;
    private static final float PERCENTAGE_OF_MEMORY = 0.01f;
    private static final int CACHE_SIZE_KB = (int)(RUNTIME_MAX_MEMORY_KB * PERCENTAGE_OF_MEMORY);

    private static final int MINI_KIND = 1; //new Point(512, 384)
    private static final int FULL_SCREEN_KIND = 2; //new Point(1024, 786)
    private static final int MICRO_KIND = 3; //new Point(96, 96)

    @Override
    protected @NonNull LruCache<Uri, Bitmap> makeCache() {
        return new LruCache<Uri, Bitmap>(CACHE_SIZE_KB){
            @Override
            protected int sizeOf(Uri key, Bitmap value) {
                return value.getByteCount()/1024;
            }
        };
    }

    @Override
    protected @Nullable Bitmap newValue(@NonNull Context context, @NonNull Uri uri) {
        try {
            Log.d("サムネ生成処理開始 newValue() uri="+uri);
            final @Nullable String scheme = uri.getScheme();
            if(scheme==null) return null;
            final @NonNull ContentResolver cr = context.getContentResolver();
            switch (scheme){
                case ContentResolver.SCHEME_CONTENT:
                    final @Nullable MediaUtil.MediaDataSet dataSet = MediaUtil.getMediaDataSet(cr, uri);
                    if(dataSet!=null && MimeUtil.SUPPORTED_IMAGE.existMimeType(dataSet.mimeType)) {
                        try {//ContentUris.parseId(uri)で例外、変わったUriとかがあってたまにidが取れない。
                            return MediaStore.Images.Thumbnails.getThumbnail(cr, ContentUris.parseId(uri), MINI_KIND, null);
                        }catch (Exception e){
                            Log.d("getThumbnailに失敗。getBitmapを試す。", e);
                            //resume next
                        }
                    }else if(dataSet!=null && MimeUtil.SUPPORTED_VIDEO.existMimeType(dataSet.mimeType)) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                return cr.loadThumbnail(uri, new Size(512, 384), null); //MINI_KIND相当
                            }else {
                                //noinspection deprecation //非推奨だがSDKが対応していないため古いAPIを実行
                                return MediaStore.Video.Thumbnails.getThumbnail(cr, ContentUris.parseId(uri), MINI_KIND, null);
                            }
                        }catch (Exception e){
                            Log.d("getThumbnailに失敗。getBitmapを試す。", e);
                            //resume next
                        }
                    }
                    //else
                    //ビットマップを縮小する方法でサムネ生成を試みる
                    return BitmapUtil.copyBitmapWithFitDensity(BitmapUtil.getBitmap(new BitmapUtil.MyUri(cr, uri), BITMAP_OUTER_SIZE_HALF, BITMAP_OUTER_SIZE_HALF),
                            BITMAP_OUTER_SIZE, BITMAP_OUTER_SIZE); //密度を変更して縦横2倍に
                case ContentResolver.SCHEME_FILE:
                    return BitmapUtil.copyBitmapWithFitDensity(BitmapUtil.getBitmap(new BitmapUtil.MyFile(new File(Objects.requireNonNull(uri.getPath()))), BITMAP_OUTER_SIZE_HALF, BITMAP_OUTER_SIZE_HALF),
                            BITMAP_OUTER_SIZE, BITMAP_OUTER_SIZE); //密度を変更して縦横2倍に
                case ContentResolver.SCHEME_ANDROID_RESOURCE:
                default:
                    return BitmapUtil.copyBitmapWithFitDensity(BitmapUtil.getBitmap(new BitmapUtil.MyUri(cr, uri), BITMAP_OUTER_SIZE_HALF, BITMAP_OUTER_SIZE_HALF),
                            BITMAP_OUTER_SIZE, BITMAP_OUTER_SIZE); //密度を変更して縦横2倍に
            }
        } catch (Exception e) {
            Log.d("サムネキャッシュ生成に失敗", e);
            return null;
        }
    }

    @Override
    protected boolean isDestroyed(@NonNull Bitmap bitmap) {
        return bitmap.isRecycled();
    }

    //region
    /** シングルトン、初期化順に注意すること。staticは上から評価される */
    private static final @NonNull LiveThumbnailLruCache instance = new LiveThumbnailLruCache();
    private LiveThumbnailLruCache(){}
    @NonNull
    public static LiveThumbnailLruCache getInstance() {
        return instance;
    }
    //endregion

}
