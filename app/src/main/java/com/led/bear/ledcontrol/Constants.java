package com.led.bear.ledcontrol;

/**
 * Created by Bear on 2015/12/30.
 *
 */
public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    // 从BluetoothChatService传回来交给Handler处理的消息类型\

    // 蓝牙socket状态改变，有4个状态  监听、正在连接、已连接、无
    public static final int MESSAGE_STATE_CHANGE = 1;
    // 蓝牙服务socket在读别的设备发来的内容
    public static final int MESSAGE_READ = 2;
    // 蓝牙socket正在写要发送的内容
    public static final int MESSAGE_WRITE = 3;
    // 蓝牙连接了一个设备且获得了设备名字
    public static final int MESSAGE_DEVICE_NAME = 4;
    // 有需要用Toast控件广播的内容
    public static final int MESSAGE_TOAST = 5;


    // 获取设备名时的键值
    public static final String DEVICE_NAME = "device_name";
    // 获取广播内容的键值
    public static final String TOAST = "toast";
}
