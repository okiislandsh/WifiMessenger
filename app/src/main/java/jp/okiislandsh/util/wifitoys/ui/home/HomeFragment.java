package jp.okiislandsh.util.wifitoys.ui.home;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.async.AsyncUtil;
import jp.okiislandsh.library.android.live.LiveClock;
import jp.okiislandsh.library.android.net.NetUtil;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMDataBase;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMPrefInt;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMService;
import jp.okiislandsh.library.android.view.BadgeTextView;
import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.library.android.view.chain.WidthChainFrameLayout;
import jp.okiislandsh.library.android.view.live.LifecycleLinearLayout;
import jp.okiislandsh.library.android.view.live.LiveBadgeTextView;
import jp.okiislandsh.library.core.Function;
import jp.okiislandsh.util.wifitoys.AbsDestinyFragmentWithIPMService;
import jp.okiislandsh.util.wifitoys.MainActivity;
import jp.okiislandsh.util.wifitoys.R;
import jp.okiislandsh.util.wifitoys.databinding.FragmentHomeBinding;

public class HomeFragment extends AbsDestinyFragmentWithIPMService {

    /** ViewBinding(レイアウト参照) */
    private FragmentHomeBinding bind;

    private HomeViewModel vm;

    private final @NonNull LiveClock liveNetStatusUpdateClock = LiveClock.newInstance(null, LiveClock.TIME_SPAN._5SECONDLY);

    //region ライフサイクル
    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        bind = FragmentHomeBinding.inflate(inflater, container, false);

        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View _view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(_view, savedInstanceState);

        //TODO: オンラインユーザがいないとき、「IP-Messengerユーザが見つかりません」みたいなメッセージ

        //ステータス表示の自動更新
        updateTextNetInfo();
        liveNetStatusUpdateClock.observe(getViewLifecycleOwner(), unused -> updateTextNetInfo());
        newLiveServiceNotify().observe(getViewLifecycleOwner(), unused -> updateTextNetInfo());

