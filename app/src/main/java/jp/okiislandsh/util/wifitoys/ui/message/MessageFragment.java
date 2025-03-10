package jp.okiislandsh.util.wifitoys.ui.message;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.FileUtil.getFileExtension;
import static jp.okiislandsh.library.core.Function.with;
import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.Formatter;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.MutableCreationExtras;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import jp.okiislandsh.library.android.BitmapUtil;
import jp.okiislandsh.library.android.For;
import jp.okiislandsh.library.android.IntentUtil;
import jp.okiislandsh.library.android.MyAppCompatActivity;
import jp.okiislandsh.library.android.async.AsyncUtil;
import jp.okiislandsh.library.android.live.LiveGroup;
import jp.okiislandsh.library.android.live.NonNullLiveData;
import jp.okiislandsh.library.android.mediastore.MediaUtil;
import jp.okiislandsh.library.android.net.service.ipmsg.DL_STATE;
import jp.okiislandsh.library.android.net.service.ipmsg.FILE_ATTR_MODE;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_AttachFile;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Message;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMPrefBool;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMService;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMTCPProgressLogger;
import jp.okiislandsh.library.android.view.live.LifecycleButton;
import jp.okiislandsh.library.android.view.live.LifecycleLinearLayout;
import jp.okiislandsh.library.android.view.nestlist.FileListView;
import jp.okiislandsh.library.core.DateUtil;
import jp.okiislandsh.library.core.Function;
import jp.okiislandsh.library.core.MyUtil;
import jp.okiislandsh.util.wifitoys.AbsDestinyFragmentWithIPMService;
import jp.okiislandsh.util.wifitoys.BuildConfig;
import jp.okiislandsh.util.wifitoys.MainActivity;
import jp.okiislandsh.util.wifitoys.R;
import jp.okiislandsh.util.wifitoys.RecyclerScrollPosition;
import jp.okiislandsh.util.wifitoys.databinding.FragmentMessageBinding;
import jp.okiislandsh.util.wifitoys.view.MessageAdapter;
import jp.okiislandsh.util.wifitoys.view.MultipleTalkView;

/** アドレスに紐づくメッセージ一覧画面 */
public class MessageFragment extends AbsDestinyFragmentWithIPMService implements MultipleTalkView.OnItemClickListener {

    /** ViewBinding(レイアウト参照) */
    private FragmentMessageBinding bind;

    private MessageViewModel vm;

    private @Nullable MessageAdapter messageAdapter = null;

    private static final int COLOR_LIST_BACK = 0xa0ffffff;

    /** bindDataに使用するワーカスレッド */
    private static final @NonNull HandlerThread workerThread = new HandlerThread("MessageFragment WorkerThread");
    private static final @NonNull Handler workerHandler;
    static {
        workerThread.start();
        //workerHandler = new Handler(workerThread.getLooper());
        workerHandler = MyUtil.getQuietly(() -> new Handler(workerThread.getLooper()), new Handler()); //IDE layout.xmlプレビュー時のレンダリング例外回避。 実際にはnew Handler()が実行されるとWorkerThreadが無いためダメ
    }

    //region ライフサイクル
    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        final long sessionID = MessageFragmentArgs.fromBundle(getArguments()).getSessionID();
        vm = new ViewModelProvider(
                getViewModelStore(), //外部で共有しないなら常にthis.getViewModelStore()
                ViewModelProvider.Factory.from(MessageViewModel.initializer), //ViewModelの初期化に干渉するならInitializerが必要
                //Extrasで引数を渡す
                //memo
                // MutableCreationExtrasを渡せばよいが、Applicationなど特別なKeyを初期化するためにthis.getDefaultViewModelCreationExtras()をベースに必要があった。
                // AndroidDeveloper(https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories?hl=ja#creationextras_custom)では最新のActivityやFragmentを使っている限りApplicationなど必要なものは勝手に追加されると書いてあった。うそつき。
                with(new MutableCreationExtras(getDefaultViewModelCreationExtras()), extras-> extras.set(MessageViewModel.KEY_SESSION_ID, sessionID))
        ).get( //positionをkeyに含めることでViewModelStore内で識別が可能になる。KeyのデフォルトはViewModel.Class.CanonicalNameが使用される。
                getClass().getCanonicalName()+sessionID,
                MessageViewModel.class
        );
        //ViewBindingインフレート
        bind = FragmentMessageBinding.inflate(inflater, container, false);

