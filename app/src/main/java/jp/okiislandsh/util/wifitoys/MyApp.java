package jp.okiislandsh.util.wifitoys;

import android.app.Application;

public class MyApp extends Application {
    public static MyApp app;

    public MyApp() {
        super();
        app = this;
    }

}
