package com.dean.dragview;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.dean.dragview.data.Cheeses;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private DragLayout mDragViewLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDragViewLayout = (DragLayout) findViewById(R.id.dv_container);
        final ImageView ivHead = (ImageView) findViewById(R.id.iv_head);
        final ListView lvMain = (ListView) findViewById(R.id.lv_main);
        final ListView lvMenu = (ListView) findViewById(R.id.lv_menu);
        mDragViewLayout.setOnStatusChangedListener(new DragLayout.OnStatusChangedListener() {
            @Override
            public void onOpened(View view) {
                //菜单被打开
            }

            @Override
            public void onClosed(View view) {
                //菜单被关闭
                //主面板头像左右晃动
                ObjectAnimator animator=ObjectAnimator.ofFloat(ivHead,"translationX",15);
                animator.setInterpolator(new CycleInterpolator(5));
                animator.setDuration(500);
                animator.start();
            }

            @Override
            public void onStatusChanging(View view, float percent) {
                //正在拖动菜单面板
                ivHead.setAlpha(1 - percent+0.3f);
            }
        });
        lvMain.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Cheeses.NAMES));
        lvMenu.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Cheeses.sCheeseStrings) {
            //修改菜单栏的文字为白色
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view=super.getView(position, convertView, parent);
                if (view != null && view instanceof TextView) {
                    TextView tvText= (TextView) view;
                    tvText.setTextColor(Color.WHITE);
                }
                return view;
            }
        });
    }

    public void switchMenu(View view) {
        mDragViewLayout.togglePanel();
    }
}
