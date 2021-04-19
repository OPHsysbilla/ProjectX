package autoexclue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import am.widget.wraplayout.R;

/**
 * Created by lei.jialin on 2021/4/19
 */
public class ExerciseCementItem extends CementItem<ExerciseCementItem.ViewHolder> {
    private final String str;
    public ExerciseCementItem(String s) {
        StringBuilder ss=new StringBuilder();
        for (int i = 0; i < 10; i++) {
            ss.append(s);
        }
        this.str = ss.toString();
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_cement_test;
    }

    @Override
    void onBindViewHolder(AutoExcludeLayout.ViewHolder vh, ViewGroup parent) {
        if(vh instanceof ExerciseCementItem.ViewHolder) {
            ((ViewHolder) vh).tv.setText(str);
        }
    }

    public static class ViewHolder extends AutoExcludeLayout.ViewHolder {
        private TextView tv;
        ViewHolder(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tv_item_simple_title);
        }
    }
}
