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


public class MainActivity extends Activity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener {

    protected BluetoothAdapter mBluetoothAdapter;        //Set to the default adapter during onCreate()
    protected BluetoothConnectedThread connectedThread;  //Bluetooth connection is handled in this class, which runs in a separate thread

    //Defines the UI elements described in activity_main.xml
    protected TextView fromGlove;
    protected Button connectDevices;
    protected Button red;
    protected Button green;
    protected Button blue;
    protected Button custom1;
    protected Button custom2;
    protected Button custom3;
    protected Button custom4;
    protected Button off;
    protected Button singleRandom;
    
    protected SeekBar cycleSpeed;

    protected SeekBar setCycleJump;
    protected SeekBar setLoopDelay;

    protected TextView cycleSpeedValue;
    protected SeekBar setHSV;
    protected BroadcastReceiver mReceiver;

    protected HSLColorPicker colorWheel;

    protected RadioGroup lightTypeToggle;
    protected RadioGroup cycleModeToggle;

    protected final int REQUEST_FOR_BT_ADDRESS = 104;

    protected final byte START_CODE      = (byte) 0xFF;
    protected final byte END_CODE        = (byte) 0x0F;
    protected final byte SET_RGB         = (byte) 0x0A;
    protected final byte SET_LOOP_DELAY  = (byte) 0x0B;
    protected final byte SET_CYCLE_JUMP  = (byte) 0x0C;
    protected final byte SET_SINGLE_RGB  = (byte) 0x0D;

    private Random rand;
    int rainbowIndex = 0;

    protected enum LIGHT_DISPLAY_TYPE
    {
        all_same, single_random
    }

    protected enum CYCLE_MODE
    {
        rainbow, single_color
    }

    LIGHT_DISPLAY_TYPE lightSelectionPattern;
    CYCLE_MODE cycleMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectedThread = null;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int res = 69;
            startActivityForResult(enableBtIntent, res);
        }

        connectDevices = (Button) findViewById(R.id.viewPairedDevicesButton);
        connectDevices.setOnClickListener(this);

        red = (Button) findViewById(R.id.redButton);
        red.setOnClickListener(this);

        green = (Button) findViewById(R.id.greenButton);
        green.setOnClickListener(this);

        blue = (Button) findViewById(R.id.blueButton);
        blue.setOnClickListener(this);

        custom1 = (Button) findViewById(R.id.customButton1);
        custom1.setOnClickListener(this);

        custom2 = (Button) findViewById(R.id.customButton2);
        custom2.setOnClickListener(this);

        custom3 = (Button) findViewById(R.id.customButton3);
        custom3.setOnClickListener(this);

        custom4 = (Button) findViewById(R.id.customButton4);
        custom4.setOnClickListener(this);

        off = (Button) findViewById(R.id.offButton);
        off.setOnClickListener(this);

        lightTypeToggle = (RadioGroup) findViewById(R.id.light_mode_toggle_group);
        lightTypeToggle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                switch(checkedId)
                {
                    case R.id.all_same_radio: lightSelectionPattern = LIGHT_DISPLAY_TYPE.all_same; Log.d("radio", String.valueOf(checkedId));
                        break;
                    case R.id.single_random_radio: lightSelectionPattern = LIGHT_DISPLAY_TYPE.single_random;

                    default:
                        break;
                }
            }
        });

        cycleModeToggle = (RadioGroup) findViewById(R.id.cycle_mode_toggle_group);
        cycleModeToggle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                switch(checkedId)
                {
                    case R.id.rainbow_toggle_radio: cycleMode = CYCLE_MODE.rainbow;
                        break;
                    case R.id.single_color_radio: cycleMode = CYCLE_MODE.single_color;

                    default:
                        break;
                }
            }
        });

        setCycleJump = (SeekBar) findViewById(R.id.cycleJumpBar);
        setCycleJump.setOnSeekBarChangeListener(this);


        setLoopDelay = (SeekBar) findViewById(R.id.loopDelayBar);
        setLoopDelay.setOnSeekBarChangeListener(this);

        fromGlove = (TextView) findViewById(R.id.byteFromBt);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte [] buff = intent.getByteArrayExtra("bytesFromGlove");
                fromGlove.setText("Result from glove: " + String.valueOf(buff[0]));
            }
        };

        colorWheel = (HSLColorPicker) findViewById(R.id.colorWheel);
        colorWheel.setColorSelectionListener(new OnColorSelectionListener() {
            @Override
            public void onColorSelected(int i) {
                //Log.d("COLOR", String.valueOf((i >> 16) & 0xFF) + " " + String.valueOf((i >> 8) & 0xFF) + " " + String.valueOf(i & 0xFF));
                if (cycleMode == CYCLE_MODE.rainbow) {
                    sendDataToGlove(SET_SINGLE_RGB, (i >> 16) &0xFF, (i >> 8) & 0xFF, i & 0xFF, rainbowIndex, 0);
                    if (rainbowIndex <= 4)
                        ++rainbowIndex;
                    else
                        rainbowIndex = 0;
                } else {
                    sendDataToGlove(SET_RGB, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, 0, 0);

                }
            }


            @Override
            public void onColorSelectionStart(int i) {
                sendDataToGlove(SET_RGB, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, 0,0);
            }

            @Override
            public void onColorSelectionEnd(int i) {
                sendDataToGlove(SET_RGB, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, 0,0);

            }
        });

        //register a receiver from the BluetoothConnectionThread to pick up any data coming from the gloves
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingFromGlove"));

        rand = new Random();
    }


    protected void sendDataToGlove(byte messageCode, int data1, int data2, int data3, int data4, int data5)
    {
        byte [] toGlove = {START_CODE, messageCode, (byte) data1, (byte) data2, (byte) data3, (byte) data4, (byte) data5, END_CODE};
        Intent fromGloveIntent = new Intent("outgoingToGlove");
        fromGloveIntent.putExtra("bytesToGlove", toGlove);
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(fromGloveIntent);
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

            case R.id.redButton: sendColorToGlove(255, 0, 0);
                break;
            case R.id.greenButton: sendColorToGlove(0, 255, 0);
                break;
            case R.id.blueButton: sendColorToGlove(0, 0, 255);
                break;
            case R.id.customButton1: sendColorToGlove(255, 0, 67);
                break;
            case R.id.customButton2: sendColorToGlove(255, 37, 0);
                break;
            case R.id.customButton3: sendColorToGlove(0, 102, 255);
                break;
            case R.id.customButton4: sendColorToGlove(255, 255, 255);
                break;
            case R.id.offButton: sendDataToGlove(SET_RGB, 0, 0, 0, 0, 0); //sendDataToGlove(SET_RGB, 0, 0, 0, 0, 0);
                break;
            case R.id.viewPairedDevicesButton: openConnectionMenu();
                break;
            default:
                break;

        }

    }

    private void sendColorToGlove(int r, int g, int b)
    {
        if(lightSelectionPattern == LIGHT_DISPLAY_TYPE.single_random)
        {
            sendDataToGlove(SET_SINGLE_RGB, r, g, b, rand.nextInt(5), 0);
        } else
        {
            sendDataToGlove(SET_RGB, r, g, b, 0, 0);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int code = seekBar.getId();

        switch (code)
        {
            case R.id.cycleJumpBar: //sendCycleSpeedToGlove(i);
                break;
            case R.id.loopDelayBar: //sendDataToGlove(SET_LOOP_DELAY, i, 0, 0, 0, 0);
                break;
            default:
                break;
        }
    }

    private void sendCycleSpeedToGlove(int speed)
    {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}

