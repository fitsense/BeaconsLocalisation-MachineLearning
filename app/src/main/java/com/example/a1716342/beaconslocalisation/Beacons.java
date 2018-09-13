package com.example.a1716342.beaconslocalisation;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

public class Beacons extends Activity implements BeaconConsumer {

    /*
    This class allows to ignore beacons which don't matter for an experiment
     */
    private static final String TAG = "Selection";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    //Layout components
    TabLayout bar;

    //Beacon Manager
    BeaconManager beaconManager;

    File myFile;

    //HashTable containing beacons' information
    //KEY : {UUID, Major, Minor}
    //VALUE : RSSI
    Hashtable<ArrayList<String>,Integer> beaconsList;

    //Recycler view
    private RecyclerView rv;
    private RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacons);

        //Checking the state of the required permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check...
            //... for coarse location
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
            //... to write in external storage
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        //getting beaconManager instance (object) for Main Activity class
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        myFile = new File(this.getFilesDir(),"ignored_beacons_list.dat");
        //Create the file if it does not exist
        if (!myFile.exists()) {
            try {
                myFile.createNewFile();
                Log.i(TAG, "Creation du fichier");
            } catch (IOException e) {
                Log.e(TAG, "ERROR :" + e.getMessage());
            }
        }

        //Setting the navigate bar
        bar = findViewById(R.id.bar);
        bar.addTab(bar.newTab().setText("Localisation"));
        bar.addTab(bar.newTab().setText("Data"));
        bar.addTab(bar.newTab().setText("Beacons"));
        bar.getTabAt(2).select();

        bar.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                beaconManager.unbind(Beacons.this);
                if (bar.getSelectedTabPosition()==0){
                    Intent localisation = new Intent(Beacons.this, Localisation.class);
                    startActivity(localisation);
                }
                else if (bar.getSelectedTabPosition()==1){
                    Intent beaconsDetection = new Intent(Beacons.this, Data.class);
                    startActivity(beaconsDetection);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        //Setting beaconList
        beaconsList = new Hashtable<ArrayList<String>, Integer>();

        //Setting Adapter
        rv = findViewById(R.id.search_recycler_selection);
        rv.setLayoutManager(new LinearLayoutManager(Beacons.this));

        //Binding activity to the BeaconService
        beaconManager.bind(Beacons.this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        //Specifies a class that should be called each time the BeaconService gets ranging data,
        // which is nominally once per second when beacons are detected.
        beaconManager.addRangeNotifier(new RangeNotifier() {
            /*
               This Override method tells us all the collections of beacons and their details that
               are detected within the range by device
             */
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Log.i(TAG, "Activite courante :" + this.toString());
                //If Beacon is detected then size of collection is > 0
                if (beacons.size() > 0) {
                    // Iterating through all Beacons from Collection of Beacons
                    for (Beacon b : beacons) {
                        ArrayList<String> beaconID = new ArrayList<String>();
                        beaconID.add(b.getId1().toString());
                        beaconID.add(b.getId2().toString());
                        beaconID.add(b.getId3().toString());

                        //If the beacon is not in the hashtable, then adding it
                        if (beaconsList.get(beaconID) == null) {
                            beaconsList.put(beaconID, b.getRssi());
                        }
                        //Else change his RSSI in the hashtable
                        else {
                            beaconsList.replace(beaconID, b.getRssi());
                        }
                    }
                }
                try {
                    Beacons.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Setting up the Adapter for Recycler View
                            ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(beaconsList.keySet());
                            adapter = new AdapterBeacons(beaconsList,keys);
                            rv.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }catch(Exception e){ }
            }
        });
        try {
            //Tells the BeaconService to start looking for beacons that match the passed Region object.
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) { }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Unbinds an Android Activity or Service to the BeaconService to avoid leak.
        beaconManager.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager.isBound(this)) {
            beaconManager.setBackgroundMode(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager.isBound(this)) {
            beaconManager.setBackgroundMode(false);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        /*
        This method is used to ask for permissions
        */
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                break;
            }
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //Granted.
                    Log.i(TAG,"Write in external storage permission granted.");
                }
                else{
                    //Denied.
                    Log.e(TAG,"Permission denied, sorry.");
                }
                break;
            }
        }
    }
}
