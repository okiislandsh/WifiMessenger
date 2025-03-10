package jp.okiislandsh.util.wifitoys.view;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;

public class MessageAdapter extends AbsMyAdapter<Long, MessageAdapter.ViewHolder> {
    private final @NonNull LiveData<IPMEntity_Address> liveAddress;
    public MessageAdapter(@NonNull Context context, @NonNull LiveData<IPMEntity_Address> liveAddress, @NonNull AbsMyAdapter.OnBindDataKey<Long> callbackBindDataKey, @NonNull AbsMyAdapter.OnBindData<Long, ViewHolder> callbackBindData, @NonNull AbsMyAdapter.OnGetItemCount callbackGetItemCount) {
        super(context, callbackBindDataKey, callbackBindData, callbackGetItemCount);
        this.liveAddress = liveAddress;
    }

    @Override
    public @NonNull ViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(new MultipleTalkView(context, liveAddress));
    }

    public static class ViewHolder extends AbsMyAdapter.MyViewHolder<Long>{
        /** superのitemViewを上書き、いちいちキャストしなくて済む */
        public final @NonNull MultipleTalkView itemView;
        ViewHolder(@NonNull MultipleTalkView itemView) {
            super(itemView);
            this.itemView = itemView;
        }
        @Override
        public void resetViewHolder(@NonNull Long messageID) {
            itemView.reset(messageID, null);
        }
    }

}
