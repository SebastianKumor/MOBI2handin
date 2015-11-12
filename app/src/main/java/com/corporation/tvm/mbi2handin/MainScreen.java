package com.corporation.tvm.mbi2handin;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

public class MainScreen extends AppCompatActivity implements BeaconConsumer {
    public static Region mRegion = new Region("Server", Identifier.parse("Here is my UUID"), null, null);
    private BeaconManager mBeaconManager;
    private BeaconBaseAdapter beaconBaseAdapter;

    private ListView beaconsListLv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBeaconManager = BeaconManager.getInstanceForApplication(this);
        //BEACON PARSER
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
//        mBeaconManager.debug = true;
        beaconBaseAdapter = new BeaconBaseAdapter(this);
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enable :)
            }
        }

        //Start Monitoring and Ranging
        mBeaconManager.bind(this);
    }
/*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_main_screen, container, false);

        //UI
        beaconsListLv = (ListView) view.findViewById(R.id.beaconsListView);

        //Set Adapter
        beaconsListLv.setAdapter(beaconBaseAdapter);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enable :)
            }
        }

        //Start Monitoring and Ranging
        mBeaconManager.bind(this);

        return view;
    }
*/
    @Override
    public void onResume() {
        super.onResume();
        if(mBeaconManager.isBound(this)){
            mBeaconManager.setBackgroundMode(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mBeaconManager.isBound(this)){
            mBeaconManager.setBackgroundMode(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBeaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        try {
            //Scan lasts for SCAN_PERIOD time
            mBeaconManager.setForegroundScanPeriod(1000l);
//        mBeaconManager.setBackgroundScanPeriod(0l);
            //Wait every SCAN_PERIOD_INBETWEEN time
            mBeaconManager.setForegroundBetweenScanPeriod(0l);
            //Update default time with the new one
            mBeaconManager.updateScanPeriods();
        }catch (RemoteException e){
            e.printStackTrace();
        }

        //Set Monitoring
        mBeaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.d("TEST", "ENTERED beacon region");
                //Start Raning as soon as you detect a beacon
                try {
                    mBeaconManager.startRangingBeaconsInRegion(mRegion);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                Log.d("TEST", "EXITED beacon region");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.d("TEST", "SWITCHED from seeing/not seeing beacon to state " + state);
            }
        });

        //Set Ranging
        mBeaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(final Collection<Beacon> beacons, Region region) {
                if (beacons != null && beacons.size() > 0) {
                    MainScreen.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            beaconBaseAdapter.initAll(beacons);
                        }
                    });
                }
            }
        });

        try {
            //Start Monitoring
            mBeaconManager.startMonitoringBeaconsInRegion(mRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        this.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int mode) {
        return this.bindService(intent, serviceConnection, mode);
    }
}