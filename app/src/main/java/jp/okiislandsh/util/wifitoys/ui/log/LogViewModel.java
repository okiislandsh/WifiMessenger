package jp.okiislandsh.util.wifitoys.ui.log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.util.wifitoys.RecyclerScrollPosition;

public class LogViewModel extends ViewModel {

    protected final LiveData<LogDB.LogContainer> liveLastSystemLog;
    protected final LiveData<LogDB.LogContainer> liveLastUserLog;

    /** スクロール位置 */
    public final @NonNull MutableLiveData<RecyclerScrollPosition> liveScrollPosition;

    public LogViewModel(@NonNull SavedStateHandle state) {
        liveLastSystemLog = LogDB.getLastSystemLogLiveData();
        liveLastUserLog = LogDB.getLastUserLogLiveData();

        //スクロール位置
        liveScrollPosition = state.getLiveData("liveScrollPosition");
    }

    public @NonNull LogDB.ViewTable getSystemLog(){
        return LogDB.getViewTableAll();
    }

    public @NonNull LogDB.ViewTable getUserLog(){
        return LogDB.getViewTable(LogDB.LEVEL.USER);
    }

}