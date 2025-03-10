package jp.okiislandsh.util.wifitoys.ui.dump;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

public class DumpViewModel extends AndroidViewModel {

    public final @NonNull MutableLiveData<TABLE> liveSelectedTable;
    public enum TABLE {
        ADDRESS_TABLE, MESSAGE_TABLE, ATTACH_FILE_TABLE
    }

    public DumpViewModel(@NonNull Application context, @NonNull SavedStateHandle savedStateHandle) {
        super(context);

        liveSelectedTable = savedStateHandle.getLiveData("liveSelectedTable", TABLE.ADDRESS_TABLE);
    }

}