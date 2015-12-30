package com.led.bear.ledcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Bear on 2015/12/30.
 *
 */
public class BluetoothService {
    // Debugging
    private static final String TAG = "BluetoothChatService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";

    // Unique UUID for this application
    // 增加单片机 UUID 可以与 HC-05 通信
    private static final UUID MCU_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }


    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     *
     */
    public synchronized void connect(BluetoothDevice device) {

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write un synchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // BUG：此处加上start 重新开始监听模式
        // 当目前存在一个连接时 新的连接会被关闭
        // 导致需要连接两次才能建立新的链接
        // 在已连接线程中解决该问题

        // Start the service over to restart listening mode
        // BluetoothChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        // 本地的服务端socket
        private final BluetoothServerSocket mmServerSocket;
        // 连接类型
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            // 创建一个用于监听的服务端socket
            // NAME参数没关系
            // MY_UUID是确定唯一通道的标示符
            // 用于连接的socket也要通过它产生
            try {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MCU_UUID_SECURE);
            } catch (IOException e) {

            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            // 监听到连接上为止
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    // accept 阻塞
                    // 要不就是成功地建立一个连接  要不就是返回一个异常
                    socket = mmServerSocket.accept();
                } catch (IOException e) {

                    break;
                }

                // If a connection was accepted
                // 连接建立成功
                if (socket != null) {
                    // 同步块  同一时间  只有一个线程可以访问该区域
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                // 状态正常  开始进行线程通信
                                // 实际就是开启通信线程ConnectedThread
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                // 未准备或已连接状态  关闭新建的这个socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    break;
                                }
                                break;
                        }
                    }
                }
            }

        }

        public void cancel() {
            //关闭服务端的socket
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            // 通过远程设备以及唯一的UUID创建一个用于连接的socket
            try {
                tmp = device.createRfcommSocketToServiceRecord(
                        MCU_UUID_SECURE);
            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        public void run() {
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            // 一定要停止扫描 不然会减慢连接速度
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            // 连接到服务端的socket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                // connect方法也会造成阻塞 直到成功连接 或返回一个异常
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                // 出现异常 关闭socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                }
                // 连接失败发送要Toast的消息
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            // 重置连接线程，因为我们已经完成了
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            // 开始进行线程通信  开启通信线程ConnectedThread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            // 关闭连接用的socket
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private boolean listenFlag = true;

        public ConnectedThread(BluetoothSocket socket) {

            // 这个是之前的用于连接的socket
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            // 从连接的socket里获取输入输出流
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer = new byte[5];
            byte[] send_buffer;
            int bytes = 0;
            boolean flag = false;

            // Keep listening to the InputStream while connected
            // 已经连接上以后持续从通道中监听输入流的情况
            while (true) {
                try {
                    // Read from the InputStream
                    // 从通道的输入流InputStream中读取数据到buffer数组中
                    byte temp = (byte)mmInStream.read();
                    if(temp == 0x0A) {
                        flag = true;
                        bytes = 0;
                    }
                    if(flag){
                        buffer[bytes++] = temp;
                        if(bytes >= 4){
                            send_buffer = buffer;
                            // Send the obtained bytes to the UI Activity
                            // 将获取到数据的消息发送到UI界面，同时也把内容buffer发过去显示
                            mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, send_buffer)
                                    .sendToTarget();
                            flag = false;
                            bytes = 0;
                        }
                    }


                } catch (IOException e) {

                    // 连接异常断开的时候发送一个需要Toast的消息
                    connectionLost();

                    // 判断线程退出时是否进入监听
                    if(listenFlag)
                        BluetoothService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            // 这个方法用于把发送内容写到通道的OutputStream中
            // 会在发信息是被调用
            try {
                // 将buffer内容写进通道
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                // 用于将自己发送给对方的内容也在UI界面显示
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {

            }
        }

        public void cancel() {
            // 关闭socket  即关闭通道
            try {
                // 线程退出时不进入监听
                listenFlag = false;
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }
}
