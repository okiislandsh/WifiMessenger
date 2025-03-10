package jp.okiislandsh.util.wifitoys.ui.attachfile;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.FileUtil.getFileExtension;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.Objects;

import jp.okiislandsh.library.android.BitmapUtil;
import jp.okiislandsh.library.android.IntentUtil;
import jp.okiislandsh.library.android.MimeUtil;
import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.SizeUtil;
import jp.okiislandsh.library.android.mediastore.MediaUtil;
import jp.okiislandsh.library.android.net.service.ipmsg.FILE_ATTR_MODE;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_AttachFile;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_AttachFileKeys;
import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.util.wifitoys.AbsDestinyFragmentWithIPMService;
import jp.okiislandsh.util.wifitoys.BuildConfig;
import jp.okiislandsh.util.wifitoys.databinding.FragmentAttachFileBinding;
import jp.okiislandsh.util.wifitoys.view.AbsMyAdapter;
import jp.okiislandsh.util.wifitoys.view.UriView;

public class AttachFileFragment extends AbsDestinyFragmentWithIPMService {

    private FragmentAttachFileBinding bind;
    private AttachFileViewModel vm;

    /** bindDataに使用するワーカスレッド */
    private static final @NonNull HandlerThread workerThread = new HandlerThread("MessageFragment WorkerThread");
    private static final @NonNull Handler workerHandler;
    static {
        workerThread.start();
        //workerHandler = new Handler(workerThread.getLooper());
        workerHandler = MyUtil.getQuietly(() -> new Handler(workerThread.getLooper()), new Handler()); //IDE layout.xmlプレビュー時のレンダリング例外回避。 実際にはnew Handler()が実行されるとWorkerThreadが無いためダメ
    }

    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        vm = new ViewModelProvider(this).get(AttachFileViewModel.class);

        bind = FragmentAttachFileBinding.inflate(inflater, container, false);

        final int SPAN_COUNT = 3;
        final @NonNull Point realSize = MyUtil.getRealSize(requireActivity());

        final @NonNull Handler handler = new Handler();

        adapter = new AttachFileAdapter<>(requireContext(), (int)(realSize.x/SPAN_COUNT*1.3f), position -> Objects.requireNonNull(vm.liveKeyList.getValue()).get(position), (isUpdate, holder, position, key) -> {
            //Workerスレッドでデータ取得
            workerHandler.postAtFrontOfQueue(() -> { //データアクセスはワーカースレッドで、postAtFrontOfQueue = LIFO
                final @Nullable IPMEntity_AttachFile file = vm.db.publicDao().getAttachFile(key.messageID, key.fileID); //データ取得
                handler.post(() -> { //画面に表示
                    if(!key.equals(holder.itemView.getTag()))return; //既にフレームアウトした？

                    if (file == null) { //取得できなかった、レコード削除？
                        holder.itemView.resetNoData();
                        holder.itemView.setOnClickListener(null);
                        holder.itemView.setOnLongClickListener(null);
                    }else{ //データを取得できた
                        holder.itemView.reset(file);
                        holder.itemView.setOnClickListener(v->onItemClick(file));
                        holder.itemView.setOnLongClickListener(v->onItemLongClick(file));
                    }
                });
            });
        }, () -> MyUtil.requireNonNull(vm.liveKeyList.getValue(), List::size, 0) );

        vm.liveKeyList.observe(getViewLifecycleOwner(), list->adapter.notifyDataSetChanged());

        bind.fileList.setLayoutManager(new GridLayoutManager(requireContext(), SPAN_COUNT)); //TODO:PrefIntを作って列数を保存
        bind.fileList.setAdapter(adapter);

