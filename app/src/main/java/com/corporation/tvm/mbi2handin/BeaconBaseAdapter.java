package com.corporation.tvm.mbi2handin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class BeaconBaseAdapter extends BaseAdapter {

    private Context myContext;

    private LayoutInflater inflater;
    public static ArrayList<Beacon> beacons;

    public BeaconBaseAdapter(Context context) {
        this.myContext = context;
        this.inflater = LayoutInflater.from(context);
        this.beacons = new ArrayList<Beacon>();
    }

    public void initAll(Collection<Beacon> newBeacons) {
        this.beacons.clear();
        this.beacons.addAll(newBeacons);
        //Sort
        Collections.sort(this.beacons, new Comparator<Beacon>() {
            @Override
            public int compare(Beacon b1, Beacon b2) {
                String mac1 = b1.getBluetoothAddress();
                String mac2 = b2.getBluetoothAddress();

                return mac1.compareTo(mac2);
            }
        });
    }

    @Override
    public int getCount() {
        return beacons.size();
    }

    @Override
    public Beacon getItem(int position) {
        return beacons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.table_view_row, null);
            convertView.setTag(new ViewHolder(convertView));
        }

        bind(getItem(position), position, convertView);
        return convertView;
    }


    private void bind(Beacon beacon, int position, View view) {
        ViewHolder holder = (ViewHolder) view.getTag();

        holder.manufacturerTextView.setText("Manufacturer: " + beacon.getManufacturer());
        holder.idOneTextView.setText("UUID: " + beacon.getId1());
        holder.idTwoTextView.setText("Major: " + beacon.getId2());
        holder.idThreeTextView.setText("Minor: " + beacon.getId3());
        holder.txPowerTextView.setText("TX-Power: " + beacon.getTxPower());
        holder.rssiTextView.setText("RSSI: " + beacon.getRssi());
        holder.distanceTextView.setText(String.format("DISTANCE: (%.2f m)", beacon.getDistance()));
        holder.nameTextView.setText("Bluetooth Name: " + beacon.getBluetoothName());
        holder.addressTextView.setText("Bluetooth Adrs: " + beacon.getBluetoothAddress());

    }



    static class ViewHolder {
        final TextView nameTextView;
        final TextView manufacturerTextView;
        final TextView idOneTextView;
        final TextView idTwoTextView;
        final TextView idThreeTextView;
        final TextView txPowerTextView;
        final TextView rssiTextView;
        final TextView distanceTextView;
        final TextView addressTextView;

        ViewHolder(View view) {
            nameTextView = (TextView) view.findViewWithTag("name");
            manufacturerTextView = (TextView) view.findViewWithTag("manufacturer");
            idOneTextView = (TextView) view.findViewWithTag("id_one");
            idTwoTextView = (TextView) view.findViewWithTag("id_two");
            idThreeTextView = (TextView) view.findViewWithTag("id_three");
            txPowerTextView = (TextView) view.findViewWithTag("tx_power");
            rssiTextView = (TextView) view.findViewWithTag("rssi");
            distanceTextView = (TextView) view.findViewWithTag("distance");
            addressTextView = (TextView) view.findViewWithTag("address");
        }
    }

}
