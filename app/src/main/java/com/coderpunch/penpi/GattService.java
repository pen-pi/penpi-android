package com.coderpunch.penpi;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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

public class GattService extends Service {

    public final static UUID PENPI_SERVICE = UUID.fromString("999d97c6-0e31-4b46-b8cb-ef4c2c918c00");
    public final static UUID PENPI_COMMAND = UUID.fromString("999d97c6-0e31-4b46-b8cb-ef4c2c918c01");


    public final static String GATT_START_SCAN = "com.coderpunch.penpi.GATT_START_SCAN";
    public final static String GATT_STOP_SCAN = "com.coderpunch.penpi.GATT_STOP_SCAN";

    public final static String GATT_SCAN_STARTED = "com.coderpunch.penpi.GATT_SCAN_STARTED";
    public final static String GATT_SCAN_STOPPED = "com.coderpunch.penpi.GATT_SCAN_STOPPED";
    public final static String GATT_SCAN_RESULT = "com.coderpunch.penpi.GATT_SCAN_RESULT";

    public final static String GATT_DEVICE_CONNECT = "com.coderpunch.penpi.GATT_DEVICE_CONNECT";
    public final static String GATT_DEVICE_DISCONNECT = "com.coderpunch.penpi.GATT_DEVICE_DISCONNECT";

    public final static String GATT_DEVICE_FOUND = "com.coderpunch.penpi.GATT_DEVICE_FOUND";
    public final static String GATT_DEVICE_CONNECTED = "com.coderpunch.penpi.GATT_DEVICE_CONNECTED";
    public final static String GATT_DEVICE_DISCONNECTED = "com.coderpunch.penpi.GATT_DEVICE_DISCONNECTED";
    public final static String GATT_DEVICE_READ = "com.coderpunch.penpi.GATT_DEVICE_READ";
    public final static String GATT_DEVICE_WRITE = "com.coderpunch.penpi.GATT_DEVICE_WRITE";
    public final static String GATT_DEVICE_NOTIFY = "com.coderpunch.penpi.GATT_DEVICE_NOTIFY";
    public final static String GATT_DEVICE_SET_NOTIFY = "com.coderpunch.penpi.GATT_DEVICE_SET_NOTIFY";

    private static final String TAG = "GattService";




    private boolean mScanning;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private final IBinder mBinder = new LocalBinder();

    private static final long SCAN_PERIOD = 20000;
    private HashMap<String, BluetoothDevice> mDevices;

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
        filterGatt.addAction(GATT_DEVICE_CONNECT);
        filterGatt.addAction(GATT_DEVICE_DISCONNECT);

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
        broadcastDevice(device, GATT_DEVICE_FOUND);
    }

    private void broadcast(Intent intent){
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastDevice(BluetoothDevice device, String action){

        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra("BluetoothDevice", device);
        broadcast(intent);
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
                case GATT_DEVICE_SET_NOTIFY:
                    {
                        BluetoothDevice blDevice = intent.getParcelableExtra("BluetoothDevice");
                        connect(blDevice);
                    }
                    stopScan();
                    break;
                case GATT_DEVICE_CONNECT:
                    {
                        BluetoothDevice blDevice = intent.getParcelableExtra("BluetoothDevice");
                        connect(blDevice);

                    }
                    break;
                default:
            }
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public BluetoothGatt connect(BluetoothDevice blDevice){
        return blDevice.connectGatt(this, false, gattCallback);
    }

    private GattCallback gattCallback = new GattCallback(){
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            BluetoothDevice blDevice = gatt.getDevice();

            Intent intent = new Intent();
            intent.setAction(GATT_DEVICE_WRITE);
            intent.putExtra("BluetoothDevice", blDevice);
            intent.putExtra("Value", characteristic.getValue());

            broadcast(intent);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            BluetoothDevice blDevice = gatt.getDevice();

            Intent intent = new Intent();
            intent.setAction(GATT_DEVICE_READ);
            intent.putExtra("BluetoothDevice", blDevice);
            intent.putExtra("Characteristic", characteristic.getUuid());
            intent.putExtra("Value", characteristic.getValue());
            broadcast(intent);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            BluetoothDevice blDevice = gatt.getDevice();

            if(newState == BluetoothProfile.STATE_CONNECTED){
                Log.i(TAG, "Connected");
                broadcastDevice(blDevice, GATT_DEVICE_CONNECTED);
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.i(TAG, "Disconnected");
                broadcastDevice(blDevice, GATT_DEVICE_DISCONNECTED);
            }

        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            BluetoothDevice blDevice = gatt.getDevice();

            Intent intent = new Intent();
            intent.setAction(GATT_DEVICE_NOTIFY);
            intent.putExtra("BluetoothDevice", blDevice);
            intent.putExtra("Characteristic", characteristic.getUuid());
            intent.putExtra("Value", characteristic.getValue());

            broadcast(intent);
        }

    };



}
