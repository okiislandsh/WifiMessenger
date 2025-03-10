package jp.okiislandsh.util.wifitoys.view;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AddressCardAdapter<KEY> extends AbsMyAdapter<KEY, AddressCardAdapter.ViewHolder<KEY>> {
    public AddressCardAdapter(@NonNull Context context, @NonNull OnBindDataKey<KEY> callbackBindDataKey, @NonNull OnBindData<KEY, ViewHolder<KEY>> callbackBindData, @NonNull OnGetItemCount callbackGetItemCount) {
        super(context, callbackBindDataKey, callbackBindData, callbackGetItemCount);
    }

    @Override
    public @NonNull ViewHolder<KEY> onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup parent, int viewType) {
        final @NonNull SimpleTextCardView view = new SimpleTextCardView(context);
        view.setPadding(8, 2, 8 , 2);
        final @NonNull RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        view.setLayoutParams(params);
        return new ViewHolder<>(view);
    }

    public static class ViewHolder<KEY> extends AbsMyAdapter.MyViewHolder<KEY>{
        /** superのitemViewを上書き、いちいちキャストしなくて済む */
        public final @NonNull SimpleTextCardView itemView;
        ViewHolder(@NonNull SimpleTextCardView itemView) {
            super(itemView);
            this.itemView = itemView;
        }
        @Override
        public void resetViewHolder(@NonNull KEY key) {
            itemView.reset();
        }
    }

}
