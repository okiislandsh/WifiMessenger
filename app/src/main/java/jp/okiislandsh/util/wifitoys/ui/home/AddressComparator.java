package jp.okiislandsh.util.wifitoys.ui.home;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Comparator;
import java.util.Objects;

import jp.okiislandsh.library.android.net.NetUtil;
import jp.okiislandsh.library.android.net.service.ipmsg.IPMEntity_Address;

public class AddressComparator implements Comparator<IPMEntity_Address>, Parcelable {
    //region parcelable
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        final @NonNull Bundle bundle = new Bundle();
        writeToBundle(this, bundle);
        dest.writeBundle(bundle);
    }
    private static final @NonNull String PARCEL_KEY_KEY = "KEY";
    private static final @NonNull String PARCEL_KEY_ASC = "ASC";
    private static final @NonNull String PARCEL_KEY_CHILD = "CHILD";
    public static void writeToBundle(@NonNull AddressComparator obj, @NonNull Bundle bundle) {
        bundle.putString(PARCEL_KEY_KEY, obj.key.name());
        bundle.putBoolean(PARCEL_KEY_ASC, obj.asc);
        if(obj.child!=null) {
            final @NonNull Bundle childBundle = new Bundle();
            writeToBundle(obj, childBundle);
            bundle.putBundle(PARCEL_KEY_CHILD, childBundle);
        }
    }
    public static final Parcelable.Creator<AddressComparator> CREATOR = new Parcelable.Creator<AddressComparator>() {
        public AddressComparator createFromParcel(Parcel source) {
            return new AddressComparator(source.readBundle(getClass().getClassLoader()));
        }
        public AddressComparator[] newArray(int size) {
            return new AddressComparator[size];
        }
    };
    //endregion

    public enum KEY {NN, GN, IP, ENTRY}
    private final @NonNull KEY key;
    private final boolean asc;
    private final @Nullable AddressComparator child;
    public AddressComparator(@NonNull KEY key, boolean asc, @Nullable AddressComparator child) {
        this.key = key;
        this.asc = asc;
        this.child = child;

        //同じキーが無いか探索
        if(equals(child) ||
                (child!=null &&  (findChild(this, child) || findChild(child, this))) ){
            throw new IllegalArgumentException("自身を子や親に追加することはできない");
        }
    }
    /** Parcelable用 */
    public AddressComparator(@NonNull Bundle bundle) {
        this(KEY.valueOf(bundle.getString(PARCEL_KEY_KEY)),
                bundle.getBoolean(PARCEL_KEY_ASC),
                bundle.containsKey(PARCEL_KEY_CHILD) ? new AddressComparator(bundle.getBundle(PARCEL_KEY_CHILD)) : null);
    }
    /** chainComparator.child内を再帰探索 */
    private static boolean findChild(@NonNull AddressComparator chainComparator, @NonNull AddressComparator find){
        if(chainComparator.key==find.key){
            return true;
        }
        if(chainComparator.child!=null){
            return findChild(chainComparator.child, find);
        }
        return false;
    }
    @Override
    public int compare(IPMEntity_Address o1, IPMEntity_Address o2) {
        return compare(key, o1, o2) * (asc ? 1 : -1);
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof AddressComparator &&
                key==((AddressComparator) obj).key &&
                asc==((AddressComparator) obj).asc &&
                Objects.equals(this.child, ((AddressComparator) obj).child);
    }

    public static int compare(@NonNull KEY key, IPMEntity_Address o1, IPMEntity_Address o2) {
        switch (key){
            case NN: return o1.nickName.compareToIgnoreCase(o2.nickName);
            case GN: return (o2.groupName==null ? Integer.MIN_VALUE : (o1.groupName==null ? Integer.MAX_VALUE : (o1.groupName.compareToIgnoreCase(o2.groupName))));
            case IP: return NetUtil.compareTo(o1.ip, o2.ip);
            case ENTRY: return (int)(o1.entryTime - o2.entryTime); //適当
        }
        return 0;
    }
}
