package jp.okiislandsh.util.wifitoys.ui.message;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import java.util.List;
import java.util.Objects;

import jp.okiislandsh.library.android.net.service.ipmsg.IPMDataBase;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.util.wifitoys.MyApp;
import jp.okiislandsh.util.wifitoys.RecyclerScrollPosition;

public class MessageViewModel extends AndroidViewModel {

    public final @NonNull IPMDataBase db;

    /** カレントユーザID、起動引数による */
    private final @NonNull LiveData<Long> sessionID;

    /** カレントユーザ */
    public final @NonNull LiveData<IPMEntity_Address> liveAddress;

    /** ユーザに紐づいたメッセージ情報 */
    public final @NonNull LiveData<List<Long>> liveMessageIDList;

    /** スクロール位置 */
    public final @NonNull MutableLiveData<RecyclerScrollPosition> liveScrollPosition;

    /** ViewModelの引数 */
    public static final @NonNull CreationExtras.Key<Long> KEY_SESSION_ID = new CreationExtras.Key<Long>() {};

    public static final ViewModelInitializer<MessageViewModel> initializer = new ViewModelInitializer<>(
            MessageViewModel.class,
            creationExtras -> {
                final @Nullable MyApp app = (MyApp) creationExtras.get(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY);
                assert app != null;
                final @NonNull SavedStateHandle savedStateHandle = SavedStateHandleSupport.createSavedStateHandle(creationExtras);

                final @Nullable Long sessionID = creationExtras.get(KEY_SESSION_ID);
                assert sessionID != null;

                return new MessageViewModel(app, savedStateHandle, sessionID);
            }
    );

    public MessageViewModel(@NonNull Application application, @NonNull SavedStateHandle state, @NonNull Long sessionID) {
        super(application);

        this.sessionID = new MutableLiveData<>(sessionID); //Navigationアクションを使用して起動した場合、getArguments()からNavigation引数を取得、NavigationGraph参照

        db = IPMDataBase.getInstance(application);
        liveAddress = db.asyncUser(sessionID);
        liveMessageIDList = db.asyncMessageIDs(sessionID);

        //スクロール位置
        liveScrollPosition = state.getLiveData("liveScrollPosition");
    }

    public long getSessionID(){
        return Objects.requireNonNull(sessionID.getValue());
    }

}