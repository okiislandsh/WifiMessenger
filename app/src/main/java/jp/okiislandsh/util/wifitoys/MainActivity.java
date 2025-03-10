package jp.okiislandsh.util.wifitoys;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;

import java.util.LinkedHashSet;
import java.util.Set;

import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.android.MyAppCompatActivity;
import jp.okiislandsh.library.android.live.NonNullLiveData;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMPrefBool;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMService;
import jp.okiislandsh.library.android.preference.IBooleanPreference;
import jp.okiislandsh.library.core.Function;
import jp.okiislandsh.library.core.MyUtil;
import jp.okiislandsh.library.core.WeakSet;
import jp.okiislandsh.util.wifitoys.databinding.ActivityMainBinding;
import jp.okiislandsh.util.wifitoys.preference.PrefBool;

public class MainActivity extends MyAppCompatActivity implements IPMService.Listener {

    private static final @NonNull LogDB.ILog<CharSequence> Log = LogDB.getStringInstance();

    /** アプリケーションバーパターンと対話するNavigationUIメソッドの構成オプション */
    private AppBarConfiguration mAppBarConfiguration;

    /** 通知クリック時のペンディングインテントに仕込む */
    private static final @NonNull String INTENT_EXTRA_NOTIFICATION_CLICK = "INTENT_EXTRA_NOTIFICATION_CLICK";

    private ActivityMainBinding bind;

    private NonNullLiveData<Boolean> prefDevMode;

    /** メインアクティビティを関連付けてIPMServiceを起動する */
    public static void startForegroundService(){
        final @NonNull Intent intent = new Intent(MyApp.app, MainActivity.class);
        intent.putExtra(INTENT_EXTRA_NOTIFICATION_CLICK, true); //TODO: extraはIntent.equalsの対象に含まれない。問題がないか考える
        final @NonNull PendingIntent pending = PendingIntent.getActivity(MyApp.app, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        IPMService.startForegroundService(MyApp.app, MyApp.app.getString(R.string.app_name), MyApp.app.getString(jp.okiislandsh.library.android.R.string.notification_channel_receive_description), pending);
    }

    /** メインアクティビティを関連付けてIPMServiceを終了する */
    public static void stopForegroundService(){
        IPMService.stopForegroundService(MyApp.app);
    }

    private final @NonNull Set<LiveData<?>> liveSet = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Activityの初回起動時のみ
        if (savedInstanceState == null) {
            //サービス起動、この方式で起動した場合unbindでも削除されないはず
            //複数回呼び出しても問題ないらしい
            //中コスト Android4.1エミュレータで42ms
            startForegroundService();
        }

        Function.voidNonNull<IBooleanPreference> boolFunction = pref->{
            @NonNull LiveData<Boolean> live = pref.getLive(this);
            live.observe(this, value -> Log.d("[Setting]"+pref.getKey() + "=" + value));
            liveSet.add(live);
        };
        for(@NonNull IBooleanPreference pref: PrefBool.values()) boolFunction.run(pref);
        for(@NonNull IBooleanPreference pref: IPMPrefBool.values()) boolFunction.run(pref);

        //ViewBinding 高コスト Android4.1エミュレータで237ms
        bind = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(bind.drawer);

        //ツールバーの設定(androidx.appcompat.widget.Toolbar)
        setSupportActionBar(bind.toolbar); //アクティビティのアクションツールバーとして関連付ける。ナビゲーションメニューとオプションメニューが使用可能になる。
        MyUtil.requireNonNull(getSupportActionBar(), actionBar->actionBar.setDisplayHomeAsUpEnabled(true)); //戻るボタン

        //横向きの時ツールバーを隠す
        /*if(!MyUtil.getNowDeviceOrientationIsPortrait(this)) {
            MyUtil.requireNonNull(getSupportActionBar(), ActionBar::hide);
        }*/

        /*
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG) //スナックバー構築、Toastみたいに画面下部にメッセージを表示する
                .setAction("Action", null).show(); //setActionの第2引数で任意のメッセージクリック時のアクションを設定できる
         */

        //アプリケーションバーを構築する。
        //メニューのトップレベルの宛先となる項目のメニューIDを渡す。
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_archive, R.id.nav_dump, R.id.nav_attach_file,
                R.id.nav_log, R.id.nav_setting, R.id.nav_about)
                .setOpenableLayout(bind.drawer)
                .build();

