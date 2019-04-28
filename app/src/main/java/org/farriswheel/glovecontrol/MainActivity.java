package org.farriswheel.glovecontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.madrapps.pikolo.HSLColorPicker;
import com.madrapps.pikolo.listeners.OnColorSelectionListener;

import java.util.Random;


public class MainActivity extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    protected BluetoothAdapter mBluetoothAdapter;        //Set to the default adapter during onCreate()
    protected BluetoothConnectedThread connectedThread;  //Bluetooth connection is handled in this class, which runs in a separate thread
    protected BluetoothConnectedThread leftConnection;
    protected BluetoothConnectedThread rightConnection;

    //Defines the UI elements described in activity_main.xml
    protected TextView fromGlove;
    protected Button connectDevices;
    protected SeekBar fingerIndex;
    protected BroadcastReceiver mReceiver;
    protected Button randomButton;

    protected HSLColorPicker colorWheel;

    protected final int REQUEST_FOR_BT_ADDRESS = 104;

    protected final byte SET_RGB_ALL     = (byte) 0x0A;

    protected final byte BUFFERSIZE = 16;
    protected final byte NUMPIXELS = 5;

    private Random rand;
    byte rainbowIndex = 0;

    private byte [] dataToGlove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataToGlove = new byte[BUFFERSIZE];


        connectedThread = null;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int res = 69; //heh
            startActivityForResult(enableBtIntent, res);
        }

        /*** Initialize UI elements that will be interactive ***/
        connectDevices = (Button) findViewById(R.id.viewPairedDevicesButton);
        connectDevices.setOnClickListener(this);
        fingerIndex = (SeekBar) findViewById(R.id.finger_index_bar);

        fromGlove = (TextView) findViewById(R.id.byteFromBt);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte [] buff = intent.getByteArrayExtra("bytesFromGlove");
               //fromGlove.setText("Result from glove: " + String.valueOf(buff[0]));
            }
        };

        colorWheel = (HSLColorPicker) findViewById(R.id.colorWheel);
        colorWheel.setColorSelectionListener(new OnColorSelectionListener() {
            @Override
            public void onColorSelected(int color) {
                /* if(fingerIndex.getProgress() == 5)
                {
                    sendDataToGlove(SET_RGB_ALL, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, 0, 0);
                } else
                    sendDataToGlove(SET_RGB_SINGLE, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, fingerIndex.getProgress(), 0);
                    */
                for(int i=0;i<NUMPIXELS;++i)
                {
                    dataToGlove[i*3]       = (byte) ((color >> 16) & 0xFF);
                    dataToGlove[(i*3)+1]   = (byte) ((color >> 8)  & 0xFF);
                    dataToGlove[(i*3)+2]   = (byte) (color         & 0xFF);
                }
                sendDataToGlove(color);

            }


            @Override
            public void onColorSelectionStart(int i) {
                //sendDataToGlove(SET_RGB, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, fingerIndex.getProgress(),0);
            }

            @Override
            public void onColorSelectionEnd(int i) {
                //sendDataToGlove(SET_RGB, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, fingerIndex.getProgress(),0);

            }
        });



        //register a receiver from the BluetoothConnectionThread to pick up any data coming from the gloves
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingFromGlove"));

        rand = new Random();


    }


    /*protected void sendDataToGlove(byte messageCode, int data1, int data2, int data3, int data4, int data5)
    {
        byte [] toGlove = {START_CODE, messageCode, (byte) data1, (byte) data2, (byte) data3, (byte) data4, (byte) data5, END_CODE};
        Intent fromGloveIntent = new Intent("outgoingToGlove");
        fromGloveIntent.putExtra("bytesToGlove", toGlove);
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(fromGloveIntent);
    }*/

    protected void sendDataToGlove(int color)
    {
        //int color = colorWheel.getSolidColor();

        //dataToGlove[0] = 127;
        //dataToGlove[1] = 127;
        //dataToGlove[2] = 127;

        dataToGlove[BUFFERSIZE-1] = SET_RGB_ALL;

        Intent sendBytesToGloveIntent = new Intent("outgoingToGlove");
        sendBytesToGloveIntent.putExtra("bytesToGlove", dataToGlove);
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(sendBytesToGloveIntent);

    }

    protected void openConnectionMenu()
    {
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent, REQUEST_FOR_BT_ADDRESS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case REQUEST_FOR_BT_ADDRESS:
                if(resultCode == Activity.RESULT_OK)
                {
                    String address = data.getStringExtra("btAddressKey");
                    connectedThread = new BluetoothConnectedThread(mBluetoothAdapter.getRemoteDevice(data.getStringExtra("btAddressKey")), this);
                    connectedThread.start();
                }
                break;
        }
    }

    @Override
    public void onClick(View v)
    {
        int code = v.getId();
        switch (code)
        {
            case R.id.offButton: //sendDataToGlove(SET_RGB_ALL, 0, 0, 0, 0, 0);
                break;
            case R.id.viewPairedDevicesButton: openConnectionMenu();
                break;
            case R.id.send_rainbow:
                //sendDataToGlove(SET_RAINDOW, rainbowIndex, 0, 0, 0, 0);
                ++rainbowIndex;
            default:
                break;

        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int code = seekBar.getId();

        switch (code)
        {
            case R.id.cycleJumpBar:
                break;
            default:
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

}

