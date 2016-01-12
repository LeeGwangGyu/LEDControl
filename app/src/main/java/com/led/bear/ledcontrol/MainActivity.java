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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends FragmentActivity {


    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private List<String> list = new ArrayList<String>();

    byte[] send = {(byte)0xAA,1,100,(byte)128,(byte)0xBB};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init_BT();
        setup_UI();
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

    private void init_BT(){
        // 获得本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        // 不支持蓝牙设备
        if (mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this,
                    "Bluetooth is not available", Toast.LENGTH_LONG).show();
            MainActivity.this.finish();
        }

        if (mBluetoothService == null) {
            mBluetoothService = new BluetoothService(MainActivity.this,mHandler);
        }
    }

    private void setup_UI() {

        setSlidingMenu();


        ArrayAdapter<String> modeArrayAdapter =
                new ArrayAdapter<String>(this, R.layout.device_name);
        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        spinner.setAdapter(modeArrayAdapter);
        modeArrayAdapter.add("Mode 1");
        modeArrayAdapter.add("Mode 2");
        modeArrayAdapter.add("Mode 3");
        modeArrayAdapter.add("Breath");

        Button button;
        button =(Button)findViewById(R.id.button);
        button.setOnClickListener(button_click);
        button =(Button)findViewById(R.id.mLEDStateControl);
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

    private void setSlidingMenu(){
        // configure the SlidingMenu
        // 设置左滑菜单
        SlidingMenu menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        // 设置滑动的屏幕范围，该设置为全屏区域都可以滑动
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        // SlidingMenu划出时主页面显示的剩余宽度
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        // SlidingMenu滑动时的渐变程度
        menu.setFadeDegree(0.35f);
        //使SlidingMenu附加在Activity上
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        //为侧滑菜单设置布局
        menu.setMenu(R.layout.layout_configmenu);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if(resultCode == Activity.RESULT_OK){
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK){
                    return;
                }
                Toast.makeText(MainActivity.this, R.string.bt_not_enabled_leaving,
                        Toast.LENGTH_SHORT).show();
                MainActivity.this.finish();
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



    private final  Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    Button button;
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            button = (Button) findViewById(R.id.mBluetoothStateShow);
                            button.setBackgroundResource(R.mipmap.connect_bluetooth);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            button = (Button) findViewById(R.id.mBluetoothStateShow);
                            button.setBackgroundResource(R.mipmap.disconnect_bluetooth);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    // String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    // String readMessage = new String(readBuf, 0, msg.arg1);
                    // mBluetoothService.write(readBuf);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDeviceName;
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(MainActivity.this, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                        Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    View.OnClickListener button_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.button:
                    // Intent serverIntent = new Intent(MainActivity.this,DeviceListActivity.class);
                    // startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                    Intent bluetooth = new Intent();
                    bluetooth.setClass(MainActivity.this, DeviceListActivity.class);
                    //startActivity(bluetooth);
                    startActivityForResult(bluetooth,REQUEST_CONNECT_DEVICE_SECURE);
                    break;
                case R.id.mLEDStateControl:
                    if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        Toast.makeText(MainActivity.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Button button = (Button) findViewById(R.id.mLEDStateControl);
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



}



