package autoexclue.adapter;

import androidx.annotation.Nullable;

public abstract class CellAdapterDataObserver {
    public void onChanged() {
        // Do nothing
    }

    public void onItemRangeChanged(int positionStart, int itemCount) {
        // do nothing
    }

    public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
        // fallback to onItemRangeChanged(positionStart, itemCount) if app
        // does not override this method.
        onItemRangeChanged(positionStart, itemCount);
    }

    public void onItemRangeInserted(int positionStart, int itemCount) {
        // do nothing
    }

    public void onItemRangeRemoved(int positionStart, int itemCount) {
        // do nothing
    }

    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        // do nothing
    }

    public void onClearData() {

    }
}