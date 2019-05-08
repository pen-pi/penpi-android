package com.coderpunch.penpi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.os.Message;

import java.util.List;
import java.util.UUID;


//
//class GattHandler extends Handler {
//    public static final int MSG_CONNECTED = 1;
//    public static final int MSG_DISCONNECTED = 2;
//    private final TerminalActivity context;
//
//    public GattHandler(TerminalActivity context) {
//
//        this.context = context;
//    }
//
//    @Override
//    public boolean handleMessage(Message msg) {
//        switch (msg.what) {
//            case MSG_CONNECTED:
//                break;
//            case MSG_DISCONNECTED:
//                break;
//        }
//        return true;
//    }
//}


public class TerminalActivity extends AppCompatActivity implements TextView.OnEditorActionListener {
    private static final String TAG = "TerminalActivity";



    EditText mTerminalInput;
    TextView mTerminalOutput;
    ScrollView mTerminalScroll;
    TextView mDeviceName;
    BluetoothGatt mDeviceGatt;
    TextView mConnectionStatus;
    GattCallback mGattCallback;


    private BluetoothGattService mPenpiService;
    private BluetoothGattCharacteristic mCommandCharacteristic;


    BluetoothDevice mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        mDevice = getIntent().getParcelableExtra("BluetoothDevice");


        mTerminalInput = (EditText) findViewById(R.id.terminalInput);
        mTerminalOutput = (TextView) findViewById(R.id.terminalOutput);
        mTerminalScroll = (ScrollView) findViewById(R.id.terminalScroll);

        mDeviceName = (TextView) findViewById(R.id.deviceName);
        mConnectionStatus = (TextView) findViewById(R.id.connectionStatus);

        if(mDevice.getName() != null)
            mDeviceName.setText(mDevice.getName());
        else
            mDeviceName.setText(mDevice.getAddress());

        mTerminalInput.setOnEditorActionListener(this);



        // Register receiver
        IntentFilter filterGatt = new IntentFilter();
        filterGatt.addAction(GattService.GATT_DEVICE_CONNECTED);
        filterGatt.addAction(GattService.GATT_DEVICE_DISCONNECTED);
        filterGatt.addAction(GattService.GATT_DEVICE_READ);
        filterGatt.addAction(GattService.GATT_DEVICE_WRITE);
        filterGatt.addAction(GattService.GATT_DEVICE_NOTIFY);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filterGatt);
    }

    public void connect(View v) {

        Intent intent = new Intent();

        intent.setAction(GattService.GATT_DEVICE_CONNECT);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);

    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            String command = mTerminalInput.getText().toString();
            mTerminalInput.setText("");
            mTerminalOutput.append("$ "+command+"\n");

            if(mDeviceGatt != null){
                mCommandCharacteristic.setValue(command);
                mDeviceGatt.writeCharacteristic(mCommandCharacteristic);
            }

            return true;
        }
        return false;
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            BluetoothDevice btDevice;

            switch (action) {
                case GattService.GATT_DEVICE_CONNECTED:
                    btDevice = (BluetoothDevice) intent.getParcelableExtra("BluetoothDevice");

                    mPenpiService = mDeviceGatt.getService(GattService.PENPI_SERVICE);
                    mCommandCharacteristic = mPenpiService.getCharacteristic(GattService.PENPI_COMMAND);
                    mDeviceGatt.setCharacteristicNotification(mCommandCharacteristic, true);

                    Log.i(TAG, "Device connected");
                    break;
                case GattService.GATT_DEVICE_DISCONNECTED:
                    btDevice = (BluetoothDevice) intent.getParcelableExtra("BluetoothDevice");
                    Log.i(TAG, "Device disconnected");
                    break;

                case GattService.GATT_DEVICE_WRITE:
                    btDevice = (BluetoothDevice) intent.getParcelableExtra("BluetoothDevice");
                    Log.i(TAG, "Device write");
                    break;

                case GattService.GATT_DEVICE_READ:
                    Log.i(TAG, "Device read");
                    btDevice = (BluetoothDevice) intent.getParcelableExtra("BluetoothDevice");
                    break;

                case GattService.GATT_DEVICE_NOTIFY:
                    Log.i(TAG, "Device notiffy");
                    btDevice = (BluetoothDevice) intent.getParcelableExtra("BluetoothDevice");
                    break;
                default:
            }
        }
    };
}
