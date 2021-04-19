/*
 * Copyright (C) 2018 AlexMofer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package am.project.x.business.widgets.wraplayout;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import am.widget.wraplayout.RandomUtils;
import androidx.annotation.Nullable;

import am.appcompat.app.BaseActivity;
import am.project.x.R;
import am.widget.wraplayout.WrapLayout;
import autoexclue.AutoExcludeLayout;
import autoexclue.CementItem;
import autoexclue.ExerciseCementItem;

/**
 * 自动换行布局
 */
public class WrapLayoutActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener {

    private AutoExcludeLayout mVContent;
    private View other;

    public WrapLayoutActivity() {
        super(R.layout.activity_wraplayout);
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, WrapLayoutActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSupportActionBar(R.id.wl_toolbar);
        mVContent = findViewById(R.id.wl_wl_content);
        other = findViewById(R.id.ohter_layout);
        final RadioGroup gravity = findViewById(R.id.wl_rg_gravity);
        final SeekBar horizontal = findViewById(R.id.wl_sb_horizontal);
        final SeekBar vertical = findViewById(R.id.wl_sb_vertical);
        final SeekBar height = findViewById(R.id.wl_sb_height);

        gravity.setOnCheckedChangeListener(this);
        gravity.check(R.id.wl_rb_top);
        horizontal.setOnSeekBarChangeListener(this);
        horizontal.setProgress(15);
        vertical.setOnSeekBarChangeListener(this);
        vertical.setProgress(15);
        height.setOnSeekBarChangeListener(this);
        height.setProgress(15);
        initAutoLayout();
    }

    private void initAutoLayout() {
        adapter = new AutoExcludeLayout.Adapter();
        mVContent.setAdapter(adapter);
        generateData();
    }

    private void generateData() {
        adapter.clearData();
        List<CementItem<?>> a = new ArrayList<>();
        int size = RandomUtils.nextInt(1, 12);
        for (int i = 0; i < size; i++) {
            a.add(new ExerciseCementItem("titele: " + RandomUtils.nextDouble()));
        }
        adapter.addDataList(a);
    }

    AutoExcludeLayout.Adapter adapter;

    // Listener
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.wl_rb_top:
                mVContent.setGravity(WrapLayout.GRAVITY_TOP);
                break;
            case R.id.wl_rb_center:
                mVContent.setGravity(WrapLayout.GRAVITY_CENTER);
                break;
            case R.id.wl_rb_bottom:
                mVContent.setGravity(WrapLayout.GRAVITY_BOTTOM);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.wl_sb_horizontal:
                mVContent.setHorizontalSpacing(
                        (int) (progress * getResources().getDisplayMetrics().density));
                break;
            case R.id.wl_sb_vertical:
                mVContent.setVerticalSpacing(
                        (int) (progress * getResources().getDisplayMetrics().density));
                break;
            case R.id.wl_sb_height:
                ViewGroup.LayoutParams lp = other.getLayoutParams();
                float p = progress * 1.0f / 100f;
                lp.height = (int) (p * getResources().getDisplayMetrics().density * 1000);
                other.setLayoutParams(lp);
                other.requestLayout();
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
