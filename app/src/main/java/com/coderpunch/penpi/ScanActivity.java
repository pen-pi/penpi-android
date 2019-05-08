package com.coderpunch.penpi;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "DeviceListAdapter";
    private ListView mDeviceList;
    private DeviceListAdapter mDeviceAdapter;
    private Button mScanButton;


    private HashMap<String, BluetoothDevice> mDevices;

    private final static UUID PENPI_SERVICE = UUID.fromString("999d97c6-0e31-4b46-b8cb-ef4c2c918c00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);


        mDevices = new HashMap<>();


        mDeviceList = findViewById(R.id.deviceList);
        mScanButton = findViewById(R.id.scanButton);





        mDeviceAdapter = new DeviceListAdapter(this, R.layout.adapter_device, new ArrayList<BluetoothDevice>());
        mDeviceList.setAdapter(mDeviceAdapter);


        // Register receiver
        IntentFilter filterGatt = new IntentFilter();
        filterGatt.addAction(GattService.GATT_SCAN_STARTED);
        filterGatt.addAction(GattService.GATT_SCAN_STOPPED);
        filterGatt.addAction(GattService.GATT_SCAN_RESULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filterGatt);


        // Start our Gatt service
        Intent serviceIntent = new Intent(this, GattService.class);
        startService(serviceIntent);


        // Setup click listener
        mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                BluetoothDevice btDevice = ((DeviceListAdapter) parent.getAdapter()).getItem(position);
                Log.i(TAG, btDevice.getAddress());

                Intent intent = new Intent(ScanActivity.this, TerminalActivity.class);
                intent.putExtra("BluetoothDevice", btDevice);
                startActivity(intent);


            }
        });
    }


    public void startScan(View v){
        ((ArrayAdapter<BluetoothDevice>)mDeviceList.getAdapter()).clear();

        mScanButton.setEnabled(false);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(GattService.GATT_START_SCAN);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(broadcastIntent);

    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch(action){
                case GattService.GATT_SCAN_STARTED:
                    mScanButton.setEnabled(false);
                    break;
                case GattService.GATT_SCAN_STOPPED:
                    mScanButton.setEnabled(true);
                    break;
                case GattService.GATT_SCAN_RESULT:

                    ScanResult result = (ScanResult) intent.getParcelableExtra("ScanResult");
                    BluetoothDevice btDevice = result.getDevice();

                    if(btDevice == null)
                        return;

                    if(mDevices.containsKey(btDevice.getAddress()))
                        return;

                        // Check if the device is our Penpi
                    List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();

                    if(uuids == null)
                        return;

                    boolean isPenpi = false;
                    for(ParcelUuid parcelUuid: uuids){
                        if(parcelUuid.getUuid().equals(PENPI_SERVICE)){
                            isPenpi = true;
                            break;
                        }
                    }

                    if(isPenpi) {
                        mDevices.put(btDevice.getAddress(), btDevice);
                        mDeviceAdapter.add(btDevice);
                    }

                    break;
                default:
            }
        }
    };
}