        //ヘッダ部
        bind.buttonStartService.setOnClickListener(v-> {
            MainActivity.startForegroundService();
            doBindService(); //タイムラグなどで、万が一接続できないこともあるらしい
            AsyncUtil.postMainThread(()-> {
                //一定時間経過後にActivityが生きていたらbindリクエスト
                if(requireActivity().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    doBindService();
                }
            }, 300);
        });
        bind.buttonStopService.setOnClickListener(v-> MainActivity.stopForegroundService());
        vm.nickName.observe(getViewLifecycleOwner(),
                s -> bind.textName.setText(s+" / "+vm.groupName.getValue()));
        vm.groupName.observe(getViewLifecycleOwner(),
                s -> bind.textName.setText(vm.nickName.getValue()+" / "+s));
        bind.buttonUserInfoEdit.setOnClickListener(v->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.nav_setting_user_info));

        //アドレス帳表示
        adapter = new AddressAdapter(requireContext(), getViewLifecycleOwner(), vm.onlineList, vm.addressComparator,
                bind.colTextNN, bind.colTextGN, bind.colTextIP);
        bind.userList.setAdapter(adapter);
        bind.userList.setOnItemClickListener((parent, view, position, id) -> {
            final @Nullable IPMEntity_Address address = adapter.getItem(position);
            if(address==null) {
                showToastS("NullReference.");
            }else{
                NavDirections action = HomeFragmentDirections.actionNavHomeToNavMessage().setSessionID(address.sessionID);
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(action);
                /*
                new AlertDialog.Builder(context)
                        .setMessage(HomeFragment.toString(address))
                        .show();*/
            }
        });
        bind.userList.setOnItemLongClickListener((parent, view, position, id) -> {
            final @Nullable IPMEntity_Address address = adapter.getItem(position);
            if(address==null) {
                showToastS("NullReference.");
            }else{
                new AlertDialog.Builder(requireContext())
                        .setView(Function.with(newText(address.toString(), newParamsWW()), t->t.setTextIsSelectable(true)))
                        .show();
            }
            return true;
        });
        adapter.setBlackListOnClickListener(position->{
            final @Nullable IPMEntity_Address address = adapter.getItem(position);
            if(address==null) {
                showToastS("NullReference.");
            }else{
                new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.msg_clear_blacklist)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> requireReceiveService(s->s.setBlackListState(address, false)))
                        .show();
            }
        });
        adapter.setStarOnClickListener(position->{
            final @Nullable IPMEntity_Address address = adapter.getItem(position);
            if(address==null) {
                showToastS("NullReference.");
            }else{
                new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.msg_reverse_friend)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> requireReceiveService(s->s.setFriendState(address, !address.friend)))
                        .show();
            }
        });

        //非アクティブユーザの未読がある場合のアーカイブボタンの表示制御
        final @NonNull LiveData<Long> liveUnShowMessageCount = vm.db.asyncCountOfUnShowedMessagesOfArchive(); //非アクティブユーザの未読件数Live
        liveUnShowMessageCount.observe(getViewLifecycleOwner(), count-> {
            if(count<=0){
                bind.fabAlert.setVisibility(GONE);
                return;
            }
            bind.fabAlert.setVisibility(VISIBLE);
            final @NonNull SpannableStringBuilder buf = new SpannableStringBuilder(getString(R.string.menu_archives));
            buf.append(newSizeSpan("("+(count<10 ? count : "9+")+")", .8f));
            bind.fabAlert.setText(buf);
        });

        //非アクティブユーザの未読がある場合のアーカイブ直リンク
        bind.fabAlert.setOnClickListener(v-> requireMainActivity().getNavController(R.id.nav_host_fragment).navigate(R.id.action_nav_home_to_nav_archive));

        //起動から2秒後にメッセージ受信通知をクリア、現状メッセージ受信通知は単一の通知IDを使っているため細かい制御はできない
        //ホーム画面とメッセージ画面を表示した時だけ消す。通知にメッセージIDみたいのがあったり、メッセージの受信音を通知音にたよっていなければ他の方法がよい
        AsyncUtil.postMainThread(()->requireReceiveService(IPMService::clearMessageNotification), 2000);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuProvider.super.onPrepareMenu(menu); //一応
                menu.add(0, R.string.action_refresh, 0, R.string.action_refresh).setIcon(android.R.drawable.ic_menu_rotate).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.string.action_refresh) {
                    requireReceiveService(receiveService->{
                        receiveService.refreshOnlineUsers();
                        showToastL(getString(R.string.msg_refresh) + "(" +
                                MyUtil.toStringOf(NetUtil.getBroadcastAddress2(), BR, "null", (buf, ip) -> buf.append(NetUtil.toString(ip))) +
                                ")");
                    });
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); //Fragmentでaddする時の作法

    }

    private static void setEnableVisible(@NonNull View view, boolean enable){
        view.setEnabled(enable);
        view.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
    }

    /** サービス状況とかの表示Textを更新する */
    protected void updateTextNetInfo(){
        Log.d("updateTextNetInfo start "+Thread.currentThread().getName());
        requireReceiveService(receiveService->{
            final long elapsedTimeMillis = receiveService.getServiceElapsedTimeMillis();
            final long elapsedTimeMinutes = elapsedTimeMillis / (1000 * 60);
            final @NonNull String elapsedTimeText = "("+isJa("サービス経過: ", "Service elapsed: ") +
                    elapsedTimeMinutes + isJa("分", elapsedTimeMinutes<=1 ? "Minute" : "Minutes") + ")";
            bind.textServiceStatus.setText(R.string.service_is_running);
            setEnableVisible(bind.buttonStartService, false);
            setEnableVisible(bind.buttonStopService, true);
            if (!IPMService.isServiceExecutableNetworkStatus()) {
                bind.textNetInfo.setText(getString(R.string.msg_no_available_network));
                updateTextNetStyle_Orange();
            } else {
                final @NonNull Integer[] portArray = IPMPrefInt.getPortList(requireContext()).toArray(new Integer[0]);
                final @NonNull SpannableStringBuilder buf = new SpannableStringBuilder();
                switch (portArray.length) {
                    case 0:
                        buf.append("No found Port-Setting. Error?");
                        break;
                    case 1:
                        if (portArray[0].equals(IPMPrefInt.MAIN_PORT.getDefaultValue())) {
                            buf.append(receiveService.isAvailableUDP(portArray[0]) ?
                                            getString(R.string.msg_service_normal) :
                                            newColorSpan(getString(R.string.port)+portArray[0]+": "+getString(R.string.udp_unavailable),0xffff8888 , null)
                            );
                            break;
                        }
                        //else 2425以外を開いている場合、ポート番号を表示する
                    default:
                        for (int port : portArray) {
                            final boolean isAvailableUDP = receiveService.isAvailableUDP(port);
                            final @NonNull CharSequence msg = getString(R.string.port)+ port +": "+(isAvailableUDP? getString(R.string.udp_available) : getString(R.string.udp_unavailable));
                            buf.append(isAvailableUDP ? msg : newColorSpan(msg, 0xffff8888, null));
                            buf.append(BR); //末尾にサービス経過時間を追加するため常に改行
                        }
                }
                buf.append(newSizeSpan(elapsedTimeText, .7f));
                bind.textNetInfo.setText(buf);
                updateTextNetStyle_Blue();
            }
        }, ()->{
            bind.textServiceStatus.setText(R.string.service_is_not_running);
            setEnableVisible(bind.buttonStartService, true);
            setEnableVisible(bind.buttonStopService, false);
            bind.textNetInfo.setText(getString(R.string.msg_service_does_not_exist_or));
            updateTextNetStyle_Red();
        });
    }

    public void updateTextNetStyle_Blue() {
        updateTextNetStyle(R.color.colorStatusBlue, R.drawable.clickable_status_blue_background);
    }
    public void updateTextNetStyle_Orange() {
        updateTextNetStyle(R.color.colorStatusOrange, R.drawable.clickable_status_orange_background);
    }
    public void updateTextNetStyle_Red() {
        updateTextNetStyle(R.color.colorStatusRed, R.drawable.clickable_status_red_background);
    }
    /** ステータス表示に使用している全てのTextViewにスタイルを適用する */
    public void updateTextNetStyle(@DrawableRes int containerBackground, @DrawableRes int clickableBackground) {
        bind.textServiceStatus.setTextColor(Color.WHITE);
        bind.textServiceStatus.setBackgroundResource(containerBackground);

        bind.textNetInfo.setTextColor(Color.WHITE);
        bind.textNetInfo.setBackgroundResource(containerBackground);

        bind.nameContainer.setBackgroundResource(containerBackground);
        bind.textName.setTextColor(Color.WHITE);
        bind.textName.setBackgroundColor(Color.TRANSPARENT);
        bind.textYourName.setTextColor(Color.WHITE);
        bind.textYourName.setBackgroundColor(Color.TRANSPARENT);
        bind.buttonUserInfoEdit.setColorFilter(Color.WHITE);
        bind.buttonUserInfoEdit.setBackgroundResource(clickableBackground);

        bind.buttonStartService.setTextColor(Color.WHITE);
        bind.buttonStartService.setBackgroundResource(clickableBackground);
        bind.buttonStopService.setTextColor(Color.WHITE);
        bind.buttonStopService.setBackgroundResource(clickableBackground);

    }

    @Override
    public void onDestroyView() {
        //ビューモデルのクリア、フラグメントの流儀
        vm = null;
        bind = null;

        super.onDestroyView();
    }

    //endregion

    private @Nullable AddressAdapter adapter = null;

    private static class AddressAdapter extends BaseAdapter implements ViewBuilderFunction {
        private final @NonNull Context context;
        @Override
        public @NonNull Context getContext() {
            return context;
        }

        /** @noinspection FieldCanBeLocal 保持しないとLiveDataは破棄されてObserverが呼ばれない*/
        private final @NonNull LiveData<List<IPMEntity_Address>> liveList;
        private final @NonNull List<IPMEntity_Address> addressList = new ArrayList<>();
        private final @NonNull MutableLiveData<AddressComparator> liveComparator;
        private final @NonNull WidthChainFrameLayout widthChainHeaderColNickName;
        private final @NonNull WidthChainFrameLayout widthChainHeaderColGroupName;
        private final @NonNull WidthChainFrameLayout widthChainHeaderColIP;

        private @Nullable BlackListOnClickListener blackListOnClickListener;
        public interface BlackListOnClickListener {
            void onClick(int position);
        }
        public void setBlackListOnClickListener(@NonNull BlackListOnClickListener listener){
            blackListOnClickListener = listener;
        }
        private @Nullable StarOnClickListener starOnClickListener;
        public interface StarOnClickListener {
            void onClick(int position);
        }
        public void setStarOnClickListener(@NonNull StarOnClickListener listener){
            starOnClickListener = listener;
        }
        AddressAdapter(@NonNull Context context,
                       @NonNull LifecycleOwner owner,
                       @NonNull LiveData<List<IPMEntity_Address>> liveList,
                       @NonNull MutableLiveData<AddressComparator> liveComparator,
                       @NonNull WidthChainFrameLayout widthChainHeaderColNickName,
                       @NonNull WidthChainFrameLayout widthChainHeaderColGroupName,
                       @NonNull WidthChainFrameLayout widthChainHeaderColIP){
            this.context = context;
            this.liveList = liveList;
            this.liveComparator = liveComparator;
            this.widthChainHeaderColNickName = widthChainHeaderColNickName;
            this.widthChainHeaderColGroupName = widthChainHeaderColGroupName;
            this.widthChainHeaderColIP = widthChainHeaderColIP;
            liveList.observe(owner, newList -> {
                synchronized (this){ //Adapterをロック
                    addressList.clear();
                    addressList.addAll(newList);
                    Collections.sort(addressList, this.liveComparator.getValue());
                    notifyDataSetChanged();
                }
            });
            liveComparator.observe(owner, comparator -> {
                synchronized (this) { //Adapterをロック
                    Collections.sort(addressList, comparator);
                    notifyDataSetChanged();
                }
            });
        }
        @Override
        public int getCount() {
            return addressList.size();
        }
        @Override
        public IPMEntity_Address getItem(int position) {
            return addressList.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView = createView();
            }
            Holder holder = (Holder) convertView.getTag();
            final @NonNull IPMEntity_Address address = addressList.get(position);
            final @ColorInt int hashColor = address.hashColor();
            //final @NonNull StateListDrawable drawable = newPressColorDrawable(hashColor, 0x88FFFFFF & hashColor, Color.LTGRAY);
            holder.userNameColView.setBackgroundColor(hashColor);
            holder.groupName.textView.setBackgroundColor(hashColor);
            holder.ip.textView.setBackgroundColor(hashColor);
            holder.userNameColView.reset(address);
            holder.groupName.setText(address.groupName);
            final @NonNull StringBuilder ipText = new StringBuilder(NetUtil.toString(address.ip));
            if(address.port != IPMPrefInt.MAIN_PORT.getDefaultValue()) ipText.append(":").append(address.port);
            holder.ip.setText(newSizeSpan(ipText, .8f));

            holder.userNameColView.blackView.setOnClickListener(v->{
                if(blackListOnClickListener!=null) blackListOnClickListener.onClick(position);
            });
            holder.userNameColView.starView.setOnClickListener(v->{
                if(starOnClickListener!=null) starOnClickListener.onClick(position);
            });

            return convertView;
        }
        private static class Holder {
            LinearLayout root;
            UserNameColView userNameColView;
            TextColView groupName;
            TextColView ip;
        }
        private @NonNull View createView(){
            Holder holder = new Holder();
            holder.root = newLinearLayout(LinearLayout.HORIZONTAL, new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            holder.userNameColView = addUserNameCol(holder.root, widthChainHeaderColNickName);
            holder.groupName = addTextCol(holder.root, widthChainHeaderColGroupName);
            holder.ip = addTextCol(holder.root, widthChainHeaderColIP);

            /*holder.addressView = new IPMAddressView(context);
            holder.root.addView(holder.addressView);*/

            holder.root.setTag(holder);

            return holder.root;
        }
        @NonNull UserNameColView addUserNameCol(@NonNull ViewGroup parent, @NonNull WidthChainFrameLayout chainView){
            final @NonNull UserNameColView ret = new UserNameColView(context);
            ret.setLayoutParams(new AbsListView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)); //FrameがPARENTでTextもPARENTだと、Text最大公約数的なWRAPになる
            ret.userNameTextView.setLayoutParams(new UserNameColView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            parent.addView(ret);
            chainView.addChainView(ret);
            return ret;
        }
        @NonNull TextColView addTextCol(@NonNull ViewGroup parent, @NonNull WidthChainFrameLayout chainView){
            final @NonNull TextColView ret = new TextColView(context);
            ret.setLayoutParams(new AbsListView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)); //FrameがPARENTでTextもPARENTだと、Text最大公約数的なWRAPになる
            ret.textView.setLayoutParams(new TextColView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            parent.addView(ret);
            chainView.addChainView(ret);
            return ret;
        }
    }
    private static class TextColView extends FrameLayout implements ViewBuilderFunction.OnViewGroup {
        public final @NonNull TextView textView;
        public TextColView(@NonNull Context context) {
            super(context);
            textView = addText(null, null, null);
        }
        public void setText(@Nullable CharSequence text){
            textView.setText(text);
        }
    }
    private static class UserNameColView extends LifecycleLinearLayout implements ViewBuilderFunction.OnViewGroup {
        public final @NonNull LiveBadgeTextView<Long> liveBadgeView;
        public final @NonNull BadgeTextView absenceView;
        public final @NonNull BadgeTextView blackView;
        public final @NonNull TextView starView;
        public final @NonNull TextView userNameTextView;
        public UserNameColView(@NonNull Context context) {
            super(context);

            setOrientation(HORIZONTAL);

            liveBadgeView = new LiveBadgeTextView<>(context);
            addView(liveBadgeView);
            absenceView = new BadgeTextView(context);
            absenceView.setTextColor(Color.BLACK);
            absenceView.postBackColor(Color.WHITE);
            addView(absenceView);
            blackView = new BadgeTextView(context);
            blackView.setTextColor(Color.WHITE);
            blackView.postBackColor(Color.BLACK);
            addView(blackView);
            starView = addText(null, null, null);
            userNameTextView = addText(null, newParams0M(1f), null);

            observe(LiveClock.newInstance(null, LiveClock.TIME_SPAN._30SECONDLY),
                    unused->{
                        if(latestAddress!=null) reset(latestAddress);
                    }
            );
        }

        private @Nullable IPMEntity_Address latestAddress;
        public void reset(){
            latestAddress = null;
            liveBadgeView.clearLiveData();
            absenceView.setText("");
            blackView.setText("");
            starView.setText("");
            userNameTextView.setText("");
        }
        public void reset(@NonNull IPMEntity_Address address){
            latestAddress = address;
            liveBadgeView.setLiveData(IPMDataBase.getInstance(getContext()).asyncCountOfUnShowedMessagesOfAddress(address.sessionID));
            absenceView.setText(address.isAbsence() ? "!" : "");
            blackView.setText(address.blackList ? "B" : "");
            starView.setText(address.friend ? "★" : "☆");
            starView.setTextColor(address.friend ? 0xffff8800 : 0x66000000);
            userNameTextView.setText(newSpan(
                    address.getDisplayName(),
                    newSizeSpan(" "+getElapsedTimeText(address.lastTime), .5f)
            ));
        }
        /** 現在時刻との差から、n分テキスト */
        public @NonNull CharSequence getElapsedTimeText(long timeMillis){
            final long elapsedTimeMillis = System.currentTimeMillis() - timeMillis;
            final int seconds = (int) (elapsedTimeMillis/1000);
            if(seconds < 0){
                return "err";
            }else if (seconds < 60) {
                return isJa("たった今", "Now");
            } else {
                final int minutes = seconds/60;
                return isJa(String.format(Locale.JAPANESE, "%d+分前", minutes), String.format(Locale.US, "%d+m", minutes));
            }
        }
    }

}