        return bind.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm.liveAddress.observe(getViewLifecycleOwner(), address->{ //アドレス情報が取得できた
            //ツールバー設定
            setToolbarTitle(address==null ? getString(R.string.menu_messages) : address.getDisplayName());
            //背景色
            if(address!=null)bind.getRoot().setBackgroundColor(address.hashColor());
            //オフラインの時の使用可否
            if(address!=null && address.isOnline()) {
                bind.fabNewMail.setVisibility(View.VISIBLE);
                bind.fabAttachFile.setVisibility(View.VISIBLE);
                bind.fabAttachDirectory.setVisibility(View.VISIBLE);
            }else{
                bind.fabNewMail.setVisibility(View.GONE);
                bind.fabAttachFile.setVisibility(View.GONE);
                bind.fabAttachDirectory.setVisibility(View.GONE);
            }
        });

        final @NonNull Handler handler = new Handler();

        //アダプタを初期化
        messageAdapter = new MessageAdapter(requireContext(), vm.liveAddress, index -> Objects.requireNonNull(vm.liveMessageIDList.getValue()).get(index), (isUpdate, holder, position, messageID) -> {
            //ViewHolder.TagのメッセージIDの変更があるか？isUpdate = isNotifyDataSetChanged
            if(isUpdate) return; //自律的に各Viewが更新するため何もしなくていい

            //既読
            requireReceiveService(receiveService-> requireAddress(address-> receiveService.showedMessage(address, messageID), ()->{}), ()->{});

            //ユーザ操作イベント
            holder.itemView.setOnItemClickListener(this);

            //Workerスレッドでデータ取得
            workerHandler.postAtFrontOfQueue(() -> { //データアクセスはワーカースレッドで、postAtFrontOfQueue = LIFO
                final @NonNull List<Integer> liveFiles = vm.db.publicDao().getAttachFileIDs(messageID);
                //メインスレッドでデータ反映
                handler.post(() -> {
                    //既にフレームアウトした？
                    if(!messageID.equals(holder.itemView.getTag()))return;
                    //メッセージIDとファイルIDリストでViewHolderをリセット
                    holder.itemView.reset(messageID, liveFiles);
                });
            });
        }, () -> MyUtil.requireNonNull(vm.liveMessageIDList.getValue(), List::size, 0) );

        final @NonNull LinearLayoutManager llm = new LinearLayoutManager(requireContext());
        bind.messageList.setLayoutManager(llm);
        bind.messageList.setAdapter(messageAdapter);
        bind.messageList.setBackgroundColor(COLOR_LIST_BACK);

        vm.liveMessageIDList.observe(getViewLifecycleOwner(), list->messageAdapter.notifyDataSetChanged());

