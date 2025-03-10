package jp.okiislandsh.util.wifitoys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import jp.okiislandsh.library.android.net.service.ipmsg.IPMService;
import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.library.core.Function;

public abstract class AbsDestinyFragmentWithIPMService extends AbsDestinyFragment implements ViewBuilderFunction.OnFragment {

    protected @Nullable IPMService receiveService(){
        return ((MainActivity) requireActivity()).receiveService;
    }

    protected void requireReceiveService(@NonNull Function.voidNonNull<IPMService> fn){
        requireReceiveService(fn, null);
    }
    protected void requireReceiveService(@NonNull Function.voidNonNull<IPMService> fn, @Nullable Runnable noService){
        requireMainActivity(activity->{
            if (activity.receiveService == null) {
                if(noService==null) {
                    showToastS("No found IPMService.");
                }else{
                    noService.run();
                }
            } else {
                fn.run(activity.receiveService);
            }
        });
    }

    protected void doBindService(){
        ((MainActivity) requireActivity()).doBindService();
    }

    protected @NonNull LiveData<Void> newLiveServiceNotify(){
        return ((MainActivity) requireActivity()).newLiveServiceNotify();
    }

}