        //AndroidXライブラリのナビゲーションコントローラオブジェクトを取得する。第2引数はナビゲーションホストのID
        //final @NonNull NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment); //<fragment>
        //final @NonNull NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController(); //<FragmentContainerView>
        final @NonNull NavController navController = getNavController(R.id.nav_host_fragment);

        //ナビゲーションコントローラにアクションバーを設定する。画面の切り替わりとアクションバーのタイトルが連動するようになる。
        //AppCompatActivity.getSupportActionBar()で取得できるアクションバーをセットアップし、NavControllerで使用する。
        //AppBarConfigurationはナビゲーションボタンの表示方法を制御する。
        //navigateUp(NavController, AppBarConfiguration)を呼び出し"アップ"ボタンを処理する。
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        //ナビゲーションコントローラで使用するナビゲーションビューを設定する。
        //メニュー項目が選択されるとonNavDestinationSelected(MenuItem, NavController)が呼び出される。
        //NavigationViewで選択した項目は、宛先が変更されると自動的に更新される。
        //NavigationViewがDrawerLayoutに含まれている場合、メニュー項目の選択と同時にドロワーが閉じる。
        //NavigationViewにBottomSheetBehaviorが関連付けられている場合、メニュー項目を選択すると、下のシートは非表示になる。(BottomSheetダイアログと同様の動作)
        NavigationUI.setupWithNavController(bind.navView, navController);

