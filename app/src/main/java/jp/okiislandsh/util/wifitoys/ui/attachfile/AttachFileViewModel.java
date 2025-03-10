package jp.okiislandsh.util.wifitoys.ui.attachfile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;

import java.util.List;

import jp.okiislandsh.library.android.net.service.ipmsg.IPMDataBase;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_AttachFileKeys;

public class AttachFileViewModel extends AndroidViewModel {

    protected final @NonNull IPMDataBase db;
    protected final @NonNull LiveData<List<IPMEntity_AttachFileKeys>> liveKeyList;

    public AttachFileViewModel(@NonNull Application context, @NonNull SavedStateHandle savedStateHandle) {
        super(context);

        db = IPMDataBase.getInstance(context);
        liveKeyList = db.asyncAttachFileKeys();
    }

}