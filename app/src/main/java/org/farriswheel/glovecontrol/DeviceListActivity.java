package org.farriswheel.glovecontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by will on 7/20/18.
 */

public class DeviceListActivity extends Activity {

    protected ListView deviceListView;
    protected BluetoothAdapter adapter;
    protected List<BluetoothDevice> pairedDevices;
    protected Intent fromIntent;

    DeviceListActivity() {
        fromIntent = getIntent();
    }

    @Override
    protected void onCreate(Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
        setContentView(R.layout.activity_paired);

        deviceListView = (ListView) findViewById(R.id.pairedDeviceListView);

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            pairedDevices = new ArrayList<>(adapter.getBondedDevices());
        } else
            Log.e("ERROR", "Couldn't get bt adapter");


        populateListView();
        registerClickCallback();
    }

    private void populateListView() {

        //build adapter
        ArrayAdapter<BluetoothDevice> pairedDeviceAdapter = new BtDeviceListViewAdapter();
        deviceListView = (ListView) findViewById(R.id.pairedDeviceListView);
        deviceListView.setAdapter(pairedDeviceAdapter);

    }

    private void registerClickCallback()
    {
        ListView list = (ListView) findViewById(R.id.pairedDeviceListView);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id)
            {
                Intent addressIntent = new Intent("clickedWithAddress");
                addressIntent.putExtra("btAddressKey", pairedDevices.get(position).getAddress());
                setResult(RESULT_OK, addressIntent);
                finish();
            }
        });
    }

    private class BtDeviceListViewAdapter extends ArrayAdapter<BluetoothDevice>
    {
        public BtDeviceListViewAdapter()
        {
            super(DeviceListActivity.this, R.layout.bt_device, pairedDevices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View itemView = convertView;

            if(itemView == null)
            {
                itemView = getLayoutInflater().inflate(R.layout.bt_device, parent, false);
            }

            BluetoothDevice current = pairedDevices.get(position);
            TextView currentName = (TextView) itemView.findViewById(R.id.btName);
            currentName.setText(current.getName());
            TextView currentAddress = (TextView) itemView.findViewById(R.id.btAddress);
            currentAddress.setText(current.getAddress());


            return itemView;
        }
    }
}