        //スクロール位置の保存
        bind.messageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                try {
                    vm.liveScrollPosition.postValue(RecyclerScrollPosition.getScrollPosition(bind.messageList, llm));
                } catch (Exception e) {
                    Log.e("スクロール位置の保存に失敗", e);
                }
            }
        });

        //初期スクロール位置の復元
        final @Nullable RecyclerScrollPosition initialScrollPosition = vm.liveScrollPosition.getValue();
        if (initialScrollPosition != null) {
            llm.scrollToPositionWithOffset(initialScrollPosition.firstVisiblePositionIndex, initialScrollPosition.positionOffset);
        }
        //新規メッセージボタン
        bind.fabNewMail.setOnClickListener(v -> {
            final @NonNull EditText editText = new EditText(requireContext());
            editText.setHint(isJa("メッセージ", "Message"));
            new AlertDialog.Builder(requireContext())
                    .setView(editText)
                    .setPositiveButton(R.string.button_send_msg, (dialog, which) -> {
                        requireReceiveService(receiveService->{
                            requireAddress(address->{
                                //TODO:相手がオフラインならコールバックで処理する
                                // post系は全部コールバック
                                if(receiveService.postIPMMessage(address, editText.getText().toString()) ) { //成功
                                    showToastS(editText.getText()+BR+getString(R.string.msg_send));
                                }else{ //失敗
                                    showToastS("送信に失敗。May be Offline");
                                }
                            });
                        });
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
        /*//ロングクリックでバージョン情報取得
        bind.fabNewMail.setOnLongClickListener(v -> {
            requireReceiveService(receiveService->{
                requireAddress(address->{
                    receiveService.postGetVersionInfo(address);
                    showToastS("Get Version Info.");
                });
            });
            return true;
        });*/
        //TODO:Intent.ACTION_OPEN_DOCUMENT_TREEでSDアクセス許可を得る？現状SDが見えないため調査する事。

        //添付
        bind.fabAttachFile.setOnClickListener(v-> attachFileLauncher.launch(new String[]{"*/*"}));

        //ディレクトリ添付
        bind.fabAttachDirectory.setOnClickListener(v-> attachDirectoryLauncher.launch(null));

        //起動から2秒後にメッセージ受信通知をクリア、現状メッセージ受信通知は単一の通知IDを使っているため細かい制御はできない
        //ホーム画面とメッセージ画面を表示した時だけ消す。通知にメッセージIDみたいのがあったり、メッセージの受信音を通知音にたよっていなければ他の方法がよい
        AsyncUtil.postMainThread(()->requireReceiveService(IPMService::clearMessageNotification), 2000);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuProvider.super.onPrepareMenu(menu); //一応
                menu.add(0, R.string.action_clear_messages, 0, R.string.action_clear_messages);

                final @Nullable IPMEntity_Address address = vm.liveAddress.getValue();
                if(address!=null && address.isOnline()) menu.add(0, R.string.action_get_version_info, 1, R.string.action_get_version_info);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.string.action_clear_messages) {
                    requireReceiveService(service->service.deleteMessageAndFile(vm.getSessionID()));
                    return true;
                } else if (itemId == R.string.action_get_version_info) {
                    final @Nullable IPMEntity_Address address = vm.liveAddress.getValue();
                    if (address == null) {
                        showToastS("No found SessionID error.");
                    }else if(!address.isOnline()){
                        showToastS("User is offline.");
                    } else {
                        requireReceiveService(receiveService->{
                            receiveService.postGetVersionInfo(address);
                            showToastL("GET VERSION INFO");
                        });
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); //Fragmentでaddする時の作法

    }

    private void requireAddress(@NonNull Function.voidNonNull<IPMEntity_Address> fn){
        requireAddress(fn, null);
    }

    private void requireAddress(@NonNull Function.voidNonNull<IPMEntity_Address> fn, @Nullable Runnable noAddress){
        final @Nullable IPMEntity_Address address = vm.liveAddress.getValue();
        if(address==null){
            if(noAddress==null) {
                showToastS("Error! No found current address.");
            }else{
                noAddress.run();
            }
        }else {
            fn.run(address);
        }
    }

    @Override
    public void onMessageClick(@NonNull IPMEntity_Address address, @NonNull IPMEntity_Message message) {

        if(receiveService()==null) {
            showToastS("Not connected IPMService...");
        }else {
            new AlertDialog.Builder(requireContext())
                    //TODO:ダイアログがダサい、現状左側に縦並びで返信と共有が表示され、右下にキャンセルボタンが表示される。ダイアログをある程度小さくしてコンテキストメニューのような感じにする
                    .setItems(new String[]{getString(R.string.action_reply), getString(R.string.action_share)}, (dialog, which) -> { //TODO:翻訳
                        switch (which){
                            case 0: //返信
                                final @NonNull EditText editText = new EditText(requireContext());
                                editText.setBackgroundColor(Color.YELLOW);
                                editText.setText(BR+BR+address.getDisplayName()+ DateUtil.toEasyString(message.date)+BR+"> "+message.msg.replace(BR, BR+"> "));
                                new AlertDialog.Builder(requireContext())
                                        .setView(editText)
                                        .setPositiveButton(R.string.button_send_msg, (dialog2, which2) -> {
                                            requireReceiveService(receiveService->{
                                                if (receiveService.postIPMMessage(address, editText.getText().toString())) { //成功
                                                    showToastS(editText.getText() + BR + getString(R.string.msg_send));
                                                } else { //失敗
                                                    showToastS("May be offline...");
                                                }
                                            });
                                        })
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .show();
                                break;
                            case 1: //共有
                                if(!IntentUtil.sendMessage(requireContext(), message.msg, null, getString(jp.okiislandsh.library.android.R.string.intent_share))){
                                    showToastS("Not found application to share text.");
                                }
                                break;
                            default:
                                //
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

    }

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    public void onUriClick(@NonNull IPMEntity_Address a, @NonNull IPMEntity_Message m, @NonNull IPMEntity_AttachFile f) {

        final @Nullable IPMService receiveService = receiveService();
        if(f.uri==null){ //ダウンロードが完了していない
            if(m.owner) { //自分が送ったのにUriが無いなんてありえない
                showToastS("Illegal State Error. uri==null。たぶんバグ。");
            }else if(receiveService==null){
                showToastS("Not connected IPMService...");
            }else if(!a.isOnline()){
                showToastS("May be offline...");
            }else{
                if (receiveService.containsDownloadQueue(a, m, f)) { //ダウンロード中
                    //プログレスを表示
                    onUriLongClick(a, m, f);
                } else { //ダウンロード中ではない
                    if(f.releaseMark) { //リリース済み
                        showToastS("Released file.");
                    }else{ //リリースされていない
                        //保存確認ダイアログ
                        // 保存先 Download or InApp
                        // 書込権限 Granted or Denied
                        // [開始] [権限取得] [Cancel]
                        //memo:IPMServiceはprefをチェックしてダウンロード先を決定しているため、画面上のダイアログで指示された保存先に応じてPrefを書き換える必要がある
                        final @NonNull AtomicReference<Dialog> dialogRef = new AtomicReference<>();
                        final @NonNull NonNullLiveData<Boolean> livePrefExternal = IPMPrefBool.ENABLE_DOWNLOAD_TO_EXTERNAL.getLive(requireContext());
                        //final boolean requireWritePermission_IfSaveToExternal = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
                        final @NonNull String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                        final @Nullable LiveData<Void> livePermissionNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? MyAppCompatActivity.getLivePermissionNotifier(permission) : null;
                        //権限ラベル
                        final @Nullable TextView textPermissionState = livePermissionNotify!=null ? newText("だみー", newParamsMW()) : null;
                        //開始ボタン
                        final @NonNull LifecycleButton buttonDownload = newTextStyleButton(newSizeSpan(isJa("開始", "Start"), 1.5f), newParamsWW(), v-> {
                            //スイッチと権限のバリデータ
                            //Q未満で外部ストレージで
                            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !IPMPrefBool.ENABLE_DOWNLOAD_TO_EXTERNAL.get(requireContext())){
                                //権限が無い場合
                                if(!checkSelfPermission(permission)) {
                                    showToastL(isJa("書込み権限がありません。", "Require write permission."));
                                    ((MainActivity) requireActivity()).permissionsLauncher.launch(new String[]{permission});
                                    return;
                                }
                            }
                            //ダウンロード開始
                            requireReceiveService(receiveService2->{
                                if(!receiveService2.requestDownloadQueue(a, m, f)){
                                    showToastS("Download request filed.");
                                }
                            });
                            dialogRef.get().dismiss();
                        });
                        buttonDownload.observeAndCall(new LiveGroup(livePrefExternal, livePermissionNotify),
                                unused -> buttonDownload.setEnabled(
                                        Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT || //Q以降で権限不要
                                        !IPMPrefBool.ENABLE_DOWNLOAD_TO_EXTERNAL.get(requireContext()) || //アプリ内ストレージなら権限不要
                                        checkSelfPermission(permission) //権限を取得していればOK
                                ));
                        //権限取得ボタン
                        final @Nullable LifecycleButton buttonPermission = livePermissionNotify!=null ? newTextStyleButton(newSizeSpan(isJa("権限取得", "GrantPermission"), 1.5f), newParamsWW(), v-> {
                            //権限取得
                            if(IPMPrefBool.ENABLE_DOWNLOAD_TO_EXTERNAL.get(requireContext()) && !checkSelfPermission(permission)){
                                ((MainActivity) requireActivity()).permissionsLauncher.launch(new String[]{permission});
                            }else{
                                showToastS("bug？");
                            }
                        }) : null;
                        //キャンセル取得ボタン
                        final @NonNull LifecycleButton buttonCancel = newTextStyleButton(newSizeSpan(isJa("キャンセル", "Cancel"), 1.5f), newParamsWW(), v-> dialogRef.get().dismiss());

                        //保存先トグル
                        final @NonNull LifecycleButton switchSaveTo = newTextStyleButton("だみー", newParamsWW(),
                                v-> IPMPrefBool.ENABLE_DOWNLOAD_TO_EXTERNAL.toggle(requireContext()));
                        switchSaveTo.observeAndCall(livePrefExternal, b-> {
                            //ラベル
                            switchSaveTo.setText(newSizeSpan(isJa("保存先:", "Save to: ") + (b ? "Download Directory" : "In app"), 1.5f));
                            //書き込み権限表示可否
                            if(livePermissionNotify!=null) {
                                setVisible(textPermissionState, b);
                                final boolean isGreen = checkSelfPermission(permission);
                                setVisibleGone(buttonPermission, b && !isGreen);
                            }
                        });
                        //保存先トグルのLifecycleを利用してPermissionラベルを変更する
                        if(livePermissionNotify!=null) {
                            switchSaveTo.observeAndCall(livePermissionNotify, unused -> {
                                final boolean isGreen = checkSelfPermission(permission);
                                textPermissionState.setText(newSizeSpan(isJa("書込権限:", "WritePermission:") + (isGreen ? "OK" : "NG"), 1.5f));
                                setVisibleGone(buttonPermission, !isGreen);
                            });
                        }

                        //ダイアログ表示
                        final @NonNull Dialog dialog = newDialogXClose(getString(R.string.msg_want_to_start_download), "x",
                                newLinearLayout(LinearLayout.VERTICAL, newParamsMW(), switchSaveTo, textPermissionState),
                                newLinearLayout(LinearLayout.HORIZONTAL, newParamsMW(), buttonDownload, buttonPermission, buttonCancel),
                                jp.okiislandsh.library.android.R.style.Size80Dialog,
                                0x88FFFFFF, null);
                        dialogRef.set(dialog);
                        dialog.show();
                    }
                }
            }
        }else{ //アップロードもしくはダウンロード済み
            //memo フォルダの共有はうまくいかない "resource/folder"や"vnd.android.document/directory"を指定しても対応アプリが見つからないし、*/*で共有しても開けない
            //memo Android 8.1 Galaxyで成功
            try {
                final boolean isDirectory = (FILE_ATTR_MODE.IPMSG_FILE_DIR.is(f.fileAttribute));
                final @Nullable File file = BitmapUtil.uriIsFileScheme(f.uri);
                if(file!=null && file.isDirectory()){
                    try{
                        FileListView.showFileListDialog(requireContext(), isJa("DIR転送ファイル", "Transferred DIR"), null,
                                Objects.requireNonNull(file.getParentFile()), file, null,
                                null, fileListView -> fileListView.callback.set(new FileListView.NestCallback() {
                                    @Override
                                    public Context getContext() {
                                        return MessageFragment.this.requireContext();
                                    }
                                    @Override
                                    public void onItemClicked(@NonNull View view, @NonNull File file) {
                                        final @Nullable String extension = getFileExtension(file.getName(), false);
                                        final @NonNull String mimeType = MyUtil.nvl(extension==null ? null : MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase()), "*/*");
                                        final @NonNull Uri shareUri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".FileProvider", file);
                                        final @NonNull Intent openIntent = IntentUtil.buildOpen(shareUri, mimeType);
                                        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        if (!IntentUtil.startActivity(requireContext(), openIntent, file.getName()+"\t"+mimeType)) {
                                            //失敗
                                            showToastS("Can't open uri." + BR + file.getName() + BR + mimeType);
                                        }
                                    }
                                    @Override
                                    public void onCreateMenu(@NonNull ActionMode mode, @NonNull Menu menu, @NonNull File file) {
                                        onCreateDefaultContextMenu(fileListView, menu, file, (item)->mode.finish());
                                    }
                                    @Override
                                    public void onCreateMenu(@NonNull ActionMode mode, @NonNull Menu menu, @NonNull List<File> files) {
                                        onCreateDefaultContextMenu(fileListView, menu, files, (item)->mode.finish());
                                    }
                                }), true, true, true);
                    }catch (Exception e){
                        showToastL("showFileListDialog failed.", e);
                    }
                }else{
                    final @Nullable String extension = getFileExtension(f.fileName, false);
                    final @NonNull String mimeType = (isDirectory ? "resource/folder" : MyUtil.nvl(extension==null ? null : MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase()), "*/*"));
                    final @NonNull Uri shareUri;
                    if (file == null) {
                        shareUri = f.uri;
                    } else {
                        shareUri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".FileProvider", file);
                    }
                    final @NonNull Intent openIntent = IntentUtil.buildOpen(shareUri, mimeType);
                    openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (!IntentUtil.startActivity(requireContext(), openIntent, null)) {
                        //失敗
                        showToastS("Can't open uri." + BR + f.fileName + BR + mimeType);
                    }
                }
            }catch (Exception e){
                Log.w("UriからIntentを飛ばしてファイルを開くのに失敗。"+BR+f.uri, e);
                showToastL(isJa("ファイルオープンに失敗", "Failed to Open")+BR+f.uri+BR+e);
            }
        }

    }

    @Override
    public boolean onUriLongClick(@NonNull IPMEntity_Address a, @NonNull IPMEntity_Message m, @NonNull IPMEntity_AttachFile f) {
        final @Nullable FILE_ATTR_MODE mode = FILE_ATTR_MODE.parse(f.fileAttribute);
        final @Nullable LiveData<IPMTCPProgressLogger.FileProgress> liveProgress = IPMTCPProgressLogger.getLiveData(f.messageID, f.fileID);
        final @NonNull LifecycleLinearLayout view = new LifecycleLinearLayout(requireContext()) {
            final @NonNull TextView textFileMode = addText(MyUtil.requireNonNull(mode, Enum::name, Integer.toHexString(f.fileAttribute)), null, null);
            final @NonNull TextView textFileCompletedInfo = addText(null, null, null);
            final @Nullable TextView textFileSize = (mode!=FILE_ATTR_MODE.IPMSG_FILE_DIR) ? //DIRの時サイズは常に0
                    addText("FileSize=" + Formatter.formatFileSize(requireContext(), f.fileSize), null, null) : null;
            final @NonNull TextView textFileUri = addText(f.uri==null ? null : f.uri.toString(), null, null);

            final @NonNull TextView textProgressDirCount = addText(null, null, null);
            final @NonNull TextView textProgressFileCount = addText(null, null, null);
            final @NonNull TextView textProgressByteSum = addText(null, null, null);
            final @NonNull TextView textProgressSkipCount = addText(null, null, null);
            final @NonNull TextView textProgressLastFile = addText(null, null, null);
            final @NonNull TextView textProgressException = addText(null, null, null);
            { //コンストラクタ
                liveProgress.observe(this, this::onChanged); //問題無い、Groupでオブザーブするだけで、実際のLiveDataは別な参照を使用する
                setOrientation(VERTICAL);

                if(f.uri==null){
                    textFileUri.setVisibility(GONE);
                }
            }

            private void onChanged(@Nullable IPMTCPProgressLogger.FileProgress progress) {
                final @NonNull DL_STATE currentState;
                if(progress==null) {
                    final @Nullable IPMService receiveService = receiveService();
                    final boolean isContainsDownload = (!m.owner && receiveService!=null && receiveService.containsDownloadQueue(a, m, f));
                    textFileCompletedInfo.setText(isContainsDownload ? "Download Queue" :
                            ((f.completedDate==null ? "Not " : "") + (m.owner?"Uploaded":"Downloaded"))
                    );
                    currentState = (isContainsDownload ? DL_STATE.START :
                            (f.completedDate==null ? DL_STATE.NONE : DL_STATE.COMPLETE)
                    );

                    textProgressDirCount.setVisibility(GONE);
                    textProgressFileCount.setVisibility(GONE);
                    textProgressByteSum.setVisibility(GONE);
                    textProgressSkipCount.setVisibility(GONE);
                    textProgressLastFile.setVisibility(GONE);
                }else{
                    textFileCompletedInfo.setText(progress.state.name());
                    currentState = progress.state;

                    textProgressDirCount.setVisibility(VISIBLE);
                    textProgressFileCount.setVisibility(VISIBLE);
                    textProgressByteSum.setVisibility(VISIBLE);
                    textProgressSkipCount.setVisibility(VISIBLE);
                    textProgressLastFile.setVisibility(VISIBLE);

                    textProgressDirCount.setText("DirCount=" + progress.completeDirCount);
                    textProgressFileCount.setText("FileCount=" + progress.completeFileCount);
                    textProgressByteSum.setText("ByteSum=" + Formatter.formatFileSize(requireContext(), progress.byteSum) +
                            (f.fileSize==0 ? "" : " "+String.format("%.2f%%", 100f * progress.byteSum / f.fileSize) ) );
                    textProgressSkipCount.setText("SkipCount=" + progress.skipCount);
                    textProgressLastFile.setText(BR+"ProgressFile=" + progress.lastPartProgress_FileName + BR +
                                    Formatter.formatShortFileSize(requireContext(), progress.lastPartProgress_PartByte) + " / " + Formatter.formatShortFileSize(requireContext(), progress.lastPartProgress_TotalByte) +
                                    (progress.lastPartProgress_TotalByte==0 ? "" : " " + String.format("%.2f%%", 100f * progress.lastPartProgress_PartByte / progress.lastPartProgress_TotalByte)) + BR +
                                    progress.lastPartProgress_Uri
                    );

                }
                //プログレスカラー
                switch (currentState) {
                    case NONE:
                    case START: textFileCompletedInfo.setTextColor(textProgressDirCount.getCurrentTextColor());
                        break;
                    case TRANSFERRING: textFileCompletedInfo.setTextColor(Color.BLUE);
                        break;
                    case COMPLETE: textFileCompletedInfo.setTextColor(Color.GREEN);
                        break;
                    case TIME_OUT:
                    case ERROR: textFileCompletedInfo.setTextColor(Color.RED);
                        break;
                }

                if(progress==null || progress.exception==null) {
                    textProgressException.setVisibility(GONE);
                }else{
                    textProgressException.setVisibility(VISIBLE);
                    textProgressException.setText(progress.exception.toString());
                }
            }
        };
        new AlertDialog.Builder(requireContext())
                .setTitle(f.fileName)
                .setView(view)
                .setNegativeButton(android.R.string.ok, null) //TODO:中止ボタン、再度ダイアログを出す
                .show();
        return true;
    }

    @Override
    public void onDestroyView() {

        //ビューモデルのクリア、フラグメントの流儀
        vm = null;
        bind = null;

        super.onDestroyView();
    }

    //endregion

    //region 添付ファイルのActivityResultContracts

    /** 複数の添付ファイル処理<br/>
     * 起動方法： openMultipleDocumentsLauncher.launch(mimeTypes); */
    public final @NonNull ActivityResultLauncher<String[]> attachFileLauncher = registerForActivityResult(new OpenMultipleDocuments(),
            uriList -> {
                Log.d("attachFileLauncher start uriList.size="+uriList.size());

                final @Nullable IPMEntity_Address address = vm.liveAddress.getValue();
                if(address==null){
                    showToastL("Not found address information");
                    return;
                }

                //パーミッションを永続化する Android8以降でこれが無いとUriを開く際にSecurityExceptionが発生する
                //リクエストにIntent.FLAG_GRANT_PERSISTABLE_URI_PERMISSIONフラグが必要
                final @NonNull ContentResolver cr = requireContext().getContentResolver();
                for (@Nullable Uri uri : uriList) {
                    if(uri!=null) cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                //一応Emptyチェック
                if(!uriList.isEmpty()) { //Uriがあるなら

                    //確認用メッセージ
                    final @NonNull StringBuilder buf = new StringBuilder();
                    for(@NonNull Uri uri: uriList){
                        final @Nullable MediaUtil.MediaDataSet meta = MediaUtil.getMediaDataSet(requireContext().getContentResolver(), uri);
                        if (meta == null) {
                            Log.d("File Not Found. uri = "+ uri);
                        }else{
                            if(0<buf.length()) buf.append(BR);
                            buf.append(meta.displayName)
                                    .append("(")
                                    .append(Formatter.formatFileSize(requireContext(), meta.size))
                                    .append(")");
                        }
                    }
                    final @NonNull String dialogMessage = buf.toString();

                    //ダイアログで送信してもいいか確認
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.msg_want_to_send_file)
                            .setMessage(dialogMessage)
                            .setPositiveButton(R.string.label_send_file, (dialog, which) -> {
                                requireReceiveService(receiveService->{
                                    try {
                                        if (receiveService.postIPMMessage(address, getString(R.string.label_send_file), uriList)) { //成功
                                            showToastS(MyUtil.substring(dialogMessage, 100) + BR + getString(R.string.msg_send));
                                        } else { //失敗
                                            showToastS(isJa("送信に失敗。多分オフライン。", "Error. May be Offline."));
                                        }
                                    } catch (Exception e) {
                                        showToastS("Error", e);
                                    }
                                });
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
            }
    );

    /** 複数のOpenableのActivityResultContract (Intent代替) */
    private static class OpenMultipleDocuments extends ActivityResultContract<String[], List<Uri>>{
        @Override
        public @NonNull Intent createIntent(@NonNull Context context, String[] mimeTypes) {
            final @NonNull Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            intent.addCategory(Intent.CATEGORY_OPENABLE); //ACTION_OPEN_DOCUMENTでのみ使える、openFileDescriptor()できるの限定ってこと

            if(mimeTypes.length==1) {
                intent.setType(mimeTypes[0]);
            }else{
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }

            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); //複数選択可能 see Intent.EXTRA_ALLOW_MULTIPLE
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, null); //初期選択Uriを設定できる
                }*/
            //FLAG_GRANT_READ_URI_PERMISSIONやFLAG_GRANT_WRITE_URI_PERMISSIONと組み合わせると、
            // デバイスの再起動後に URI 権限の付与を保持できます。
            // このフラグは、可能な永続化の許可のみを提供します。受信側アプリケーションは、実際に永続化するために
            // takePersistableUriPermission(URI、int) を呼び出す必要があります。
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true); //ローカルストレージオンリー

            return intent;
        }
        @Override
        public @Nullable SynchronousResult<List<Uri>> getSynchronousResult(@NonNull Context context, String[] input) {
            return super.getSynchronousResult(context, input);
        }
        @Override
        public List<Uri> parseResult(int resultCode, @Nullable Intent intent) {
            Log.d("OpenMultipleDocuments#parseResult resultCode="+resultCode);
            return resultCode!=Activity.RESULT_OK || intent==null ? Collections.emptyList() : getClipListUri(intent);
        }
        private static @NonNull List<Uri> getClipListUri(@NonNull Intent data){
            Log.d("OpenMultipleDocuments#getClipListUri start");

            final @NonNull LinkedHashSet<Uri> resultSet = new LinkedHashSet<>();

            //単一選択の場合
            resultSet.add(data.getData());

            //複数選択の場合
            final @Nullable ClipData clipData = data.getClipData();
            if(clipData!=null && 0<clipData.getItemCount()){
                For.each(clipData, resultSet::add);
            }

            Log.d("OpenMultipleDocuments#getClipListUri resultSet.size="+resultSet.size());
            return new ArrayList<>(resultSet);
        }
    }

    /** ディレクトリの添付ファイル処理<br/>
     * 起動方法： openDirectoryLauncher.launch(null, null); */
    public final @NonNull ActivityResultLauncher<Void> attachDirectoryLauncher = registerForActivityResult(new OpenDirectory(),
            treeUri -> {
                Log.d("attachDirectoryLauncher start treeUri="+treeUri);

                final @Nullable IPMEntity_Address address = vm.liveAddress.getValue();
                if(address==null){
                    showToastL("Not found address information");
                    return;
                }

                final @Nullable DocumentFile documentFile = OpenDirectory.toDocumentFile(requireContext(), treeUri);

                //一応Emptyチェック
                if(documentFile!=null) { //Uriがあるなら

                    //パーミッションを永続化する Android8以降でこれが無いとUriを開く際にSecurityExceptionが発生する
                    //リクエストにIntent.FLAG_GRANT_PERSISTABLE_URI_PERMISSIONフラグが必要
                    requireContext().getContentResolver().takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    //確認用メッセージ
                    final @NonNull StringBuilder buf = new StringBuilder();
                    buf.append(documentFile.getName());
                    if(documentFile.isFile()) {
                        buf.append(BR)
                                .append("(")
                                .append(Formatter.formatFileSize(requireContext(), documentFile.length()))
                                .append(")");
                    }
                    final @NonNull String dialogMessage = buf.toString();

                    //ダイアログで送信してもいいか確認
                    new AlertDialog.Builder(requireContext())
                            .setTitle(documentFile.isDirectory() ? R.string.msg_want_to_send_directory : R.string.msg_want_to_send_file)
                            .setMessage(dialogMessage)
                            .setPositiveButton((documentFile.isDirectory() ? R.string.label_send_directory : R.string.label_send_file), (dialog, which) -> {
                                requireReceiveService(receiveService->{
                                    try {
                                        final @Nullable LinkedList<DocumentFile> tmpList = new LinkedList<>();
                                        tmpList.add(documentFile);
                                        if (receiveService.postMessageAndDocumentFile(address, getString(documentFile.isDirectory() ? R.string.label_send_directory : R.string.label_send_file), tmpList)) { //成功
                                            showToastS(MyUtil.substring(dialogMessage, 100) + BR + getString(R.string.msg_send));
                                        } else { //失敗
                                            showToastS(isJa("送信に失敗。多分オフライン。", "Error. May be Offline."));
                                        }
                                    } catch (Exception e) {
                                        showToastS("Error", e);
                                    }
                                });
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }

            }
    );

    /** 複数のOpenableのActivityResultContract (Intent代替) */
    private static class OpenDirectory extends ActivityResultContract<Void, Uri>{
        @Override
        public @NonNull Intent createIntent(@NonNull Context context, Void unused) {
            final @NonNull Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); //システムUI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //デバイスの再起動後に URI 権限の付与を保持できます。このフラグは、可能な永続化の許可のみを提供します。受信側アプリケーションは、実際に永続化するために takePersistableUriPermission(URI、int) を呼び出す必要があります。
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); //複数選択可能 see Intent.EXTRA_ALLOW_MULTIPLE TODO:たぶんできない
            //FLAG_GRANT_READ_URI_PERMISSIONやFLAG_GRANT_WRITE_URI_PERMISSIONと組み合わせると、
            // デバイスの再起動後に URI 権限の付与を保持できます。
            // このフラグは、可能な永続化の許可のみを提供します。受信側アプリケーションは、実際に永続化するために
            // takePersistableUriPermission(URI、int) を呼び出す必要があります。
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true); //ローカルストレージオンリー
            return intent;
        }
        @Override
        public @Nullable SynchronousResult<Uri> getSynchronousResult(@NonNull Context context, Void unused) {
            return super.getSynchronousResult(context, unused);
        }
        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            return resultCode!=Activity.RESULT_OK || intent==null ? null : intent.getData();
        }
        private static @Nullable DocumentFile toDocumentFile(@NonNull Context context, @Nullable Uri treeUri){
            final @Nullable DocumentFile documentFile = (treeUri==null ? null : DocumentFile.fromTreeUri(context, treeUri) );
            return documentFile;
        }
    }

    //endregion

}