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
import java.net.ConnectException;
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

    private final byte OK_SEND = 0x12;

    public BluetoothConnectedThread(BluetoothDevice device, Context context)
    {

        mContext = context;      //context
        mmDevice = device;       //bluetooth device connected
        mmBuffer = new byte[15];  //buffer of serial data from the device

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

        if(!mmSocket.isConnected())
        {
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();

                } catch (IOException closeException) {
                }
            }
        }
        /*while(true)
        {
            try {
                if (mmBuffer != null) {
                    mmInStream.read(mmBuffer);
                    Log.d(TAG, "Received: " + bytesToHexString(mmBuffer));
                }
            } catch (IOException e)
            {
                Log.e(TAG, "Diconnect", e);
            }
        }*/

    }

    public void write(byte[] bytesOut) {
        byte [] buf = new byte[2];
        try {
            if(bytesOut != null)
            {
                while(buf[0] != 0xFF && buf[1] != 0xCC)
                    mmInStream.read(buf);
                mmOutStream.write(bytesOut);
                Log.d(TAG, "Received: " + bytesToHexString(buf) + "\nSent: " + bytesToHexString(bytesOut));
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

   public String bytesToHexString(byte [] input)
   {
       StringBuilder sb = new StringBuilder();
       for (byte b : input) {
           sb.append(String.format("%02X ", b));
       }
       return sb.toString();
   }
}