package jp.okiislandsh.util.wifitoys.view;

import static jp.okiislandsh.library.core.FileUtil.getFileExtension;

import android.content.Context;
import android.net.Uri;
import android.text.format.Formatter;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.Objects;

import jp.okiislandsh.library.android.MimeUtil;
import jp.okiislandsh.library.android.live.LiveLive;
import jp.okiislandsh.library.android.net.service.ipmsg.FILE_ATTR_MODE;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_AttachFile;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Message;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMTCPProgressLogger;
import jp.okiislandsh.library.core.AbsTuple;
import jp.okiislandsh.library.core.DateUtil;

public class TalkFileView extends TalkLayout {

    private final @NonNull LiveLive<IPMEntity_AttachFile> liveFile = new LiveLive<>();
    private final @NonNull LiveLive<IPMTCPProgressLogger.FileProgress> liveProgress = new LiveLive<>(); //Fileが設定されたタイミングでLiveDataを遅延設定する

    /** contentContainer直下コンテンツUri */
    public final @NonNull UriView uriView;

    public TalkFileView(@NonNull Context context) {
        super(context);

        uriView = new UriView(context);
        contentContainer.addView(uriView);

        liveFile.observe(this, f->{
            liveProgress.setLiveData(f==null ? null : IPMTCPProgressLogger.getLiveData(f.messageID, f.fileID));
            onChanged(liveMessage.getValue(), f, liveProgress.getValue());
        });
        liveProgress.observe(this, p->onChanged(liveMessage.getValue(), liveFile.getValue(), p));

    }

    public void setLiveData(@NonNull LiveData<IPMEntity_Address> liveAddress, @NonNull LiveData<IPMEntity_Message> liveMessage, LiveData<IPMEntity_AttachFile> liveFile){
        setLiveData(liveAddress, liveMessage);
        this.liveFile.setLiveData(liveFile);
    }

    @Override
    public void clearLiveData(){
        liveAddress.setLiveData(null);
        liveMessage.setLiveData(null);
        liveFile.setLiveData(null);
    }

    public static final class LiveTuple extends AbsTuple {
        public final @Nullable IPMEntity_Message message;
        public final @Nullable IPMEntity_AttachFile file;
        public final @Nullable IPMTCPProgressLogger.FileProgress progress;

        public LiveTuple(@Nullable IPMEntity_Message message, @Nullable IPMEntity_AttachFile file, @Nullable IPMTCPProgressLogger.FileProgress progress) {
            this.message = message;
            this.file = file;
            this.progress = progress;
        }
    }
    protected @Nullable LiveTuple lastLiveData;
    public @NonNull LiveTuple getLastLiveData(){
        return lastLiveData==null ? new LiveTuple(null, null, null) : lastLiveData;
    }

    @Override
    protected void onChanged(@Nullable IPMEntity_Address a, @Nullable IPMEntity_Message m) {
        super.onChanged(a, m);
        onChanged(m, liveFile.getValue(), liveProgress.getValue());
    }

    public void onChanged(@Nullable IPMEntity_Message m, @Nullable IPMEntity_AttachFile f, @Nullable IPMTCPProgressLogger.FileProgress progress) {
        lastLiveData = new LiveTuple(m, f, progress);

        setAlpha(f==null ? 0 : (f.completedDate==null ? 0.5f : 1));

        //ヘッダテキストに関する変更があるか
        final @NonNull String newHeaderText = makeHeaderText(f, progress, (m == null || m.owner));
        if(!newHeaderText.contentEquals(headerTextView.getText())){
            headerTextView.setText(newHeaderText);
        }

        //Uriの変更があるか
        final @Nullable Uri newUri = (f==null ? null : f.uri);
        final @Nullable Uri oldUri = uriView.getUri();
        if(oldUri==null || !Objects.equals(oldUri, newUri)){
            if(newUri==null) {
                if(f!=null) {
                    uriView.setLayoutParams(new LinearLayout.LayoutParams(256, 256));
                    if (FILE_ATTR_MODE.IPMSG_FILE_DIR.is(f.fileAttribute)) {
                        uriView.setDirUri(null, f.fileName);
                    } else {
                        uriView.setUnknownFileView(null, f.fileName);
                    }
                }else{
                    uriView.setLayoutParams(new LinearLayout.LayoutParams(256, 128));
                    uriView.setText("FileInfo no found.");
                }
            }else{
                if(FILE_ATTR_MODE.match(f.fileAttribute, FILE_ATTR_MODE.IPMSG_FILE_CLIPBOARD, FILE_ATTR_MODE.IPMSG_FILE_REGULAR)) {
                    final @Nullable String extension = getFileExtension(f.fileName, true);
                    if(MimeUtil.SUPPORTED_IMAGE.existExtension(extension) ||
                            MimeUtil.SUPPORTED_VIDEO.existExtension(extension)) {
                        uriView.setLayoutParams(newParamsWW(null));
                        uriView.setMediaUri(newUri);
                    }else{
                        uriView.setLayoutParams(new LinearLayout.LayoutParams(256, 256));
                        uriView.setUnknownFileView(newUri, f.fileName);
                    }
                }else if(FILE_ATTR_MODE.IPMSG_FILE_DIR.is(f.fileAttribute)) {
                    uriView.setLayoutParams(new LinearLayout.LayoutParams(256, 256));
                    uriView.setDirUri(newUri, f.fileName);
                }else{
                    uriView.setLayoutParams(new LinearLayout.LayoutParams(256, 128));
                    uriView.setText("Not Supported Attr Mode "+FILE_ATTR_MODE.parse(f.fileAttribute));
                }
            }
        }
    }

    private @NonNull String makeHeaderText(@Nullable IPMEntity_AttachFile file, @Nullable IPMTCPProgressLogger.FileProgress progress, boolean owner){
        if(file==null) {
            return "";
        }else{
            final @NonNull StringBuilder buf = new StringBuilder();
            if(file.completedDate==null){
                if(progress==null) { //転送前
                    buf.append(owner ? "Not Uploaded " : "Not Downloaded ");
                }else { //転送中
                    if(progress.isOnTimeOut()) {
                        buf.append("TimeOut");
                    }else if(progress.isOnError()) {
                        buf.append("Error");
                    }else {
                        if(file.fileSize==0) {
                            buf.append(Formatter.formatFileSize(getContext(), progress.byteSum));
                        }else{
                            buf.append(String.format("%.2f%%", 100f * progress.byteSum / file.fileSize) );
                        }
                    }
                }
            }else{ //転送後
                buf.append(DateUtil.toEasyString(file.completedDate)).append(" ");
                /*if(file.completedByteSum==null){
                    buf.append("error size");
                }else{
                    buf.append(Formatter.formatFileSize(getContext(), file.completedByteSum));
                }*/
            }
            return buf.toString();
        }
    }

}
