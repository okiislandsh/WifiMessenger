package jp.okiislandsh.util.wifitoys.ui.dump;

import static jp.okiislandsh.library.android.MyUtil.BR;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Formatter;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;
import java.util.List;

import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.SizeUtil;
import jp.okiislandsh.library.android.net.NetUtil;
import jp.okiislandsh.library.android.net.service.ipmsg.FILE_ATTR_MODE;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMDataBase;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_AttachFile;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_AttachFileKeys;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Message;
import jp.okiislandsh.library.android.view.DumpTableView.ColumnSetting;
import jp.okiislandsh.library.core.DateUtil;
import jp.okiislandsh.util.wifitoys.AbsDestinyFragment;
import jp.okiislandsh.util.wifitoys.R;
import jp.okiislandsh.util.wifitoys.databinding.FragmentDumpBinding;
import jp.okiislandsh.util.wifitoys.ui.dump.DumpViewModel.TABLE;

/** デバッグ用の各種テーブルダンプ */
public class DumpFragment extends AbsDestinyFragment {

    /** DB参照 */
    private IPMDataBase db;
    /** address_tableの時に取得する */
    private LiveData<List<Long>> sessionIDList;
    /** message_tableの時に取得する */
    private LiveData<List<Long>> messageIDList;
    /** attach_file_tableの時に取得する */
    private LiveData<List<IPMEntity_AttachFileKeys>> fileKeyList;

    /** ViewModel */
    private DumpViewModel vm;

    /** ViewBinding(レイアウト参照) */
    private FragmentDumpBinding bind;

    /** sp2px */
    private SizeUtil.SP2PXConst s;

