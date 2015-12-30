package com.led.bear.ledcontrol;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;


public class MainActivity extends FragmentActivity {

    private Button button;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // configure the SlidingMenu
        // 设置左滑菜单
        SlidingMenu menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        // 设置滑动的屏幕范围，该设置为全屏区域都可以滑动
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        // 设置阴影图片
        // menu.setShadowDrawable(R.drawable.menu_color);
        // 设置阴影图片的宽度
        // menu.setShadowWidthRes(R.dimen.shadow_width);

        // SlidingMenu划出时主页面显示的剩余宽度
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        // SlidingMenu滑动时的渐变程度
        menu.setFadeDegree(0.35f);
        /**
         * SLIDING_WINDOW will include the Title/ActionBar in the content
         * section of the SlidingMenu, while SLIDING_CONTENT does not.
         */
        //使SlidingMenu附加在Activity上
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        //为侧滑菜单设置布局
        menu.setMenu(R.layout.layout_configmenu);



        button =(Button)findViewById(R.id.button2);
        button.setOnClickListener(button_click);
    }

    View.OnClickListener button_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.button2:
                    // Intent serverIntent = new Intent(MainActivity.this,DeviceListActivity.class);
                    // startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                    Intent bluetooth = new Intent();
                    bluetooth.setClass(MainActivity.this, DeviceListActivity.class);
                    startActivity(bluetooth);
                    break;
                default:
                    break;
            }
        }
    };
}