        //ナビゲーションの遷移を検知するリスナーを設定
        // Toolbarのスクロール同期を制御する、HomeFragmentがモダン系スクロールレイアウトに変わったら不要な制御
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // 遷移先のFragmentのIDを取得
            int destinationId = destination.getId();
            // HomeFragmentの時、Toolbarのスクロールフラグを削除
            final int newScrollFlags = destinationId == R.id.nav_home ?
                    0 :
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS;
            //Toolbarへ反映
            final @NonNull AppBarLayout.LayoutParams toolbarLayoutParams = (AppBarLayout.LayoutParams) bind.toolbar.getLayoutParams();
            if(toolbarLayoutParams.getScrollFlags()!=newScrollFlags) {
                toolbarLayoutParams.setScrollFlags(newScrollFlags);
                bind.toolbar.setLayoutParams(toolbarLayoutParams);
            }
        });

        //開発者モードに応じたドロワーメニューの使用可否
        prefDevMode = PrefBool.DEVELOPER_MODE.getLive(this);
        prefDevMode.observe(this, bool->{
            for(@Nullable MenuItem menuItem: new MenuItem[]{
                    bind.navView.getMenu().findItem(R.id.nav_log),
                    bind.navView.getMenu().findItem(R.id.nav_dump)}){
                if(menuItem!=null) menuItem.setVisible(bool);
            }
        });

        //外部ストレージへ保存する場合、権限リクエスト
        if(IPMPrefBool.ENABLE_DOWNLOAD_TO_EXTERNAL.get(this)) checkPermissionAndRequest();

        //権限 変更検出時のoption menu再構築
        getLivePermissionNotifier(Manifest.permission.WRITE_EXTERNAL_STORAGE).observe(this, unused -> invalidateOptionsMenu());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getLivePermissionNotifier(Manifest.permission.POST_NOTIFICATIONS).observe(this, unused -> {
                if(receiveService!=null){ //サービス通知更新
                    receiveService.updateNotification();
                }
            });
        }

        //メニュー
        addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                menu.clear();
                //パーミッション実行時付与
                if(!checkPermission()){
                    menu.add(1, R.string.action_grant_permissions, 1001, R.string.action_grant_permissions);
                }

                MenuProvider.super.onPrepareMenu(menu); //一応
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                final int itemId = item.getItemId();
                if (itemId == R.string.action_grant_permissions) {
                    checkPermissionAndRequest();
                }
                return false;
            }
        });

        //TODO:受信通知タップでメッセージ画面を開きたい
        // 任意のFragmentへ遷移する場合
        // navController.navigate(HomeFragmentDirections.actionNavHomeToNavMessage().setSessionID(あいてのID));
        // この場合、バックスタックはstartDestination (HomeFragment)の上に遷移先が積まれる

        /*int action = Directions.confirmationAction(amount)
        v.findNavController().navigate(action)*/
        /*//TODO:ブロードキャストインテントでフラグメントを切り替えた方がスマート
        final @Nullable Intent intent = getIntent();
        if(intent==null || !intent.getBooleanExtra(INTENT_EXTRA_NOTIFICATION_CLICK, false)){ //ランチャー起動
            if(savedInstanceState==null && 0<IPMessageDB.getInstance().getUnShowDisplayCount()){ //初回onCreateかつ未読
                Log.d("ランチャー起動、未読あり、アーカイブフラグメントをリロード");
                setting.clearArchiveCurrentUserID(); //カレントアドレスをクリアして
                AsyncUtil.postMainThreadQuietly(()->loadDestination(R.id.nav_archive)); //アーカイブフラグメント起動
            }else {
                Log.d("ランチャー起動、通常通りアクティビティを開く");
                setting.clearArchiveCurrentUserID(); //アーカイブフラグメントの起動引数をクリアする
            }
        }else{ //インテント起動
            setIntent(null); //起動引数クリア
            //TODO:起動引数をもっと詳細にカスタマイズできるようにする
            if(0<IPMessageDB.getInstance().getUnShowDisplayCount()){ //初回onCreateかつ未読
                Log.d("インテント起動、未読あり、アーカイブフラグメントをリロード");
                setting.clearArchiveCurrentUserID(); //カレントアドレスをクリアして
                AsyncUtil.postMainThreadQuietly(()->loadDestination(R.id.nav_archive)); //アーカイブフラグメント起動
            }else {
                Log.d("インテント起動、特に考慮すべき事項が無いため、通常通りアクティビティを開く");
                setting.clearArchiveCurrentUserID(); //アーカイブフラグメントの起動引数をクリアする
            }
        }*/

    }

    //region サービス接続
    //https://developer.android.com/guide/components/bound-services?hl=ja
    /** mBound=trueの時のみアクセス可能 */
    public @Nullable IPMService receiveService;
    /** Defines callbacks for service binding, passed to bindService() */
    private final @NonNull ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("onServiceConnected("+name+")");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            IPMService.LocalBinder binder = (IPMService.LocalBinder) service;
            receiveService = binder.getService();
            //シャットダウン実装
            receiveService.addListener(MainActivity.this);
            //通知
            notifyLiveServiceNotifySet();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("onServiceDisconnected("+name+")");
            receiveService = null;
            //通知
            notifyLiveServiceNotifySet();
        }
    };
    private final @NonNull WeakSet<MutableLiveData<Void>> liveServiceNotifySet = new WeakSet<>();
    /** 弱参照で保持しているため、生成したLiveDataを強参照で保持する必要があると思う */
    public @NonNull LiveData<Void> newLiveServiceNotify(){
        final @NonNull MutableLiveData<Void> ret = new MutableLiveData<>(null);
        synchronized (liveServiceNotifySet){
            liveServiceNotifySet.add(ret);
        }
        return ret;
    }
    private void notifyLiveServiceNotifySet(){
        synchronized (liveServiceNotifySet) {
            for (@NonNull MutableLiveData<Void> l : liveServiceNotifySet) {
                l.setValue(null);
            }
        }
    }
    //endregion

    @Override
    protected void onRestart() {
        Log.d("restart");
        super.onRestart();
    }

    @Override
    protected void onStart() {
        Log.d("start");
        super.onStart();

        //注意！！画面の再生成の時、onPauseは呼ばれるのにonResumeは呼ばれない
        //サービスに接続する
        doBindService();

    }

    public void doBindService(){
        final @NonNull Intent intent = new Intent(this, IPMService.class);
        bindService(intent, connection, 0); //サービスがない場合は接続を保留
    }

    /** 画面の再生成の時、onPauseは呼ばれるのにonResumeは呼ばれない */
    @Override
    protected void onResume() {
        Log.d("start");
        super.onResume();

        //サービスに接続する
        doBindService();

        Log.d("end");
    }

    @Override
    protected void onPause() {
        Log.d("start");
        super.onPause();

        //サービスから切断する
        doUnbindService();
    }

    protected void doUnbindService(){
        if(receiveService!=null) {
            receiveService.removeListener(this);

            unbindService(connection); //接続カウンタが0になるとサービスが死ぬが、startServiceで起動した場合は死なない
        }
    }

    @Override
    protected void onStop() {
        Log.d("start");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("start");
        super.onDestroy();
    }

    /** フォアグラウンドサービス終了 */
    @Override
    public void serviceStopped() {
        Log.d("start");

    }

    /**
     * ユーザがアクションバーから"アップ"ナビゲーションを選択するたびに呼び出される。<br/>
     * このアクティビティの親が設定(マニフェストまたはエイリアスで)されている場合、"アップ"は自動的に処理される。<br/>
     * たぶん、サポート処理の実装有無を返すのが目的ではなく、アップ処理そのものだと思う。
     * 親を指定するには{@link #getSupportParentActivityIntent()}を参照すること。<br/>
     * @return true アップナビゲーションを処理 / false それ以外.
     */
    @Override
    public boolean onSupportNavigateUp() {
        final @NonNull NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        //たぶん、アップ処理し、成否を返す
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) //NavControllerにアップ処理をさせる。アップ処理が行われたらtrueが返る
                || super.onSupportNavigateUp(); //不明。既定のアップ処理？
    }

    /** ナビゲーションによって表示されている画面をリロードする */
    public void loadDestination(int navID){
        final @NonNull NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment); //コントローラ
        final @Nullable MenuItem item = bind.navView.getMenu().findItem(navID); //MenuItem逆引き
        if (item != null) { //逆引き成功
            NavigationUI.onNavDestinationSelected(item, navController);
            //navController.navigate(resId); 直接navigateを呼び出すとバグる、たぶんフラグメント増殖
        }
    }

    /** ナビゲーションによって表示されている画面をリロードする */
    public void reloadDestination(){
        final @NonNull NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment); //コントローラ
        final @Nullable NavDestination currentDestination = navController.getCurrentDestination(); //現在の画面
        if(currentDestination!=null) {
            final @Nullable MenuItem item = bind.navView.getMenu().findItem(currentDestination.getId()); //MenuItem逆引き
            if (item != null) { //逆引き成功
                NavigationUI.onNavDestinationSelected(item, navController);
                //navController.navigate(resId); 直接navigateを呼び出すとバグる
            }
        }
    }

    /** ナビゲーションによって表示されている画面をリロードする
     * @param matchIDs リロード対象の画面、一致しなければ何もしない
     */
    public void reloadDestination(int... matchIDs){
        final @NonNull NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment); //コントローラ
        final @Nullable NavDestination currentDestination = navController.getCurrentDestination(); //現在の画面
        if(currentDestination!=null) {
            for (int resId : matchIDs) {
                if (currentDestination.getId() == resId) { //現在の画面と引数が一致
                    final @Nullable MenuItem item = bind.navView.getMenu().findItem(resId); //MenuItem逆引き
                    if (item != null) { //逆引き成功
                        NavigationUI.onNavDestinationSelected(item, navController);
                        //navController.navigate(resId); 直接navigateを呼び出すとバグる
                    }
                }
            }
        }
    }

    /** ツールバータイトル変更、フラグメントからの呼び出しを想定 */
    public void pleaseTitleChange(@NonNull String title){
        bind.toolbar.setTitle(title);
    }

    protected @NonNull String[] getDangerousPermissions() {
        // Android 10 (Q) 以降では、アプリが作成したファイルへのアクセスにパーミッションは不要
        // Android 6.0 (M) 以降では、ランタイムパーミッションが必要
        // Android 5.1 (Lollipop MR1) 以前では、インストール時にパーミッションが付与される
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                IPMPrefBool.ENABLE_DOWNLOAD_TO_EXTERNAL.get(this)) {
            return new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, //ダウンロードファイルの保存のため
            };
        }else{
            if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) {
                return new String[]{Manifest.permission.POST_NOTIFICATIONS}; //MediaStore APIを介して読み書きするため権限はいらない
            }else{
                return new String[]{};
            }
        }
    }

    public boolean checkPermission(){
        return checkPermissionAndRequest(false);
    }

    public boolean checkPermissionAndRequest(){
        return checkPermissionAndRequest(true);
    }

    public boolean checkPermissionAndRequest(boolean request){
        return checkPermissionAndRequest(request, getDangerousPermissions(), null, null);
    }

    public boolean checkPermissionAndRequest(boolean request, @NonNull String[] DANGEROUS_PERMISSIONS, @Nullable String title, @Nullable String message){
        if(request) requestPermissions(DANGEROUS_PERMISSIONS, title, message);
        for(@NonNull String p: DANGEROUS_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, p)!= PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

}
