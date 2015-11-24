package com.corporation.tvm.mbi2handin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

/**
 * Created by Lubomir on 24.11.2015.
 */

public class Bluetooth extends AppCompatActivity {
    // Debugging
    private static final String TAG = "TVM";

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothArduino";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private SetupConnectingThread mConnectingThread;
    private CommunicationThread mCommunicatingThread;

    //Constructor with context and Handler for callback
    public Bluetooth(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
    }

    public void enableBt(){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 9);
    }

    public void disableBt(){
        if(mAdapter.isDiscovering())
            mAdapter.cancelDiscovery();
        mAdapter.disable();
    }

    //Given a device name, this method will look among bonded devices, and
    //try connecting to it. It will only find/connect to bonded devices
    public void connectDevice(String deviceName) {
        String address = null;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (BluetoothDevice d : adapter.getBondedDevices()) {
            if (d.getName().equals(deviceName)) address = d.getAddress();
        }
        try {
            BluetoothDevice device = adapter.getRemoteDevice(address); // Get the BluetoothDevice object
            this.connect(device); // Attempt to connect to the device
        } catch (Exception e) {
            Log.e("Unable to connect to:" + address, e.getMessage());
        }
    }

    public boolean isBtEnabled() {
        if (mAdapter.isEnabled())
            return true;
        else
            return false;
    }

    //Start the ConnectingThread to initiate a connection to a remote device.
    private synchronized void connect(BluetoothDevice device) {

        Log.d("Bluetooth", "Starting ConnectingThread to: " + device);

        // Cancel any thread currently running a connection
        if (mCommunicatingThread != null) {
            mCommunicatingThread.cancel();
            mCommunicatingThread = null;
        }

        // Start the thread to connect with the given device
        mConnectingThread = new SetupConnectingThread(device);
        mConnectingThread.start();
    }


    //This thread runs while attempting to make an outgoing connection with a
    //device. It runs straight through; the connection either succeeds or fails.
    private class SetupConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public SetupConnectingThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectingThread SocketType:" + mSocketType);
            setName("SetupConnectingThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "Unable to connect socket ", e);
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType
                            + " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the SetupConnectingThread because we're done
            synchronized (Bluetooth.this) {
                mConnectingThread = null;
            }

            // Start the communication thread
            setupCommunicationThread(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType
                        + " socket failed", e);
            }
        }
    }


    //Start the CommunicationThread to begin managing a Bluetooth connection
    public synchronized void setupCommunicationThread(BluetoothSocket socket, BluetoothDevice device, final String socketType) {

        Log.d(TAG, "setupCommunicationThread, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectingThread != null) {
            mConnectingThread.cancel();
            mConnectingThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mCommunicatingThread = new CommunicationThread(socket, socketType);
        mCommunicatingThread.start();

        // Send the name of the setupCommunicationThread device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString("Connected", device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    //Stop all threads
    public synchronized void stop() {
        Log.d(TAG, "stop");
        if (mConnectingThread != null) {
            mConnectingThread.cancel();
            mConnectingThread = null;
        }
        if (mCommunicatingThread != null) {
            mCommunicatingThread.cancel();
            mCommunicatingThread = null;
        }
    }


    //Write to the CommunicationThread in an unsynchronized manner
    private void write(byte[] out) {
        // Create temporary object
        CommunicationThread r;
        // Synchronize a copy of the CommunicationThread
        synchronized (this) {
            r = mCommunicatingThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    //Indicate that the connection attempt failed and notify the UI Activity.
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("Toast", "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    //Indicate that the connection was lost and notify the UI Activity.
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("Toast", "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    //This thread runs during a connection with a remote device. It handles all
    //incoming and outgoing transmissions.
    private class CommunicationThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public CommunicationThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create CommunicationThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mCommunicatingThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while setupCommunicationThread
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Log.d(TAG, "message bytes " + bytes);
                    Log.d(TAG, "message string bytes " + String.valueOf(bytes));
                    Log.d(TAG, "message buffer " + new String(buffer));
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        //Write to the setupCommunicationThread OutStream.
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public void sendMessage(String message) {
        // Check that there's actually something to send
        if (message.length() > 0) {
        //    char EOT = (char) 3;
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            this.write(send);
        }
    }
}
