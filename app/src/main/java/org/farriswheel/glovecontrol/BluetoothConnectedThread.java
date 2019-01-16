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

/**
 * Created by will on 7/20/18.
 */

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

    public BluetoothConnectedThread(BluetoothDevice device, Context context) { //BluetoothSocket socket) {

        mContext = context;
        mmDevice = device;
        mmBuffer = new byte[8];

        BluetoothSocket tmp = null;
        ParcelUuid [] uuidArray = mmDevice.getUuids();

        try
        {
            UUID use = UUID.fromString(uuidArray[0].toString());
            tmp = mmDevice.createRfcommSocketToServiceRecord(use);
        } catch(IOException e)
        {

        }

        mmAdapter = BluetoothAdapter.getDefaultAdapter();
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


    public void writeAfterResponse(byte [] bytesOut)
    {
        if(bytesOut != null) {
            try {
                int numBytes = mmInStream.read(mmBuffer);
                if (mmBuffer[0] == (byte) 0x0F) {
                    mmOutStream.write(bytesOut);
                }
            } catch (IOException e2) {
            }
        }

    }

    public void write(byte[] bytesOut) {
        try {
            if(bytesOut != null)
            {
                mmOutStream.write(bytesOut);
                Log.d(TAG, "Sending " + bytesToHex(bytesOut));
            }

        } catch (IOException e) {
           Log.e(TAG, "Output Stream disconnect", e);
        }
   }

    public String bytesToHex(byte[] bytes) {
       char[] hexArray = "0123456789ABCDEF".toCharArray();
       char[] hexChars = new char[bytes.length * 2];
       for (int j = 0; j < bytes.length; j++) {
           int v = bytes[j] & 0xFF;
           hexChars[j * 2] = hexArray[v >>> 4];
           hexChars[j * 2 + 1] = hexArray[v & 0x0F];
       }
       return new String(hexChars);
   }

    public void cancel() {
       try {
           mmSocket.close();
       } catch (IOException e) {
           Log.e(TAG, "COULDN'T CLOSE SOCKET", e);
       }
   }
}