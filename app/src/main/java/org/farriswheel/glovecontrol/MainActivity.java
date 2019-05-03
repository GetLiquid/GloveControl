package org.farriswheel.glovecontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.larswerkman.holocolorpicker.ColorPicker;

import java.util.Random;


public class MainActivity extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    protected BluetoothAdapter mBluetoothAdapter;        //Set to the default adapter during onCreate()
    protected BluetoothConnectedThread connectedThread;  //Bluetooth connection is handled in this class, which runs in a separate thread


    //Defines the UI elements described in activity_main.xml
    protected TextView fromGlove;
    protected Button connectDevices;
    protected SeekBar fingerIndex;
    protected BroadcastReceiver mReceiver;
    protected Button randomButton;
    protected Button resetSingleButton;

    protected RadioButton colorTypeSingle;
    protected RadioButton colorTypeAll;

    protected ColorPicker colorWheel;

    protected final int REQUEST_FOR_BT_ADDRESS = 104;

    protected final byte SET_RGB_ALL     = (byte) 0x0A;

    protected final byte BUFFERSIZE = 15;
    protected final byte NUMPIXELS = 5;

    private Random rand;
    private byte [] dataToGlove;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //rainbowLayer = new byte[BUFFERSIZE];
        //singleLayer = new byte[BUFFERSIZE];
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

        colorWheel = (ColorPicker) findViewById(R.id.colorWheel);
        colorWheel.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                for(int i=0;i<NUMPIXELS;++i)
                {
                    dataToGlove[(i*3)]     = (byte) ((color >> 16) & 0xFF);
                    dataToGlove[(i*3)+1]   = (byte) ((color >> 8)  & 0xFF);
                    dataToGlove[(i*3)+2]   = (byte) (color         & 0xFF);
                }
               // connectedThread.write();
                sendDataToGlove();
            }
        });


        randomButton = (Button) findViewById(R.id.set_color_button);
        randomButton.setOnClickListener(this);
        resetSingleButton = (Button) findViewById(R.id.reset_all_button);
        resetSingleButton.setOnClickListener(this);

        colorTypeSingle = (RadioButton) findViewById(R.id.colo_type_single_button);
        colorTypeAll = (RadioButton) findViewById(R.id.color_type_all_button);


        //register a receiver from the BluetoothConnectionThread to pick up any data coming from the gloves
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingFromGlove"));

        rand = new Random();


    }


    protected void sendDataToGlove()
    {
        //dataToGlove[0] = (byte) 0xCF;
        //dataToGlove[BUFFERSIZE-1] = (byte) 0xFC;
        /*Intent sendBytesToGloveIntent = new Intent("outgoingToGlove");
        sendBytesToGloveIntent.putExtra("bytesToGlove", dataToGlove);
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(sendBytesToGloveIntent);*/
        connectedThread.write(dataToGlove);

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
            case R.id.set_color_button:

                int color = colorWheel.getColor();
                if(colorTypeSingle.isSelected()) {
                    for (int i = 0; i < BUFFERSIZE; ++i) {
                        dataToGlove[i] = 0;
                    }

                    int index = fingerIndex.getProgress();
                    dataToGlove[(index * 3)] = (byte) ((color >> 16) & 0xFF);
                    dataToGlove[(index * 3) + 1] = (byte) ((color >> 8) & 0xFF);
                    dataToGlove[(index * 3) + 2] = (byte) (color & 0xFF);
                } else
                {
                    for(int i=0;i<NUMPIXELS;++i)
                    {
                        dataToGlove[(i*3)]     = (byte) ((color >> 16) & 0xFF);
                        dataToGlove[(i*3)+1]   = (byte) ((color >> 8)  & 0xFF);
                        dataToGlove[(i*3)+2]   = (byte) (color         & 0xFF);
                    }
                }
                sendDataToGlove();
                break;
            case R.id.reset_all_button:
                for(int i=0;i<BUFFERSIZE;++i)
                {
                    dataToGlove[i] = 0;
                }
                sendDataToGlove();
                break;
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