        return bind.getRoot();
    }

    private void onItemClick(@NonNull IPMEntity_AttachFile file){
        final @NonNull Context context = requireContext();
        if(file.uri==null){
            if(file.completedDate==null) {
                showToastL("Not downloaded." + BR + file.fileName);
            }else{
                showToastL("No found Uri error." + BR + file.fileName);
            }
        }else {
            try {
                final @Nullable File f = BitmapUtil.uriIsFileScheme(file.uri);
                final @NonNull Uri shareUri;
                final @NonNull String strMime;
                if (f != null) {
                    shareUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".FileProvider", f);
                    if (f.isDirectory()) {
                        strMime = "resource/folder";
                    } else {
                        final @Nullable String fileExtension = getFileExtension(file.fileName, false);
                        if (fileExtension != null) { //拡張子あり
                            final @Nullable MimeUtil.SUPPORTED_IMAGE supportedImage = MimeUtil.SUPPORTED_IMAGE.valueOfExtension(fileExtension);
                            if (supportedImage == null) {
                                final @Nullable MimeUtil.SUPPORTED_VIDEO supportedVideo = MimeUtil.SUPPORTED_VIDEO.valueOfExtension(fileExtension);
                                strMime = (supportedVideo == null ? "*/*" : supportedVideo.getMime1st());
                            } else {
                                strMime = supportedImage.getMime1st();
                            }
                        } else {
                            strMime = "*/*";
                        }
                    }
                } else {
                    shareUri = file.uri;
                    final @Nullable MediaUtil.MediaDataSet meta = MediaUtil.getMediaDataSet(context.getContentResolver(), shareUri);
                    strMime = (meta == null ? "*/*" : meta.mimeType); //Uriから取り出したMIME
                    //memo、通常のContentResolverでアクセスするUriに対してGrantPermissionできるかどうかはContentResolverの実装依存
                }
                final @NonNull Intent openIntent = IntentUtil.buildOpen(shareUri, strMime);
                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (!IntentUtil.startActivity(context, openIntent, "open")) {
                    //失敗
                    Toast.makeText(context, "No found viewer" + BR + file.fileName + BR + strMime, Toast.LENGTH_LONG).show();
                }
            }catch (Exception e){
                Log.e("Uri onClick error!", e);
                showToastL("Error..."+BR+e.getLocalizedMessage());
            }
        }
    }
    private boolean onItemLongClick(@NonNull IPMEntity_AttachFile file){
        new AlertDialog.Builder(requireContext())
                .setTitle("File Info")
                .setMessage(file.toString())
                .show();
        return true;
    }

    private AttachFileAdapter<IPMEntity_AttachFileKeys> adapter;

    public static class AttachFileAdapter<KEY> extends AbsMyAdapter<KEY, AttachFileAdapter.ViewHolder<KEY>> {
        final int itemHeight;
        public AttachFileAdapter(@NonNull Context context, int itemHeight, @NonNull AbsMyAdapter.OnBindDataKey<KEY> callbackBindDataKey, @NonNull AbsMyAdapter.OnBindData<KEY, ViewHolder<KEY>> callbackBindData, @NonNull AbsMyAdapter.OnGetItemCount callbackGetItemCount) {
            super(context, callbackBindDataKey, callbackBindData, callbackGetItemCount);
            this.itemHeight = itemHeight;
        }

        @Override
        public @NonNull ViewHolder<KEY> onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup parent, int viewType) {
            final @NonNull AttachFileView view = new AttachFileView(context);
            view.setPadding(8, 2, 8 , 2);
            final @NonNull RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight);
            params.setMargins(8,8,8,8);
            view.setLayoutParams(params);
            return new ViewHolder<>(view);
        }

        public static class ViewHolder<KEY> extends AbsMyAdapter.MyViewHolder<KEY>{
            /** superのitemViewを上書き、いちいちキャストしなくて済む */
            final @NonNull AttachFileView itemView;
            ViewHolder(@NonNull AttachFileView itemView) {
                super(itemView);
                this.itemView = itemView;
            }
            @Override
            public void resetViewHolder(@NonNull KEY key) {
                itemView.reset();
            }
        }
    }

    public static class AttachFileView extends CardView implements ViewBuilderFunction.OnViewGroup {
        @Nullable IPMEntity_AttachFile file;

        /** root直下 */
        final @NonNull LinearLayout container;

        /** root直下 */
        final @NonNull ImageView imageView;


        /** container直下 */
        final @NonNull UriView uriView;
        /** container直下 */
        final @NonNull TextView textView;

        public AttachFileView(Context context) {
            super(context);

            //初期文字サイズを基準に諸々の大きさを決定する
            final int charSizePx = SizeUtil.TEXT_SIZE.def.px(context);
            //final int char15SizePx = (int)(charSizePx*1.5f);
            //final int char2SizePx = charSizePx*2;
            setMinimumHeight(charSizePx*4);

            container = addLinearLayout(LinearLayout.VERTICAL, null);

            imageView = new ImageView(context);
            imageView.setLayoutParams(new LayoutParams(32, 32, Gravity.RIGHT | Gravity.TOP));
            imageView.setBackground(newTintDrawable(jp.okiislandsh.library.android.R.drawable.white_circle, Color.WHITE, false));
            addView(imageView);

            uriView = new UriView(context);
            uriView.setLayoutParams(newParamsM0(7f));
            container.addView(uriView);

            container.addView(textView = newTextRelative(null, newParamsM0(3f), .8f));

        }
        /** 中身を瞬時にクリアする */
        public void reset(){
            file = null;
            imageView.setImageBitmap(null);
            uriView.detachUri();
            textView.setText("");
        }
        /** DBからデータを取得できなかった */
        public void resetNoData(){
            file = null;
            imageView.setImageBitmap(null);
            uriView.setText("No found Uri error"); //レコードが無いError
            textView.setText("");
        }
        /** DBから取得したデータを表示する */
        public void reset(@NonNull IPMEntity_AttachFile file){
            this.file = file;

            //送ったファイルか受信したファイルか
            imageView.setImageDrawable(newTintDrawable(android.R.drawable.stat_sys_download_done,
                (file.uri==null || BitmapUtil.uriIsFileScheme(file.uri)!=null) ? Color.BLUE : Color.GREEN,
                    false)); //TODO:IPMEntity_Addressから ownerを取得する

            //サムネ展開
            if(file.uri==null) { //未ダウンロード
                if(file.completedDate==null) {
                    uriView.setText("Not Downloaded");
                }else{
                    uriView.setText("No found Uri error");
                }
            }else{ //ダウンロード済み
                //FileTypeにより適切なViewを構築する
                final @Nullable FILE_ATTR_MODE mode = FILE_ATTR_MODE.parse(file.fileAttribute);
                if (mode == FILE_ATTR_MODE.IPMSG_FILE_REGULAR || mode == FILE_ATTR_MODE.IPMSG_FILE_CLIPBOARD) { //単一ファイル
                    final @Nullable String extension = getFileExtension(file.fileName, true);
                    if (MimeUtil.SUPPORTED_IMAGE.existExtension(extension) ||
                            MimeUtil.SUPPORTED_VIDEO.existExtension(extension)) {
                        uriView.setMediaUri(file.uri);
                    }else {
                        uriView.setUnknownFileView(file.uri, extension==null ? "?" : extension);
                    }
                } else { //ディレクトリ
                    uriView.setDirUri(file.uri);
                }
            }

            //ラベル
            textView.setText(file.fileName);
        }
    }


}