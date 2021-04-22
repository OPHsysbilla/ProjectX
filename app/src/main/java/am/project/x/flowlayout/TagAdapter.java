package am.project.x.flowlayout;

import android.util.Log;
import android.view.View;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TagAdapter<T> {
    private List<ChoiceGroup<T>> mTagDatas;
    private OnDataChangedListener mOnDataChangedListener;
    @Deprecated
    private HashSet<Integer> mCheckedPosList = new HashSet<Integer>();


    public void setDatas(List<ChoiceGroup<T>> mTagDatas) {
        this.mTagDatas = mTagDatas;
    }

    public Boolean isSelectAt(int groupIndex, int position) {
        return false;
    }

    interface OnDataChangedListener {
        void onChanged();
    }

    void setOnDataChangedListener(OnDataChangedListener listener) {
        mOnDataChangedListener = listener;
    }

    public void setSelectedList(int... poses) {
        Set<Integer> set = new HashSet<>();
        for (int pos : poses) {
            set.add(pos);
        }
        setSelectedList(set);
    }

    public void setSelectedList(Set<Integer> set) {
        mCheckedPosList.clear();
        if (set != null) {
            mCheckedPosList.addAll(set);
        }
        notifyDataChanged();
    }

    HashSet<Integer> getPreCheckedList() {
        return mCheckedPosList;
    }


    public int getCount() {
        return mTagDatas == null ? 0 : mTagDatas.size();
    }

    public void notifyDataChanged() {
        if (mOnDataChangedListener != null)
            mOnDataChangedListener.onChanged();
    }

    public ChoiceGroup<T> getItem(int position) {
        return mTagDatas.get(position);
    }

    public abstract View getView(int choiceGroupIndex, FlowLayout parent, int position);


    public void onSelected(int position, View view){
        Log.d("zhy","onSelected " + position);
    }

    public void unSelected(int position, View view){
        Log.d("zhy","unSelected " + position);
    }

    public boolean setSelected(int choiceGroupIndex, int position) {
        return false;
    }


}
