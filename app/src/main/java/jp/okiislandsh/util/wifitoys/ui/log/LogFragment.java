package jp.okiislandsh.util.wifitoys.ui.log;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.MyUtil.requireNonNull;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.PrintWriter;
import java.io.StringWriter;

import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMLogEvent;
import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.library.core.MathUtil;
import jp.okiislandsh.util.wifitoys.AbsDestinyFragment;
import jp.okiislandsh.util.wifitoys.R;
import jp.okiislandsh.util.wifitoys.RecyclerScrollPosition;
import jp.okiislandsh.util.wifitoys.databinding.FragmentLogBinding;
import jp.okiislandsh.util.wifitoys.view.AbsMyAdapter;

public class LogFragment extends AbsDestinyFragment {

    private FragmentLogBinding bind;

    private LogViewModel vm;

    //region ライフサイクル
    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        vm = new ViewModelProvider(this).get(LogViewModel.class);

        //ViewBindingインフレート
        bind = FragmentLogBinding.inflate(inflater, container, false);

        //アダプター
        final @NonNull LinearLayoutManager llm = new LinearLayoutManager(requireContext());
        bind.logList.setLayoutManager(llm);
        //bind.logList.getRecycledViewPool().setMaxRecycledViews(0, 30); //少ないとスクロールがカクつく。でふぉは5

        sysLogAdapter = new LogAdapter(requireContext(), position -> vm.getSystemLog().keyAt(vm.getSystemLog().size()-position-1),
                (isUpdate, holder, position, key) -> holder.reset(vm.getSystemLog().get(key)), () -> vm.getSystemLog().size());
        userLogAdapter = new LogAdapter(requireContext(), position -> vm.getUserLog().keyAt(vm.getUserLog().size()-position-1),
                (isUpdate, holder, position, key) -> holder.reset(vm.getUserLog().get(key)), () -> vm.getUserLog().size());

