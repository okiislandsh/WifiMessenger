package jp.okiislandsh.util.wifitoys.ui.home;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import java.util.List;

import jp.okiislandsh.library.android.live.NonNullLiveData;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMDataBase;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMPrefString;

public class HomeViewModel extends AndroidViewModel {

    public final @NonNull IPMDataBase db;
    public final @NonNull LiveData<List<IPMEntity_Address>> onlineList;
    public final @NonNull LiveData<Long> onlineCount;
    public final @NonNull NonNullLiveData<String> nickName;
    public final @NonNull NonNullLiveData<String> groupName;
    public final @NonNull MutableLiveData<AddressComparator> addressComparator;

    public HomeViewModel(@NonNull Application application, @NonNull SavedStateHandle savedStateHandle) {
        super(application);

        db = IPMDataBase.getInstance(application);
        onlineList = db.asyncOnlineUsers();
        onlineCount = db.asyncCountOfOnlineUsers();

        final @NonNull Context context = application.getApplicationContext();
        nickName = IPMPrefString.NN.getLive(context);
        groupName = IPMPrefString.GN.getLive(context);

        //addressComparatorへgetすると内部にkey＆valueが作られ、自動保存される。
        addressComparator = savedStateHandle.getLiveData("addressComparator", new AddressComparator(AddressComparator.KEY.ENTRY, false, null));
    }

}