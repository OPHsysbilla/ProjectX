package autoexclue;


import android.view.ViewGroup;

import am.widget.wraplayout.R;

/**
 * Created by lei.jialin on 2021/4/19
 */
public abstract class CementItem<T>{
    abstract public int getLayoutId() ;

    abstract void onBindViewHolder(AutoExcludeLayout.ViewHolder vh, ViewGroup parent);
}
