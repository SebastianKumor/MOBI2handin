package com.corporation.tvm.mbi2handin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {
    private final String BLUETOOTH_ADAPTER_ADDRESS = "98:D3:31:30:57:7E";
    private final String OUR_BEACON_ADDRESS ="F8:6B:1B:52:3D:CD";
    private Bluetooth bt;

    Button refreshButton;
    Button enableButton;
    Button discoverButton;
    TextView enabled;
    View loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        loading.setVisibility(View.INVISIBLE);
        enabled = (TextView) findViewById(R.id.enabledString);
        enableButton = (Button) findViewById(R.id.enableButton);
        discoverButton = (Button) findViewById(R.id.discoverButton);

        refreshButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if(bt.isBtEnabled()){
                    enabled.setText("Bluetooth enabled");
                }
                else {
                    enabled.setText("Bluetooth disabled");
                }
            }
        });

        enableButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if(bt.isBtEnabled())
                    bt.disableBt();
                else
                    bt.enableBt();
            }
        });

        discoverButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                connectService();
            }
        });

    }

    public void connectService(){
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isEnabled()) {
                bt.connectDevice("HC-06");
            }
            else {
                Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT);
            }
        } catch(Exception e){
            Toast.makeText(this, "Unable to connect " + e, Toast.LENGTH_SHORT);
        }
    }
}
