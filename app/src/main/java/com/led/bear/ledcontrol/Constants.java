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

    // frame head  0xAA
    // frame command
    //        query    0x00
    //        mode 1     0x01  mode 2     0x02
    //        mode 3     0x03  breath     0x04
    //        set mode1  0x05  set mode2  0x06
    //        set mode3  0x07
    //        set breath limit1  0x08
    //        set breath limit2  0x09
    //        open 0x0A  close  0x0B direct control 0x0c
    // bright  0x00 - 0x64
    // color temperature  0x00-0xFF   middle 0x80
    // frame tail 0xBB

    public static final byte FRAME_HEAD = (byte) 0XAA;
    public static final byte FRAME_TAIL = (byte) 0XBB;
    public static final byte DEFAULT_BRIGHTNESS = 0X64;
    public static final byte DEFAULT_COLOR_TEMPRATURE = (byte) 0X80;
    public static final byte CMD_OPEN = 0X0A;
    public static final byte CMD_CLOSE = 0X0B;
    public static final byte CMD_DIRECT_CONTROL = 0X0C;
    public static final byte CMD_QUERY = 0X00;
    public static final byte CMD_MODE1 = 0X01;
    public static final byte CMD_MODE2 = 0X02;
    public static final byte CMD_MODE3 = 0X03;
    public static final byte CMD_BREATH_MODE = 0X04;
    public static final byte CMD_MODE1_SAVE = 0X05;
    public static final byte CMD_MODE2_SAVE = 0X06;
    public static final byte CMD_MODE3_SAVE = 0X07;
    public static final byte CMD_BREATH_LIMIT1 = 0X08;
    public static final byte CMD_BREATH_LIMIT2 = 0X09;


}