    //region ライフサイクル
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        db = IPMDataBase.getInstance(context);
        s = SizeUtil.SP2PXConst.getInstance(context);
    }

    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        vm = new ViewModelProvider(this).get(DumpViewModel.class);

        //ViewBindingインフレート
        bind = FragmentDumpBinding.inflate(inflater, container, false);

        //テーブルを選択させる
        vm.liveSelectedTable.observe(getViewLifecycleOwner(), table -> {
            //LiveDataを破棄
            if(sessionIDList!=null) {
                sessionIDList.removeObservers(getViewLifecycleOwner());
                sessionIDList = null;
            }
            if(messageIDList!=null) {
                messageIDList.removeObservers(getViewLifecycleOwner());
                messageIDList = null;
            }
            if(fileKeyList !=null) {
                fileKeyList.removeObservers(getViewLifecycleOwner());
                fileKeyList = null;
            }
            //見出し
            setToolbarTitle(table.name());
            //DBからテーブルのデータ取得
            switch (table){
                case ADDRESS_TABLE:
                    //主キーのみ全件取得
                    sessionIDList = db.asyncSessionIDs();
                    //任意のセルのBindData
                    sessionIDList.observe(getViewLifecycleOwner(), list -> bind.dumpTableView.setData(list, new ColumnSetting("", 0, 0, 0, true), ColumnSetting.builder()
                                    .add("S.ID", Math.max(0, (list.get(list.size()-1).toString().length()-3)/2), true)
                                    .add("IP Address: Port", 2)
                                    .add("Entry Time", 2)
                                    .add("Exit Time", 2)
                                    .add("Last Talk", 2)
                                    .add("User Name", 3)
                                    .add("Host Name", 3)
                                    .add("Nick Name", 2)
                                    .add("Group Name", 2)
                                    .add("Friend")
                                    .add("B.L.")
                                    .finish(), 0,
                            i->"",
                            (r, key, textRef) -> {
                                //DBから取得
                                @Nullable IPMEntity_Address a = db.publicDao().getUser(key); //WorkerThreadで動いているため問題ない

                                if (a == null) { //DBから取得できなかった
                                    Arrays.fill(textRef, "error");
                                } else {
                                    //配列に値を埋める
                                    int c = 0;
                                    textRef[c++] = String.valueOf(a.sessionID);
                                    textRef[c++] = NetUtil.toString(a.ip, a.port);
                                    textRef[c++] = DateUtil.toEasyString(a.entryTime);
                                    textRef[c++] = (a.exitTime == Long.MIN_VALUE ? "online" : DateUtil.toEasyString(a.exitTime));
                                    textRef[c++] = DateUtil.toEasyString(a.lastTime);
                                    textRef[c++] = a.userName;
                                    textRef[c++] = a.hostName;
                                    textRef[c++] = a.nickName;
                                    textRef[c++] = a.groupName;
                                    textRef[c++] = (a.friend ? "★" : "");
                                    textRef[c++] = (a.blackList ? "●" : "");
                                }
                            }));

                    break;

                case MESSAGE_TABLE:
                    //主キーのみ全件取得
                    messageIDList = db.asyncMessageIDs();
                    //任意のセルのBindData
                    messageIDList.observe(getViewLifecycleOwner(), list -> bind.dumpTableView.setData(list, new ColumnSetting("", 0, 0, 0, true), ColumnSetting.builder()
                                    .add("M.ID", Math.max(0, (list.get(list.size()-1).toString().length()-3)/2), true)
                                    .add("S.ID", 1)
                                    .add("Owner", 0, s.plus1)
                                    .add("Packet", 2, 0, s.minus1)
                                    .add("Commit Date" )
                                    .add("Read Date", 1)
                                    .add("Packet when ReadCheck")
                                    .add("Message", 2, 0, s.minus2)
                                    .add("Show",  0, s.plus1)
                                    .finish(), 1,
                            i->"",
                            (r, key, textRef) -> {
                                //DBから取得
                                @Nullable IPMEntity_Message m = db.publicDao().getMessage(key); //WorkerThreadで動いているため問題ない

                                if (m == null) { //DBから取得できなかった
                                    Arrays.fill(textRef, "error");
                                } else {
                                    //配列に値を埋める
                                    int c = 0;
                                    textRef[c++] = String.valueOf(m.messageID);
                                    textRef[c++] = String.valueOf(m.sessionID);
                                    textRef[c++] = (m.owner ? "o" : "");
                                    textRef[c++] = String.valueOf(m.packetNo);
                                    textRef[c++] = (m.commitDate == null ? null : (m.commitDate == Long.MAX_VALUE ? "Un-commit" : DateUtil.toEasyString(m.commitDate) ));
                                    textRef[c++] = (m.readDate == null ? null : (m.readDate == Long.MAX_VALUE ? "Un-read" :DateUtil.toEasyString(m.readDate) ));
                                    textRef[c++] = MyUtil.requireNonNull(m.packetNoWhenReadCheck, i->i.toString(), "");
                                    textRef[c++] = MyUtil.substring(m.msg.replace(BR, ""), 13) + (m.msg.length()<15 ? "" : "...");
                                    textRef[c++] = (m.showDisplay ? "o" : "");
                                }
                            }));

                    break;
                case ATTACH_FILE_TABLE:
                    //主キーのみ全件取得
                    fileKeyList = db.asyncAttachFileKeys();
                    //任意のセルのBindData
                    fileKeyList.observe(getViewLifecycleOwner(), list -> bind.dumpTableView.setData(list, new ColumnSetting("", 0, 0, 0, true), ColumnSetting.builder()
                                    .add("M.ID", Math.max(0, (Long.toString(list.get(list.size()-1).messageID).length()-3)/2), true)
                                    .add("File ID", 2, true)
                                    .add("Uri", 12, 0, s.minus2)
                                    .add("Modified", 0, s.minus1)
                                    .add("File Name", 3)
                                    .add("File Size")
                                    .add("File Attr", 3, 0, s.minus1)
                                    .add("ExAttr")
                                    .add("Completed", 3)
                                    .add("Released", s.minus2, s.plus1)
                                    .finish(), 0,
                            i->"",
                            (r, keys, textRef) -> {
                                //DBから取得
                                @Nullable IPMEntity_AttachFile f = db.publicDao().getAttachFile(keys.messageID, keys.fileID); //WorkerThreadで動いているため問題ない

                                if (f == null) { //DBから取得できなかった
                                    Arrays.fill(textRef, "error");
                                } else {
                                    //配列に値を埋める
                                    int c = 0;
                                    textRef[c++] = String.valueOf(f.messageID);
                                    textRef[c++] = Integer.toHexString(f.fileID);
                                    textRef[c++] = (f.uri == null ? null : f.uri.toString());
                                    textRef[c++] = DateUtil.toEasyString(f.modifiedTimeSec*1000);
                                    textRef[c++] = f.fileName;
                                    textRef[c++] = Formatter.formatFileSize(requireContext(), f.fileSize);
                                    textRef[c++] = FILE_ATTR_MODE.toString(f.fileAttribute);
                                    textRef[c++] = f.exAttr;
                                    textRef[c++] = "Date: " + (f.completedDate==null ? null : DateUtil.toEasyString(f.completedDate)) + BR +
                                                    "ByteSum: " + (f.completedByteSum==null ? null : Formatter.formatFileSize(requireContext(), f.completedByteSum));
                                    textRef[c++] = (f.releaseMark ? "x" : "");
                                }
                            }));

                    break;
            }
        });

        return bind.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //オプションメニュー
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuProvider.super.onPrepareMenu(menu);

                menu.add(0, R.string.action_refresh, 0, R.string.action_refresh).setIcon(android.R.drawable.ic_menu_rotate).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.add(0, R.string.action_change, 0, R.string.action_change).setIcon(android.R.drawable.ic_menu_more).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.string.action_refresh) {
                    bind.dumpTableView.notifyDataSetChanged();
                    return true;
                } else if (itemId == R.string.action_change) {
                    final @NonNull CharSequence[] tables = new CharSequence[]{TABLE.ADDRESS_TABLE.name(), TABLE.MESSAGE_TABLE.name(), TABLE.ATTACH_FILE_TABLE.name()};
                    new AlertDialog.Builder(requireContext())
                            .setItems(tables, (dialog, which) -> {
                                vm.liveSelectedTable.postValue(TABLE.valueOf(tables[which].toString()));
                                dialog.dismiss();
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); //Fragmentでaddする時の作法

    }

    @Override
    public void onDestroyView() {
        //フラグメントの流儀
        vm = null;
        bind = null;

        super.onDestroyView();
    }

}