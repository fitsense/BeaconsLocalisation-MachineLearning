package com.example.a1716342.beaconslocalisation;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

public class Data2 extends Activity implements BeaconConsumer {

    /*
    This class allows to create a dataset
    LINE  : RSSI1, RSSI2, RSSI3, RSSI4, X, Y
    */
    private static final String TAG = "Beaconsdetection";
    private static final String FILE_NAME = "ignored_beacons_list.dat";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    //Variables for saving data
    File myFile;
    File myDir;

    //Beacon Manager
    private BeaconManager beaconManager;

    //HashTable containing beacons' information
    //KEY : {UUID, Major, Minor}
    //VALUE : RSSI
    Hashtable<ArrayList<String>, Integer> beaconsList;

    //Layout Components
    Button scan, stopScan, save, newFile;
    TabLayout bar;
    EditText x, y;

    //Recycler view
    private RecyclerView rv;
    private RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data2);

        //Checking the state of the required permissions
        //See : https://altbeacon.github.io/android-beacon-library/requesting_permission.html
        //See : https://developer.android.com/training/permissions/requesting
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

        //Initialising file variables
        myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "data_beacons");
        myFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "data_beacons", "test_triangulation.csv");

        //Verifying existence of the directory
        //if it does not, then creating it
        if (!myDir.exists()) {
            myDir.mkdirs();
            Log.i(TAG, "Creation du repertoire");
        }
        Log.i(TAG, "Existence du dossier : " + myDir.exists());

        //Verifying existence of the file
        //if it does not, then creating it
        if (!myFile.exists()) {
            try {
                myFile.createNewFile();
                writeLineInFile(myFile,"Beacon_1,Beacon_2,Beacon_3,Beacon_4,Area\n");
                Log.i(TAG, "Creation du fichier");
            } catch (IOException e) {
                Log.e(TAG, "ERROR :" + e.getMessage());
            }
        }
        Log.i(TAG, "Existence du fichier : " + myFile.exists());

        //getting beaconManager instance (object) for Main Activity class
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        //Setting the navigate bar
        bar = findViewById(R.id.bar);
        bar.addTab(bar.newTab().setText("Localisation"));
        bar.addTab(bar.newTab().setText("Data"));
        bar.addTab(bar.newTab().setText("Beacons"));
        bar.getTabAt(1).select();

        bar.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                beaconManager.unbind(Data2.this);
                if (bar.getSelectedTabPosition()==0){
                    Intent localisation = new Intent(Data2.this, Localisation2.class);
                    startActivity(localisation);
                }
                else if (bar.getSelectedTabPosition()==2){
                    Intent selection = new Intent(Data2.this, Beacons.class);
                    startActivity(selection);
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        //Initialising Layout components
        scan = findViewById(R.id.scan_detection);
        stopScan = findViewById(R.id.stopScan_detection);
        save = findViewById(R.id.save);
        newFile = findViewById(R.id.new_file);
        x = findViewById(R.id.X);
        y = findViewById(R.id.Y);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Binding activity to the BeaconService
                beaconManager.bind(Data2.this);
                stopScan.setEnabled(true);
                scan.setEnabled(false);
            }
        });

        stopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Unbinding activity to the BeaconService
                beaconManager.unbind(Data2.this);
                scan.setEnabled(true);
                stopScan.setEnabled(false);
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<ArrayList<String>> keys;
                String line="";

                //creating the line
                keys = beaconsList.keySet();
                for (ArrayList<String> key:keys){
                    line+= beaconsList.get(key)+",";
                }
                line+=x.getText()+","+y.getText();
                line+="\n";
                Log.i(TAG, "Ligne ajoutee :"+line);


                writeLineInFile(myFile, line);
            }
        });

        newFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Verifying existence of the file
                //if it does not, then creating it
                if (!myFile.exists()) {
                    try {
                        myFile.createNewFile();
                        writeLineInFile(myFile,"Beacon_1,Beacon_2,Beacon_3,Beacon_4,Area\n");
                        Log.i(TAG, "Creation du fichier");
                    } catch (IOException e) {
                        Log.e(TAG, "ERROR :" + e.getMessage());
                    }
                }
                //else, deleting it then creating it in order to have a empty file
                else {
                    try {
                        myFile.delete();
                        myFile.createNewFile();
                        writeLineInFile(myFile,"Beacon_1,Beacon_2,Beacon_3,Beacon_4,Area\n");
                        Log.i(TAG, "Creation du fichier");
                    } catch (IOException e) {
                        Log.e(TAG, "ERROR :" + e.getMessage());
                    }

                }
                Log.i(TAG, "Existence du fichier : " + myFile.exists());
            }
        });

        beaconsList = new Hashtable<ArrayList<String>, Integer>();

        rv = (RecyclerView) findViewById(R.id.search_recycler_detection);
        rv.setLayoutManager(new LinearLayoutManager(Data2.this));
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
                //Setting all the RSSI to 0. The device which are no longer detected will stay to 0, the others will change.
                //Set<ArrayList<String>> keys = beaconsList.keySet();
                //for (ArrayList<String> key:keys){
                //    beaconsList.replace(key,0);
                //}
                Log.i(TAG, "Activite courante :" + this.toString());

                //If Beacon is detected then size of collection is > 0
                if (beacons.size() > 0) {
                    // Iterating through all Beacons from Collection of Beacons
                    for (Beacon b : beacons) {
                        ArrayList<String> beaconID = new ArrayList<String>();
                        beaconID.add(b.getId1().toString());
                        beaconID.add(b.getId2().toString());
                        beaconID.add(b.getId3().toString());

                        if (!isIgnored(beaconID)){
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
                }
                try {
                    Data2.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Setting up the Adapter for Recycler View
                            ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(beaconsList.keySet());
                            adapter = new AdapterData(beaconsList,keys);
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

    public void writeLineInFile(File file, String line)
        /*
        This method write the string argument in the file argument
        */
    {
        FileOutputStream fOut;
        try {
            //writing in the file
            fOut = new FileOutputStream(file, true);
            Log.i(TAG, " Fichier ouvert");
            fOut.write(line.getBytes());
            Log.i(TAG, "Ligne ajoutee");
            fOut.close();
            Log.i(TAG, "Fichier ferme");
        } catch (IOException e) {
            Log.e(TAG, "ERROR : " + e.getMessage());
        }
    }

    public boolean isIgnored(ArrayList<String> beacon){
        /*
        This method allows to know if a beacon is ignored (see the activity Selection)
        */
        FileInputStream fis;
        ObjectInputStream ois;
        ArrayList<ArrayList<String>> listBeacons;
        try {
            fis = openFileInput(FILE_NAME);
            ois = new ObjectInputStream(fis);
            listBeacons = (ArrayList<ArrayList<String>>) ois.readObject();
            ois.close();
            fis.close();
            if  (listBeacons.contains(beacon)) return true;
            else return false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
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
            beaconManager.unbind(this);
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

    //See : https://altbeacon.github.io/android-beacon-library/requesting_permission.html
    //See : https://developer.android.com/training/permissions/requesting
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
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
