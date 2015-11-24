package com.corporation.tvm.mbi2handin;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;

//import org.altbeacon.beacon.Beacon;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;

import java.util.ArrayList;
import java.util.List;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconType;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconUtils;
import uk.co.alt236.bluetoothlelib.device.beacon.ibeacon.IBeaconDevice;


public class MainScreen extends ListActivity  {

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private  BeaconManager beaconManager;
   //private Beacon beacon;
    private String power;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      //  getActionBar().setTitle(R.string.title_device);
        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.blue_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_blue_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));  // iBeacons
        //beaconManager.bind(this);




    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_screen, menu);
        menu.findItem(R.id.menu_arduino).setVisible(true);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                   R.layout.progress_layout_mater);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case  R.id.menu_arduino:
                Intent intent =new Intent(MainScreen.this,BluetoothActivity.class);
               this.startActivity(intent);
        }
        return true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }
   // @Override
//    protected void onListItemClick(ListView l, View v, int position, long id) {
//        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
//        if (device == null) return;
//        final Intent intent = new Intent(this, MainScreen.class);
//        intent.putExtra(MainScreen.Ex, device.getName());
//        intent.putExtra(MainScreen.EXTRAS_DEVICE_ADDRESS, device.getAddress());
//        if (mScanning) {
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            mScanning = false;
//        }
//        startActivity(intent);
//    }
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);


        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }
    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<String> rssis;
        private ArrayList<String> powers;
        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            rssis=new ArrayList<String>();
            powers=new ArrayList<String>();
            mInflator = MainScreen.this.getLayoutInflater();
        }
        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }
        public void deviceWithRssi(BluetoothDevice device, String rssi){
           // rssis.add(rssi);
            if(!rssis.contains(rssi)) {
                rssis.add(rssi);
            }

        }
        public void deviceWithPower(BluetoothDevice device, String power){
            // rssis.add(rssi);
            if(!powers.contains(power)) {
                powers.add(power);
            }

        }
        public void clear() {
            mLeDevices.clear();
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            String rssi;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.table_view_row, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.row_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.row_name);
                viewHolder.deviceUuid = (TextView) view.findViewById(R.id.row_uuid);
                viewHolder.deviceMinor = (TextView) view.findViewById(R.id.row_minor);
                viewHolder.deviceMajor = (TextView) view.findViewById(R.id.row_major);

                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);

            //if(rssis.size()>0) {
                rssi = rssis.get(i);
           // }
//            if (powers.size()<=i){
//
//                if (powers.size() > 0) {
//                    power = powers.get(i);
//                }
//            }

            //String rssi =

            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);


            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

           // String uuid =devic;
            viewHolder.deviceUuid.setText(rssi);
            viewHolder.deviceMajor.setText(power);
//            viewHolder.deviceMajor.setText(device.getType());
//            viewHolder.deviceMinor.setText(device.getBondState());
            return view;
        }
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String rsa=Integer.toString(rssi);
                            String pwrandrssi=rsa;
                            double distance=100;
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                            final BluetoothLeDevice deviceLe = new BluetoothLeDevice(device, rssi, scanRecord, System.currentTimeMillis());
                            ///alt library code
                            //mDeviceStore.addDevice(deviceLe);

                            double rssii= Double.parseDouble(rsa);



                            if (BeaconUtils.getBeaconType(deviceLe) == BeaconType.IBEACON) {
                                final IBeaconDevice iBeacon = new IBeaconDevice(deviceLe);
                                double txPower=Double.parseDouble(Integer.toString(iBeacon.getCalibratedTxPower()));
                                pwrandrssi =rsa+","+Integer.toString(iBeacon.getCalibratedTxPower())+", distance: "+ Math.pow(10d, ((double) txPower - rssii) / (10 * 2));
                                // DO STUFF
                                distance =  Math.pow(10d, ((double) txPower - rssii) / (10 * 2));
                            }


                            if(rsa!=null) {
                                mLeDeviceListAdapter.deviceWithRssi(device, pwrandrssi);
                               // mLeDeviceListAdapter.deviceWithPower(device,Double.toString(distance));
                            }
                        }
                    });
                }

            };
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceUuid;
        TextView deviceMinor;
        TextView deviceMajor;
    }
}