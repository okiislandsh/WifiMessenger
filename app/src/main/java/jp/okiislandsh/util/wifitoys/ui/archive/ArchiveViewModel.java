package jp.okiislandsh.util.wifitoys.ui.archive;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import java.util.List;

import jp.okiislandsh.library.android.net.service.ipmsg.IPMDataBase;
import jp.okiislandsh.util.wifitoys.RecyclerScrollPosition;

public class ArchiveViewModel extends AndroidViewModel {

    public final @NonNull IPMDataBase db;
    public final @NonNull LiveData<List<Long>> liveArchivesSessionIDs;

    public final @NonNull MutableLiveData<RecyclerScrollPosition> liveScrollPosition;

    public ArchiveViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);

        db = IPMDataBase.getInstance(application);
        liveArchivesSessionIDs = db.asyncArchivesSessionIDs();

        //スクロール位置
        liveScrollPosition = state.getLiveData("liveScrollPosition");
    }

}