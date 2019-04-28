package org.farriswheel.glovecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnectedThread extends Thread {

    private final String TAG = "BLUETOOTHCONNECT";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final BluetoothAdapter mmAdapter;
    private final Context mContext;


    private byte[] mmBuffer;
    private byte[] writeBuffer;

    public BluetoothConnectedThread(BluetoothDevice device, Context context)
    {

        mContext = context;      //context
        mmDevice = device;       //bluetooth device connected
        mmBuffer = new byte[8];  //8-byte buffer of serial data from the device

        BluetoothSocket tmp = null;
        ParcelUuid [] uuidArray = mmDevice.getUuids();

        try
        {
            UUID use = UUID.fromString(uuidArray[0].toString());
            tmp = mmDevice.createRfcommSocketToServiceRecord(use);
        } catch(IOException e)
        {
            Log.e(TAG, "Error getting uuid");
        }

        mmAdapter = BluetoothAdapter.getDefaultAdapter(); //use default (built in) bluetooth device
        mmSocket = tmp;

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = mmSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error creating Input Stream", e);
        }

        try {
            tmpOut = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error creating Output Stream", e);
        }
        mmInStream = tmpIn;
        mmOutStream = tmpOut;

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, new IntentFilter("outgoingToGlove"));
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            write(intent.getByteArrayExtra("bytesToGlove"));
        }
    };

    public void run()
    {
        mmAdapter.cancelDiscovery();

        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            try {
                mmSocket.close();

            } catch (IOException closeException) {
            }
        }

    }

    public void write(byte[] bytesOut) {
        try {
            if(bytesOut != null)
            {
                mmOutStream.write(bytesOut);
            }

        } catch (IOException e) {
           Log.e(TAG, "Output Stream disconnect", e);
        }
   }

    public void cancel() {
       try {
           mmSocket.close();
       } catch (IOException e) {
           Log.e(TAG, "COULDN'T CLOSE SOCKET", e);
       }
   }
}