        if(bind.toggleButton.isChecked()){
            bind.logList.setAdapter(sysLogAdapter);
        }else{
            bind.logList.setAdapter(userLogAdapter);
        }
        bind.toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                bind.logList.setAdapter(sysLogAdapter);
            }else{
                bind.logList.setAdapter(userLogAdapter);
            }
        });


        //スクロール位置の保存
        bind.logList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                try {
                    vm.liveScrollPosition.postValue(RecyclerScrollPosition.getScrollPosition(bind.logList, llm));
                } catch (Exception e) {
                    Log.e("スクロール位置の保存に失敗", e);
                }
            }
        });

        //初期スクロール位置の復元
        final @Nullable RecyclerScrollPosition initialScrollPosition = vm.liveScrollPosition.getValue();
        if (initialScrollPosition != null) {
            llm.scrollToPositionWithOffset(initialScrollPosition.firstVisiblePositionIndex, initialScrollPosition.positionOffset);
        }

        //共通ログオブザーバ
        vm.liveLastSystemLog.observe(getViewLifecycleOwner(), log->sysLogAdapter.notifyDataSetChanged());
        vm.liveLastUserLog.observe(getViewLifecycleOwner(), log->userLogAdapter.notifyDataSetChanged());

        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //OptionMenu
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuProvider.super.onPrepareMenu(menu);
                menu.add(0, R.string.action_clear_log, 0, R.string.action_clear_log);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.string.action_clear_log) {
                    new AlertDialog.Builder(requireContext())
                            .setMessage(R.string.action_clear_log)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                LogDB.clearAll();
                                if (sysLogAdapter != null) sysLogAdapter.notifyDataSetChanged();
                                if (userLogAdapter != null) userLogAdapter.notifyDataSetChanged();
                            })
                            .show();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); //Fragmentでaddする時の作法

    }

    @Override
    public void onDestroyView() {

        //ビューモデルのクリア、フラグメントの流儀
        vm = null;
        bind = null;

        super.onDestroyView();
    }

    //endregion

    private @Nullable LogAdapter sysLogAdapter = null;
    private @Nullable LogAdapter userLogAdapter = null;

    private static class LogAdapter extends AbsMyAdapter<Integer, LogAdapter.MyViewHolder>{
        public LogAdapter(@NonNull Context context, @NonNull OnBindDataKey<Integer> callbackBindDataKey, @NonNull OnBindData<Integer, MyViewHolder> callbackBindData, @NonNull OnGetItemCount callbackGetItemCount) {
            super(context, callbackBindDataKey, callbackBindData, callbackGetItemCount);
        }
        @Override
        public @NonNull MyViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup parent, int viewType) {
            final @NonNull LogView v = new LogView(context);
            final @NonNull RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(2, 2, 2, 4);
            v.setLayoutParams(params);
            v.setMinimumHeight(20);
            return new MyViewHolder(v);
        }

        private static class MyViewHolder extends AbsMyAdapter.MyViewHolder<Integer> {
            final @NonNull LogView itemView;
            MyViewHolder(@NonNull LogView itemView) {
                super(itemView);
                this.itemView = itemView;
            }
            @Override
            public void resetViewHolder(@NonNull Integer id) {
                itemView.reset(null);
            }
            void reset(@NonNull LogDB.LogContainer log){
                itemView.reset(log);
            }
        }
        private static class LogView extends FrameLayout implements ViewBuilderFunction.OnViewGroup {
            final @NonNull TextView textView;
            public LogView(@NonNull Context context) {
                super(context);
                textView = addTextRelative(null, null, .8f);
            }
            public void reset(@Nullable LogDB.LogContainer log){
                textView.setVisibility(log==null ? INVISIBLE : VISIBLE);
                if(log!=null){
                    textView.setTextColor(log.level== LogDB.LEVEL.ERROR ? 0xffcc0000 : (log.level== LogDB.LEVEL.WARNING ? 0xff880088 : Color.BLACK));
                    textView.setText("["+log.id+"]" + log.getHmmssSSS() + " " + log.level.name().substring(0,1) + " " + MyUtil.substring(log.logItem.toString(), 100));
                    final @NonNull String replaceException = MyUtil.requireNonNull(log.e, e->{
                        try(@NonNull StringWriter sw = new StringWriter()){
                            try(@NonNull PrintWriter pw = new PrintWriter(sw)){
                                e.printStackTrace(pw);
                                pw.flush();
                                sw.flush();
                                return BR+sw.toString();
                            }
                        }catch (Exception e2){
                            return "Can't print stack trace...";
                        }
                    }, "");
                    if(log.logItem instanceof IPMLogEvent){
                        final @NonNull IPMLogEvent l = (IPMLogEvent)log.logItem;
                        textView.setTag(
                                "["+log.id+"]" + log.getHmmssSSS() + " " + log.level.name().substring(0,1) + " " +
                                        log.getLogTag() + BR +
                                        l.toMaxString() +
                                        replaceException
                        );
                        final int[] hashColor = new int[]{0x80ffaaaa, 0x80ffffaa, 0x80aaffaa, 0x80aaffff, 0x80aaaaff, 0x80ffaaff};
                        setBackgroundColor(l.receive==null ? 0x80cccccc : hashColor[ (int)(MathUtil.toUnsigned(l.receive.packet.packetNo)%hashColor.length) ]);
                    }else{
                        textView.setTag(
                                "["+log.id+"]" + log.getHmmssSSS() + " " + log.level.name().substring(0,1) + " " +
                                        log.getLogTag() + BR +
                                        log.logItem.toString() +
                                        replaceException
                        );
                        final int[] hashColor = new int[]{0x80ccaaaa, 0x80aaccaa, 0x80aaaacc, 0x80aaaaaa, 0x80ccffff, 0x80ffccff, 0x80ffffcc, 0x80ffffff};
                        setBackgroundColor(hashColor[Math.abs(MyUtil.requireNonNull(requireNonNull(log.ste6, StackTraceElement::getFileName, "UnknownFile"), Object::hashCode, log.id))%hashColor.length]);
                    }
                    textView.setOnClickListener(v -> {
                        final @NonNull CharSequence tmp = textView.getText();
                        textView.setText(textView.getTag().toString());
                        textView.setTag(tmp);
                    });
                }
            }
        }
    }

}