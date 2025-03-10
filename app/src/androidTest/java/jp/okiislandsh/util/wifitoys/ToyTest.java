package jp.okiislandsh.util.wifitoys;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import jp.okiislandsh.library.android.async.AsyncUtil;

/**
 * 実機なら動く
 */
@RunWith(AndroidJUnit4.class)
public class ToyTest {
    private static final @NonNull String LOGTAG = "ToyTest";

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("jp.okiislandsh.util.wifitoys", appContext.getPackageName());
    }

    private static class HogeHandlerThread extends HandlerThread{
        public HogeHandlerThread(String name) {
            super(name);
        }

        @Override
        public synchronized void start() {
            super.start();
            handler = new Handler(getLooper());
        }
        private Handler handler;
        public void testPost(@NonNull Runnable runnable){
            handler.post(runnable);
            handler.post(()->AsyncUtil.postMainThreadQuietly(runnable::run));
        }
    }

    @Test
    public void handlerTest() {
        Log.d(LOGTAG, "メインスレッド開始");
        HogeHandlerThread thread = new HogeHandlerThread("hogehogehogehoge");
        thread.start();
        Log.d(LOGTAG, "メインスレッド----サブスレッド起動");
        thread.testPost(() -> Log.d(LOGTAG, "やっほい1"));
        thread.testPost(() -> Log.d(LOGTAG, "やっほい2"));
        thread.testPost(() -> Log.d(LOGTAG, "やっほい3"));
        thread.testPost(() -> Log.d(LOGTAG, "やっほい4"));
        Log.d(LOGTAG, "メインスレッド----サブスレッドポスト");
        try {
            Thread.sleep(1000);
        }catch (InterruptedException e){
            Log.e(LOGTAG, "", e);
        }
        Log.d(LOGTAG, "メインスレッド終了");
    }



    @Test
    public void jsonTest() {
        try{
            final @NonNull String str = "{'a': 1}";
            JSONObject json = new JSONObject(str);
            Log.d(LOGTAG, str);
            Log.d(LOGTAG, json.toString());

        } catch (JSONException e) {
            Log.w("JSONエラー", e);
        }
    }

}