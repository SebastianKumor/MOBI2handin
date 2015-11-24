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
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {
    private final String BLUETOOTH_ADAPTER_ADDRESS = "98:D3:31:30:57:7E";
    private final String OUR_BEACON_ADDRESS ="F8:6B:1B:52:3D:CD";
    Button refreshButton;
    Button enableButton;
    Button discoverButton;
    TextView support;
    TextView enabled;
    TextView bonded;
    BluetoothAdapter mBluetoothAdapter;
    DataOutputStream os;
    ArrayList<BluetoothDevice> devicesFound;
    View loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        loading = findViewById(R.id.loadingControl);
        loading.setVisibility(View.INVISIBLE);
        devicesFound = new ArrayList<BluetoothDevice>(0);
        support = (TextView) findViewById(R.id.supportString);
        enabled = (TextView) findViewById(R.id.enabledString);
        bonded = (TextView) findViewById(R.id.bondedString);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        refreshButton = (Button) findViewById(R.id.refreshButton);
        enableButton = (Button) findViewById(R.id.enableButton);
        discoverButton = (Button) findViewById(R.id.discoverButton);

        refreshButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (mBluetoothAdapter == null) {
                    support.setText("Phone does not support BT");
                } else {
                    support.setText("Phone supports BT");
                    if (mBluetoothAdapter.isEnabled()) {
                        enabled.setText("BT is enabled");

                        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                        List<String> s = new ArrayList<String>();
                        for (BluetoothDevice bt : pairedDevices) {
                            s.add(bt.getName() + "\n" + bt.getAddress());
                        }
                        if(s.size() > 0)
                            bonded.setText(s.get(0));
                    } else {
                        enabled.setText("BT is not enabled");
                    }
                }
            }
        });

        enableButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothAdapter.isDiscovering())
                {
                    loading.setVisibility(View.INVISIBLE);
                    mBluetoothAdapter.cancelDiscovery();
                }
                if (mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                    } else {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, 9);
                    }
                }
            }
        });

        discoverButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isEnabled()) {
                        IntentFilter filter = new IntentFilter();

                        filter.addAction(BluetoothDevice.ACTION_FOUND);
                        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

                        registerReceiver(mReceiver, filter);

                        // If we're already discovering, stop it
                        if (mBluetoothAdapter.isDiscovering()) {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                        mBluetoothAdapter.startDiscovery();
                        loading.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                loading.setVisibility(View.INVISIBLE);

                for(int i=0;i<devicesFound.size();i++){
                    if(devicesFound.get(i).getAddress().equals(BLUETOOTH_ADAPTER_ADDRESS)){
                        try{
                            //mBluetoothAdapter.cancelDiscovery();
                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(devicesFound.get(i).getAddress());

                            Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});

                            BluetoothSocket clientSocket =  (BluetoothSocket) m.invoke(device, 1);

                            clientSocket.connect();

                            os = new DataOutputStream(clientSocket.getOutputStream());

                            new ClientSock().start();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("BLUETOOTH", e.getMessage());
                        }
                    }
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found

                BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devicesFound.add(remoteDevice);
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        if(mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.cancelDiscovery();
            loading.setVisibility(View.INVISIBLE);
        }
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.cancelDiscovery();
            loading.setVisibility(View.INVISIBLE);
        }
      //  unregisterReceiver(mReceiver);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    public class ClientSock extends Thread {
        public void run () {
            try {
                os.write("1".getBytes()); // anything you want
                os.flush();
            } catch (Exception e1) {
                e1.printStackTrace();
                return;
            }
        }
    }
}
