package com.coderpunch.penpi;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_FIRST_MATCH;
import static android.support.v4.app.ActivityCompat.startActivityForResult;

public class GattService extends Service {
    public final static String GATT_START_SCAN = "com.coderpunch.penpi.GATT_START_SCAN";
    public final static String GATT_STOP_SCAN = "com.coderpunch.penpi.GATT_STOP_SCAN";

    public final static String GATT_SCAN_STARTED = "com.coderpunch.penpi.GATT_SCAN_STARTED";
    public final static String GATT_SCAN_STOPPED = "com.coderpunch.penpi.GATT_SCAN_STOPPED";
    public final static String GATT_DEVICE_FOUND = "com.coderpunch.penpi.GATT_DEVICE_FOUND";
    public final static String GATT_SCAN_RESULT = "com.coderpunch.penpi.GATT_SCAN_RESULT";

    private static final String TAG = "GattService";


    public class LocalBinder extends Binder {
        GattService getService() {
            return GattService.this;
        }
    }


    @Override
    public void onCreate(){

        BluetoothManager bluetoothManager =  (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);

        mDevices = new HashMap<>();
        mScanning = false;
        mHandler = new Handler();
        mBluetoothAdapter = bluetoothManager.getAdapter();


        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            return;
        }

        // Register receivers
        IntentFilter filterGatt = new IntentFilter();
        filterGatt.addAction(GATT_START_SCAN);
        filterGatt.addAction(GATT_STOP_SCAN);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filterGatt);

    }



    public void stopScan(){
        if(!mScanning)
            return;

        Log.v(TAG, "Stopping the scan");
        mScanning = false;

        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);

        // Broadcast
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(GATT_SCAN_STOPPED);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(broadcastIntent);
    }

    public void startScan(){
        if(mScanning)
            return;

        Log.v(TAG, "Scanning for devices...");


        mDevices.clear();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, SCAN_PERIOD);

        mScanning = true;
        mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);

        // Broadcast
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(GATT_SCAN_STARTED);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(broadcastIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void addDevice(BluetoothDevice device) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(GATT_DEVICE_FOUND);
        broadcastIntent.putExtra("BluetoothDevice", device);


        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(broadcastIntent);
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            // Broadcast the scan results
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(GATT_SCAN_RESULT);
            broadcastIntent.putExtra("ScanResult", result);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(GattService.this);
            localBroadcastManager.sendBroadcast(broadcastIntent);



            // Process the device
            BluetoothDevice btDevice = result.getDevice();
            if(btDevice != null && !mDevices.containsKey(btDevice.getAddress())) {

                mDevices.put(btDevice.getAddress(), btDevice);

                Log.i(TAG, String.valueOf(callbackType));
                Log.i(TAG, result.toString());

                if(btDevice.getUuids() != null)
                    Log.i(TAG, btDevice.getUuids().toString());


                GattService.this.addDevice(btDevice);

            }

        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch(action){
                case GATT_START_SCAN:
                    startScan();
                    break;
                case GATT_STOP_SCAN:
                    stopScan();
                    break;
                default:
            }
        }
    };

    private HashMap<String, BluetoothDevice> mDevices;


    private boolean mScanning;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private final IBinder mBinder = new LocalBinder();

    private static final long SCAN_PERIOD = 20000;
}
