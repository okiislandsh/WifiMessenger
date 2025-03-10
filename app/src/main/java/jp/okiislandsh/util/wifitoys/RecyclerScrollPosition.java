package jp.okiislandsh.util.wifitoys;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerScrollPosition extends Pair<Integer, Integer> implements Parcelable {
    public final int firstVisiblePositionIndex;
    public final int positionOffset;
    public RecyclerScrollPosition(int firstVisiblePositionIndex, int positionOffset) {
        super(firstVisiblePositionIndex, positionOffset);
        this.firstVisiblePositionIndex = firstVisiblePositionIndex;
        this.positionOffset = positionOffset;
    }

    public static @NonNull RecyclerScrollPosition getScrollPosition(@NonNull RecyclerView recyclerView, @NonNull LinearLayoutManager llm) throws Exception{
        int positionIndex = llm.findFirstVisibleItemPosition();
        final @NonNull View startView = recyclerView.getChildAt(0);
        int positionOffset = (startView == null) ? 0 : (startView.getTop() - recyclerView.getPaddingTop());
        return new RecyclerScrollPosition(positionIndex, positionOffset);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(new int[]{firstVisiblePositionIndex, positionOffset});
    }

    private static @NonNull RecyclerScrollPosition readFromParcel(Parcel in) {
        final @NonNull int[] array = new int[2];
        in.readIntArray(array);
        return new RecyclerScrollPosition(array[0], array[1]);
    }


    public static final Creator<RecyclerScrollPosition> CREATOR = new Creator<RecyclerScrollPosition>() {
        @Override
        public RecyclerScrollPosition createFromParcel(Parcel source) {
            return readFromParcel(source);
        }
        @Override
        public RecyclerScrollPosition[] newArray(int size) {
            return new RecyclerScrollPosition[0];
        }
    };
}
