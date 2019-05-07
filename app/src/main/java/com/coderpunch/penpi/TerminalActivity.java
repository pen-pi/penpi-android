package com.coderpunch.penpi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
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

import java.util.UUID;



class GattHandler extends Handler {
    public static final int MSG_CONNECTED = 1;
    public static final int MSG_DISCONNECTED = 2;
    private final TerminalActivity context;

    public GattHandler(TerminalActivity context) {

        this.context = context;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_CONNECTED:
                break;
            case MSG_DISCONNECTED:
                break;
        }
        return true;
    }
}


public class TerminalActivity extends AppCompatActivity implements TextView.OnEditorActionListener {
    private static final String TAG = "TerminalActivity";

    private final static UUID PENPI_SERVICE = UUID.fromString("999d97c6-0e31-4b46-b8cb-ef4c2c918c00");
    private final static UUID PENPI_COMMAND = UUID.fromString("999d97c6-0e31-4b46-b8cb-ef4c2c918c01");


    EditText mTerminalInput;
    TextView mTerminalOutput;
    ScrollView mTerminalScroll;
    TextView mDeviceName;
    TextView mConnectionStatus;

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



    }

    public void connect(View v) {
        mDevice.connectGatt(this,false, gattCallback);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            String command = mTerminalInput.getText().toString();
            mTerminalInput.setText("");
            mTerminalOutput.append("$ "+command+"\n");

            BluetoothGattCharacteristic mWriteCharacteristic = mDevice.getCharacteristic(Penpi.id_command);
            mWriteCharacteristic.setValue(value);
            if(!mBluetoothGatt.writeCharacteristic(mWriteCharacteristic)) {

            mTerminalScroll.post(new Runnable() {
                @Override
                public void run() {
                    mTerminalScroll.fullScroll(View.FOCUS_DOWN);
                }
            });


            return true;
        }
        return false;
    }




    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    final int newState) {


                    BluetoothGattCharacteristic characteristic = gatt.getService(PENPI_SERVICE).getCharacteristic(PENPI_COMMAND);
                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor();
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);



                    Handler mainHandler = new Handler(TerminalActivity.this.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            String intentAction;
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                mConnectionStatus.setText("Connected");

                                Log.i(TAG, "Connected to GATT server.");

                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                mConnectionStatus.setText("Disconnected");

                                Log.i(TAG, "Disconnected from GATT server.");
                            }
                        }
                    });

                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.w(TAG, "Service discovered!");
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.w(TAG, "onCharacteristicRead "+characteristic.toString());
                    }
                }

    };

}
