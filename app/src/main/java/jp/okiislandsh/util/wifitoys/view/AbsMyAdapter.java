package jp.okiislandsh.util.wifitoys.view;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import jp.okiislandsh.library.android.async.AsyncUtil;

public abstract class AbsMyAdapter<KEY, VIEW_HOLDER extends AbsMyAdapter.MyViewHolder<KEY>>
                                                                extends RecyclerView.Adapter<VIEW_HOLDER> {
    /** リストアイテムが表示されてから値の設定を始めるまでの遅延時間、これがないと高速スクロールされた時に表示されないデータの取得処理が走る。 */
    public static final int DELAY_MILLIS = 100;
    public interface OnBindDataKey<KEY>{
        @NonNull
        KEY onBindDataKey(int position);
    }
    public interface OnBindData<KEY, VIEW_HOLDER extends RecyclerView.ViewHolder>{
        /** @param isUpdate true 表示中アイテムの更新処理、恐らくnotifyDataSetChanged()がコールされた */
        void onBindData(boolean isUpdate, @NonNull VIEW_HOLDER holder, int position, @NonNull KEY key);
    }
    public interface OnGetItemCount{
        int getItemCount();
    }
    private final @NonNull Context context;
    private final @NonNull Handler handler;
    private final @NonNull OnBindDataKey<KEY> callbackBindDataKey;
    private final @NonNull OnBindData<KEY, VIEW_HOLDER> callbackBindData;
    private final @NonNull OnGetItemCount callbackGetItemCount;
    public AbsMyAdapter(@NonNull Context context, @NonNull OnBindDataKey<KEY> callbackBindDataKey, @NonNull OnBindData<KEY, VIEW_HOLDER> callbackBindData, @NonNull OnGetItemCount callbackGetItemCount){
        this.context = context;
        this.handler = AsyncUtil.getMainHandler();
        this.callbackBindDataKey = callbackBindDataKey;
        this.callbackBindData = callbackBindData;
        this.callbackGetItemCount = callbackGetItemCount;
    }
    @Override
    public final @NonNull VIEW_HOLDER onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final @NonNull VIEW_HOLDER viewHolder = onCreateViewHolder(context, parent, viewType);
        viewHolder.itemView.setOnClickListener((v)->{
            if(itemClickListener!=null) itemClickListener.onItemClick(viewHolder);
        });
        viewHolder.itemView.setOnLongClickListener((v)->{
            if(itemLongClickListener!=null) return itemLongClickListener.onItemLongClick(viewHolder);
            return false;
        });
        return onCreateViewHolder(context, parent, viewType);
    }
    public abstract @NonNull VIEW_HOLDER onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup parent, int viewType);
    @Override
    public void onBindViewHolder(@NonNull VIEW_HOLDER holder, int position) {
        final @NonNull KEY key = callbackBindDataKey.onBindDataKey(position);

        if(key.equals(holder.itemView.getTag())){ //前回と同じタグということは、
            callbackBindData.onBindData(true, holder, position, key); //表示中のアイテムに対して更新がかかった
        }else { //新たなアイテム生成
            holder.itemView.setTag(key);

            //値をクリアする
            holder.resetViewHolder(key);

            handler.postDelayed(() -> { //一定時間経過後にまだ表示されていればBind
                if (!callbackBindDataKey.onBindDataKey(position).equals(holder.itemView.getTag()))
                    return; //変わってしまった
                callbackBindData.onBindData(false, holder, position, key);
            }, DELAY_MILLIS);
        }

    }
    @Override
    public int getItemCount() {
        return callbackGetItemCount.getItemCount();
    }

    public abstract static class MyViewHolder<KEY> extends RecyclerView.ViewHolder{
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        /** 直ちにViewの前回値をクリアする。<br/>IPM DBから非同期でデータを取得し表示するため、前回値が上書きされるまでに時間がかかる。 */
        public abstract void resetViewHolder(@NonNull KEY key);
    }

    protected @Nullable OnItemClickListener<VIEW_HOLDER, KEY> itemClickListener = null;
    public interface OnItemClickListener<VIEW_HOLDER extends MyViewHolder<KEY>, KEY>{
        void onItemClick(@NonNull VIEW_HOLDER viewHolder);
    }
    public void setOnItemClickListener(@Nullable OnItemClickListener<VIEW_HOLDER, KEY> listener){
        this.itemClickListener = listener;
    }

    protected @Nullable OnItemLongClickListener<VIEW_HOLDER, KEY> itemLongClickListener = null;
    public interface OnItemLongClickListener<VIEW_HOLDER extends MyViewHolder<KEY>, KEY>{
        /** @return true if the callback consumed the long click, false otherwise. */
        boolean onItemLongClick(@NonNull VIEW_HOLDER viewHolder);
    }
    public void setOnItemLongClickListener(@Nullable OnItemLongClickListener<VIEW_HOLDER, KEY> listener){
        this.itemLongClickListener = listener;
    }


}
