package com.example.a1716342.beaconslocalisation;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Hashtable;

/*
     Adapter for Recycler View
*/
public class AdapterData extends RecyclerView.Adapter<AdapterData.ViewHolder> {
    Hashtable<ArrayList<String>, Integer> beaconsDictionnary;
    ArrayList<ArrayList<String>> keys;

    // Constructor
    public AdapterData(Hashtable<ArrayList<String>, Integer> beaconsDictionnary, ArrayList<ArrayList<String>> keys)
    {
        this.beaconsDictionnary = beaconsDictionnary;
        this.keys = keys;
    }

    /*
       View Holder class to instantiate views
     */
    class ViewHolder extends RecyclerView.ViewHolder{
        //UUID
        private TextView uuid;

        //Major
        private TextView major;

        //Minor
        private TextView minor;

        //RSSI
        private TextView rssi;

        //View Holder Class Constructor
        public ViewHolder(View itemView)
        {
            super(itemView);
            //Initializing views
            uuid = itemView.findViewById(R.id.uuid);
            major = itemView.findViewById(R.id.major);
            minor = itemView.findViewById(R.id.minor);
            rssi = itemView.findViewById(R.id.rssi);
        }
    }

    @Override
    public AdapterData.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_card_search_data,parent,false));
    }

    @Override
    public void onBindViewHolder(AdapterData.ViewHolder holder, int position) {
        //Getting the key in the list within respective position
        ArrayList<String> key = keys.get(position);
        //Displaying UUID
        holder.uuid.setText(key.get(0));
        //Displaying major
        holder.major.setText(key.get(1));
        //Displaying minor
        holder.minor.setText(key.get(2));
        //Displaying RSSI
        holder.rssi.setText(String.valueOf(beaconsDictionnary.get(key)));
    }
    @Override
    public int getItemCount()
    {
        return keys.size();
    }
}
