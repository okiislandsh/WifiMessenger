package jp.okiislandsh.util.wifitoys.ui.archive;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.async.AsyncUtil;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMDaoPublic;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMService;
import jp.okiislandsh.library.core.DateUtil;
import jp.okiislandsh.util.wifitoys.AbsDestinyFragmentWithIPMService;
import jp.okiislandsh.util.wifitoys.R;
import jp.okiislandsh.util.wifitoys.RecyclerScrollPosition;
import jp.okiislandsh.util.wifitoys.databinding.FragmentArchiveBinding;
import jp.okiislandsh.util.wifitoys.view.AddressCardAdapter;
import jp.okiislandsh.util.wifitoys.view.SimpleTextCardView;

public class ArchiveFragment extends AbsDestinyFragmentWithIPMService {

    /** ViewBinding(レイアウト参照) */
    private FragmentArchiveBinding bind;

    private ArchiveViewModel vm;

    /** bindDataに使用するワーカスレッド */
    private static final @NonNull HandlerThread workerThread = new HandlerThread("ArchiveFragment WorkerThread");
    private static final @NonNull Handler workerHandler;
    static {
        workerThread.start();
        //workerHandler = new Handler(workerThread.getLooper());
        workerHandler = MyUtil.getQuietly(()->new Handler(workerThread.getLooper()), new Handler()); //IDE layout.xmlプレビュー時のレンダリング例外回避。 実際にはnew Handler()が実行されるとWorkerThreadが無いためダメ
    }

    //region ライフサイクル
    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        vm = new ViewModelProvider(this).get(ArchiveViewModel.class);

        bind = FragmentArchiveBinding.inflate(inflater, container, false);

        return bind.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //RecyclerViewのレイアウトマネージャ
        final @NonNull LinearLayoutManager llm;
        if(MyUtil.getNowDeviceOrientationIsLandscape(requireContext())){
            llm = new GridLayoutManager(requireContext(), 2);
        }else{
            llm = new LinearLayoutManager(requireContext());
        }
        bind.userList.setLayoutManager(llm);
        //スクロール位置の保存
        bind.userList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                try {
                    vm.liveScrollPosition.postValue(RecyclerScrollPosition.getScrollPosition(bind.userList, llm));
                } catch (Exception e) {
                    Log.e("スクロール位置の保存に失敗", e);
                }
            }
        });

        //バックアクションで戻ってきたとき対応
        if(adapter!=null){ //adapterオブジェクト自体は残っているが、RecyclerViewに設定されていない。デバッグで確認。
            bind.userList.setAdapter(adapter);

            //初期スクロール位置の復元
            final @Nullable RecyclerScrollPosition initialScrollPosition = vm.liveScrollPosition.getValue();
            if (initialScrollPosition != null) {
                llm.scrollToPositionWithOffset(initialScrollPosition.firstVisiblePositionIndex, initialScrollPosition.positionOffset);
            }
        }

        //ヘッダ部
        vm.liveArchivesSessionIDs.observe(getViewLifecycleOwner(), list-> {
            if(adapter!=null){
                adapter.notifyDataSetChanged(); //値が変わっていなくてもUpdateだけでObserveが反応するので注意。
            }else {
                adapter = new AddressCardAdapter<>(requireContext(), index -> Objects.requireNonNull(vm.liveArchivesSessionIDs.getValue()).get(index), (isUpdate, holder, position, sessionID) -> {
                    //Workerスレッドでデータ取得
                    workerHandler.postAtFrontOfQueue(() -> { //データアクセスはワーカースレッドで、postAtFrontOfQueue = LIFO
                        final @Nullable IPMDaoPublic.MessageCountOfAddressTuple entity = vm.db.publicDao().getMessageCountOfAddress(sessionID);

                        AsyncUtil.postMainThread(() -> {
                            //既にフレームアウトした？
                            if (!sessionID.equals(holder.itemView.getTag())) return;

                            //DBからメッセージを取得できなかった
                            if (entity == null) {
                                holder.itemView.reset(new SimpleTextCardView.ResetTuple("", "error", "error", "error", null, null));
                            } else {
                                holder.itemView.reset(new SimpleTextCardView.ResetTuple(
                                        entity.countUnShow <= 0 ? "" : String.valueOf(entity.countUnShow),
                                        entity.address.getDisplayName() + (entity.address.groupName==null ? "" : " / "+entity.address.groupName),
                                        entity.address.isOnline() ? "online" :
                                                (entity.threadBeginDate == null ? "?" : DateUtil.toEasyString(entity.threadBeginDate)) + "-" + (entity.threadEndDate == null ? "?" : DateUtil.toEasyString(entity.threadEndDate)),
                                        newSpan(newSizeSpan(Long.toString(entity.count), 1.2f), newSizeSpan(" Messages", .6f)),
                                        v -> onItemClick(sessionID), v -> onItemLongClick(entity.address))
                                );
                                holder.itemView.setCardBackgroundColor(entity.address.hashColor());
                            }
                        });
                    });
                }, () -> { //LiveDataから再取得する、これによってnotifyDataSetChangedだけでデータ変更できる
                    final @Nullable List<Long> getList = vm.liveArchivesSessionIDs.getValue();
                    return getList == null || getList.isEmpty() ? 0 : getList.size();
                });

                bind.userList.setAdapter(adapter);
            }
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuProvider.super.onPrepareMenu(menu); //一応
                menu.add(0, R.string.action_clear_all_messages, 0, R.string.action_clear_all_messages);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.string.action_clear_all_messages) {
                    requireReceiveService(IPMService::deleteAllMessageAndFile);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); //Fragmentでaddする時の作法

    }

    private void onItemClick(long sessionID){
        NavDirections action = ArchiveFragmentDirections.actionArchiveFragmentToNavMessage().setSessionID(sessionID);
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(action);
    }

    private boolean onItemLongClick(@NonNull IPMEntity_Address address){
        //TODO:削除？
        new AlertDialog.Builder(requireContext())
                .setMessage(address.toString())
                .show();
        return false;
    }

    @Override
    public void onDestroyView() {
        //ビューモデルのクリア、フラグメントの流儀
        vm = null;
        bind = null;

        super.onDestroyView();
    }

    //endregion

    private @Nullable AddressCardAdapter<Long> adapter = null;

}