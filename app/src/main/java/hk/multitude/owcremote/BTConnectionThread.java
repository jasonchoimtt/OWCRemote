package hk.multitude.owcremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;

/**
 * Created by jason on 12/3/15.
 */
public class BTConnectionThread extends Thread {
    private final BluetoothAdapter mBt;
    private final BluetoothDevice mDevice;
    private final Handler mHandler;
    private BluetoothSocket mSocket;
    private final UUID MY_UUID = UUID.fromString("312e99d4-c8c4-11e4-8731-1681e6b88ec1");

    public static final int CONNECTION_FAILED = 0;
    public static final int CONNECTED = 1;
    public static final int DISCONNECTED = 2;
    public static final int BT_MESSAGE = 10;
    private InputStream mInput;
    private OutputStream mOutput;

    public BTConnectionThread(BluetoothAdapter bt, BluetoothDevice device, Handler handler) {
        super();
        mBt = bt;
        mDevice = device;
        mHandler = handler;

        /*try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (Exception e) {
            mSocket = null;
            e.printStackTrace();
        }*/
        try {
            // http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3
            // For heaven's sake
            mSocket = (BluetoothSocket) mDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
        } catch (Exception er) {
            mSocket = null;
            er.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (mSocket == null) {
            mHandler.obtainMessage(CONNECTION_FAILED).sendToTarget();
        }
        mBt.cancelDiscovery();
        try {
            mSocket.connect();
            mInput = mSocket.getInputStream();
            mOutput = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                mSocket.close();
            } catch (IOException er) {
                er.printStackTrace();
            }
            mHandler.obtainMessage(CONNECTION_FAILED).sendToTarget();
            return;
        }

        mHandler.obtainMessage(CONNECTED).sendToTarget();

        Scanner in = new Scanner(mInput);
        in.useDelimiter(";|\r?\n|\r");
        String line = null;

        while (true) {
            if (interrupted())
                break;
            try {
                line = in.next();
                Log.d("BT", line);
                mHandler.obtainMessage(BT_MESSAGE, line).sendToTarget();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                break;
            } catch (NoSuchElementException e) {
                break;
            }
        }

        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mHandler.obtainMessage(DISCONNECTED).sendToTarget();
    }

    public void close() {
        interrupt();
        try {
            if (mSocket.isConnected())
                mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void write(String message) {
        try {
            mOutput.write(message.getBytes());
            Log.d("BTConnectionThread", "Output: "+message);
        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
        /*try {
            sleep(100);
        } catch (InterruptedException e) {
        }*/
    }

    public synchronized void flush() {
        try {
            mOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
    }

    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected();
    }
}
