package com.led.bear.ledcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;


public class MainActivity extends FragmentActivity {

    private Button button;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService;

    byte[] send = {(byte)0xAA,1,100,(byte)128,(byte)0xBB};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获得本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        // 不支持蓝牙设备
        if (mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            MainActivity.this.finish();
        }

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
        button =(Button)findViewById(R.id.button3);
        button.setOnClickListener(button_click);

        SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                    Toast.makeText(MainActivity.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                    return;
                }
                send[2] = (byte) seekBar.getProgress();
                mBluetoothService.write(send);
            }
        });
        seekBar = (SeekBar)findViewById(R.id.seekBar2);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                    Toast.makeText(MainActivity.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                    return;
                }
                send[3] = (byte) seekBar.getProgress();
                mBluetoothService.write(send);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        // 判断蓝牙是否可用
        if (!mBluetoothAdapter.isEnabled()) {
            //如果蓝牙没有打开 请求打开蓝牙
            // 用于发送启动请求的 Intent
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // 这里会启动系统的一个Activity
            // 然后也会根据REQUEST_ENABLE_BT在OnActivityResult方法里处理
            startActivityForResult(enableIntent, 0);
        } else if (mBluetoothService == null) {
            mBluetoothService = new BluetoothService(MainActivity.this,mHandler);
        }
    }

    private final  Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    // String readMessage = new String(readBuf, 0, msg.arg1);
                    // mBluetoothService.write(readBuf);
                    break;
            }
        }
    };

    View.OnClickListener button_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.button2:
                    // Intent serverIntent = new Intent(MainActivity.this,DeviceListActivity.class);
                    // startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                    Intent bluetooth = new Intent();
                    bluetooth.setClass(MainActivity.this, DeviceListActivity.class);
                    //startActivity(bluetooth);
                    startActivityForResult(bluetooth,REQUEST_CONNECT_DEVICE_SECURE);
                    break;
                case R.id.button3:
                    Button button = (Button) findViewById(R.id.button3);
                    if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        Toast.makeText(MainActivity.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(send[1] == 0) {
                        button.setBackgroundResource(R.mipmap.led_open);
                        send[1] = 1;
                    }else {
                        button.setBackgroundResource(R.mipmap.led_close);
                        send[1] = 0;
                    }
                    mBluetoothService.write(send);
                    break;
                default:
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if(resultCode == Activity.RESULT_OK){
                    connectDevice(data);
                }
                break;
            default:break;
        }
    }

    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBluetoothService.connect(device);
    }


}
