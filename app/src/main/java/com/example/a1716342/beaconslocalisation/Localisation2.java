package com.example.a1716342.beaconslocalisation;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import static java.lang.Math.round;

public class  Localisation2 extends Activity implements BeaconConsumer {

    /*
    This class is the one where the localisation is using
    This class doesn't really use machine learning algorithm,
    even if its principle is the same that a knn with one neighbour used as regression
    */

    private static final String TAG = "Localisation";
    private static final String FILE_NAME = "ignored_beacons_list.dat";
    private static final String REFERENCES_FILE = "reference_list.csv";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    //Layout components
    TextView resultText;
    TextView nearest_rssi;
    TabLayout bar;
    Button scan, stopScan;

    //Beacon Manager
    BeaconManager beaconManager;

    //HashTable containing beacons' information
    //KEY : {UUID, Major, Minor}
    //VALUE : RSSI
    Hashtable<ArrayList<String>, Integer> beaconsDictionnary; //same that BeaconsList

    //List of all the point registered in the file
    //INDEX 0 to N-2 : RSSI
    //LAST INDEX : Area
    ArrayList<ArrayList<Integer>> listReferences;
    ArrayList<Integer> currentRSSIValues, nearestPoint;

    //File and Directory for the file of registered points
    File myDir;
    File referenceFile;

    double distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localisation2);

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
        }
        //Initialising file variables
        myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "data_beacons");
        referenceFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "data_beacons", REFERENCES_FILE);

        //Verifying existence of the directory
        //if it does not, then creating it
        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        //Verifying existence of the file
        //if it does not, then creating it
        if (!referenceFile.exists()) {
            try {
                referenceFile.createNewFile();
                writeLineInFile(referenceFile,"Beacon_1,Beacon_2,Beacon_3,Beacon_4,Area\n");
            } catch (IOException e) {
                Log.e(TAG, "ERROR :" + e.getMessage());
            }
        }

        //getting beaconManager instance (object) for Main Activity class
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconsDictionnary = new Hashtable<ArrayList<String>, Integer>();

        //Setting the navigate bar
        bar = findViewById(R.id.bar);
        bar.addTab(bar.newTab().setText("Localisation"));
        bar.addTab(bar.newTab().setText("Data"));
        bar.addTab(bar.newTab().setText("Beacons"));
        bar.getTabAt(0).select();

        bar.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                beaconManager.unbind(Localisation2.this);
                if (bar.getSelectedTabPosition()==1){
                    Intent beaconsDetection = new Intent(Localisation2.this, Data.class);
                    startActivity(beaconsDetection);
                }
                else if (bar.getSelectedTabPosition()==2){
                    Intent selection = new Intent(Localisation2.this, Beacons.class);
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

        scan = findViewById(R.id.scan_localisation);
        stopScan = findViewById(R.id.stopScan_localisation);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Binding activity to the BeaconService
                beaconManager.bind(Localisation2.this);
                stopScan.setEnabled(true);
                scan.setEnabled(false);
            }
        });
        stopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Unbinding activity to the BeaconService
                beaconManager.unbind(Localisation2.this);
                scan.setEnabled(true);
                stopScan.setEnabled(false);
            }
        });

        resultText = findViewById(R.id.result);
        nearest_rssi = findViewById(R.id.nearest_rssi);

        listReferences = new ArrayList<>();
        currentRSSIValues = new ArrayList<>();
        fillListReferences();
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

                Log.i(TAG, "Current activity : " + this.toString());

                //Updating the hashtable
                if (beacons.size() > 0) {
                    // Iterating through all Beacons from Collection of Beacons
                    for (Beacon b : beacons) {
                        ArrayList<String> beaconID = new ArrayList<String>();
                        beaconID.add(b.getId1().toString());
                        beaconID.add(b.getId2().toString());
                        beaconID.add(b.getId3().toString());

                        if (!isIgnored(beaconID)) {
                            //If the beacon is not in the hashtable, then adding it
                            if (beaconsDictionnary.get(beaconID) == null) {
                                beaconsDictionnary.put(beaconID, b.getRssi());
                            }
                            //Else change his RSSI in the hashtable
                            else {
                                beaconsDictionnary.replace(beaconID, b.getRssi());
                            }
                        }
                    }
                    currentRSSIValues.clear();
                    ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(beaconsDictionnary.keySet());
                    for(ArrayList<String> key:keys){
                        currentRSSIValues.add(beaconsDictionnary.get(key));
                    }

                    //Set the nearest Point
                    int indexNearestPoint = 0;
                    double minDistance =  distance(new ArrayList<Integer>(listReferences.get(indexNearestPoint).subList(0,currentRSSIValues.size()-1)),currentRSSIValues);
                    for (int i = 1; i<listReferences.size();i++){
                        if(distance(new ArrayList<Integer>(listReferences.get(i).subList(0,currentRSSIValues.size()-1)),currentRSSIValues) < minDistance){
                            indexNearestPoint = i;
                            minDistance = distance(new ArrayList<Integer>(listReferences.get(i).subList(0,currentRSSIValues.size()-1)),currentRSSIValues);
                        }
                    }
                    distance = minDistance;
                    nearestPoint = listReferences.get(indexNearestPoint);

                    //Display the area of the nearest Point
                    Localisation2.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resultText.setText(String.valueOf(nearestPoint.get(nearestPoint.size()-1)+1));
                                nearest_rssi.setText(nearestPoint.toString());

                            } catch (Exception e) {
                                Log.e(TAG, "ERROR : " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
        try {
            //Tells the BeaconService to start looking for beacons that match the passed Region object.
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            Log.e(TAG, "ERROR : " + e.getMessage());
        }
    }

    public boolean isIgnored(ArrayList<String> beacon) {
        /*
        This method allows  to know if a beacon is ignored (see the activity Selection)
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
            if (listBeacons.contains(beacon)) return true;
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

    public void writeLineInFile(File file, String line)
    {
        /*
        This method allows to write the string argument in the file argument
        */
        FileOutputStream fOut;
        try {
            //writing in the file
            fOut = new FileOutputStream(file, true);
            fOut.write(line.getBytes());
            fOut.close();
        } catch (IOException e) {
            Log.e(TAG, "ERROR : " + e.getMessage());
        }
    }

    public void fillListReferences() {
        /*
        This method fills the list listReference with the contents of the file referenceFile
         */
        FileReader fr;
        BufferedReader br;
        String[] elements;
        listReferences.clear();
        ArrayList<Integer> referencePoint;
        try {
            fr = new FileReader(referenceFile);
            br = new BufferedReader(fr);

            String line = br.readLine();

            for (line = br.readLine(); line != null; line = br.readLine()){
                referencePoint = new ArrayList<>();
                elements = line.split(",");
                for(int i =0; i<elements.length-1; i++){
                    referencePoint.add(Integer.parseInt(elements[i]));
                }
                switch (elements[elements.length-1]) {
                    case "A":
                        referencePoint.add(0);
                        break;
                    case "B":
                        referencePoint.add(1);
                        break;
                    case "C":
                        referencePoint.add(2);
                        break;
                    case "D":
                        referencePoint.add(3);
                        break;
                    case "E":
                        referencePoint.add(4);
                        break;
                }

                listReferences.add(( ArrayList<Integer>) referencePoint.clone());
            }
            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double distance(ArrayList<Integer> point1, ArrayList<Integer> point2){
        double distance = 0;
        for (int i = 0; i<point1.size(); i++){
            distance+=(point1.get(i)-point2.get(i))*(point1.get(i)-point2.get(i));
        }
        return Math.sqrt(distance);
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
        }
    }

}
