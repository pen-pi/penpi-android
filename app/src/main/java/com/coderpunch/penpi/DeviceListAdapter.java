package com.coderpunch.penpi;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.ViewAnimator;

import java.util.ArrayList;
import java.util.Collection;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
    private static final String TAG = "DeviceListAdapter";

    private Context mContext;
    int mResource;

    public DeviceListAdapter(Context context, int resource, ArrayList<BluetoothDevice> objects){
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    public View getView(int position, View convertView, ViewGroup parent){

        BluetoothDevice device = getItem(position);

        LayoutInflater inflater = LayoutInflater.from(mContext);

        convertView = inflater.inflate(mResource, parent, false);


        TextView tvName = (TextView) convertView.findViewById(R.id.deviceName);
        TextView tvAddress = (TextView) convertView.findViewById(R.id.deviceAddress);

        tvName.setText(device.getName());
        tvAddress.setText(device.getAddress());

        return convertView;
    }

}
