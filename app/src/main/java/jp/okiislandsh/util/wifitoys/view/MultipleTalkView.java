package jp.okiislandsh.util.wifitoys.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pools;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.Objects;

import jp.okiislandsh.library.android.net.service.ipmsg.IPMDataBase;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_AttachFile;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Message;
import jp.okiislandsh.library.android.view.ViewBuilderFunction;

public class MultipleTalkView extends LinearLayout implements ViewBuilderFunction.OnViewGroup {

    /** TODO:ここで管理すべき？上位コンテナ側で指定すべきでは？ */
    public static final int CHILD_MARGIN_BOTTOM = 20;

    /** 子へ渡す用 */
    private final @NonNull LiveData<IPMEntity_Address> liveAddress;

    /** 1つのTalkMessageView */
    private final @NonNull TalkMessageView talkMessageView;

    /** TalkFileViewのプール。文明の利器。 */
    private static class TalkFileViewPool {
        /** 最大プール数、これ以上生成しrecycleしてもMAXを超えず破棄される */
        private static final int MAX_POOL_SIZE = 10;
        /** スレッドセーフなプールオブジェクト */
        private static final Pools.SynchronizedPool<TalkFileView> sPool = new Pools.SynchronizedPool<>(MAX_POOL_SIZE);
        /** プールから取得または生成 */
        public static @NonNull TalkFileView obtain(@NonNull Context context, @NonNull LiveData<IPMEntity_Address> liveAddress) {
            @Nullable TalkFileView instance = sPool.acquire();
            return (instance != null) ? instance : newTalkFileView(context);
        }
        private static @NonNull TalkFileView newTalkFileView(@NonNull Context context){
            final @NonNull TalkFileView view = new TalkFileView(context);
            final @NonNull LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = CHILD_MARGIN_BOTTOM;
            view.setLayoutParams(params);
            return view;
        }
        /** プールへ戻す */
        public static void recycle(@NonNull TalkFileView view) {
            sPool.release(view);
        }
    }

    protected @Nullable OnItemClickListener listener;
    public interface OnItemClickListener{
        void onMessageClick(@NonNull IPMEntity_Address address, @NonNull IPMEntity_Message  message);
        default void onUriClick(@NonNull IPMEntity_Address address, @NonNull IPMEntity_Message  message, @NonNull IPMEntity_AttachFile file){}
        /** @return true if the callback consumed the long click, false otherwise. */
        default boolean onUriLongClick(@NonNull IPMEntity_Address address, @NonNull IPMEntity_Message  message, @NonNull IPMEntity_AttachFile file){
            return false;
        }
    }
    public void setOnItemClickListener(@Nullable OnItemClickListener listener){
        this.listener = listener;
    }

    public MultipleTalkView(@NonNull Context context, @NonNull LiveData<IPMEntity_Address> liveAddress) {
        super(context);
        this.liveAddress = liveAddress;

        setOrientation(VERTICAL);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setMinimumHeight(CHILD_MARGIN_BOTTOM*2); //大きさが0圧縮されないように

        //メッセージViewをキャッシュ、まだaddしない
        talkMessageView = new TalkMessageView(context);
        talkMessageView.setLayoutParams(setMarginBottom(newParamsMW(null), CHILD_MARGIN_BOTTOM));
        talkMessageView.messageTextView.setOnClickListener(v->{
            final @Nullable IPMEntity_Address address = liveAddress.getValue();
            if(listener!=null && address!=null && talkMessageView.lastLiveData!=null) listener.onMessageClick(address, talkMessageView.lastLiveData);
        });

        //TalkFileViewは自動でプールされる

    }

    private long lastMessageID;
    private @Nullable List<Integer> lastFileIDList;

    public void reset(long messageID, @Nullable List<Integer> fileIDList){
        if(lastMessageID!=messageID || !Objects.equals(lastFileIDList, fileIDList)) {
            lastMessageID = messageID;
            lastFileIDList = fileIDList;

            clearChildView();

            final @NonNull IPMDataBase db = IPMDataBase.getInstance(requireContext());
            final @NonNull LiveData<IPMEntity_Message> liveMessage = db.asyncMessage(messageID);

            //ファイルView
            if (fileIDList != null && !fileIDList.isEmpty()) {
                for (int fileID : fileIDList) {
                    final @NonNull TalkFileView fileView = TalkFileViewPool.obtain(requireContext(), liveAddress);
                    fileView.setLiveData(liveAddress, liveMessage, db.asyncAttachFile(messageID, fileID));

                    fileView.uriView.setOnClickListener(v->{
                        final @Nullable IPMEntity_Address address = liveAddress.getValue();
                        if(listener!=null && address!=null && fileView.lastLiveData!=null &&
                                fileView.lastLiveData.message!=null && fileView.lastLiveData.file!=null) {
                            listener.onUriClick(address, fileView.lastLiveData.message, fileView.lastLiveData.file);
                        }
                    });
                    fileView.uriView.setOnLongClickListener(v-> {
                        final @Nullable IPMEntity_Address address = liveAddress.getValue();
                        return listener!=null && address!=null && fileView.lastLiveData!=null &&
                                fileView.lastLiveData.message!=null && fileView.lastLiveData.file!=null &&
                                listener.onUriLongClick(address, fileView.lastLiveData.message, fileView.lastLiveData.file);
                    });

                    addView(fileView);
                }
            }
            //メッセージView
            talkMessageView.setLiveData(liveAddress, liveMessage);
            addView(talkMessageView);
        }
    }

    protected void clearChildView(){
        talkMessageView.clearLiveData();
        for(int i=0; i<getChildCount(); i++){
            final @NonNull View v = getChildAt(i);
            if(v instanceof TalkMessageView) {
                ((TalkMessageView) v).clearLiveData();
            }else if(v instanceof TalkFileView){
                ((TalkFileView) v).clearLiveData();
                TalkFileViewPool.recycle((TalkFileView) v);
            }
        }
        removeAllViews();
    }

}
