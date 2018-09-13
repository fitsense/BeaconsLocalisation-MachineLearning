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

import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import static java.lang.Math.round;

public class Localisation3 extends Activity implements BeaconConsumer {

    /*
    This class is the one where the localisation is using
    It uses machine learning with RSSI as data to predict coordinates
    Class MapMaker is then used to get the area from coordinates
    */
    private static final String TAG = "Localisation";
    private static final String FILE_NAME = "ignored_beacons_list.dat";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    //Mode of computing
    //1 : simple
    //2 : with rssi average
    private static final int MODE = 1;

    //Layout components
    TextView coordinates, area;
    TabLayout bar;
    Button scan, stopScan;

    //Beacon Manager
    BeaconManager beaconManager;

    //HashTable containing beacons' information
    //KEY : {UUID, Major, Minor}
    //VALUE : RSSI
    Hashtable<ArrayList<String>, Integer> beaconsList;

    //Weka variables
    ConverterUtils.DataSource source;
    Instances dataX, dataY;
    Instance instance;

    //Models
    Classifier modelX, modelY;

    MapMaker map;

    RSSIAverage rssiAverage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localisation3);

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

        //getting beaconManager instance (object) for Main Activity class
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconsList = new Hashtable<ArrayList<String>, Integer>();

        //Setting the navigate bar
        bar = findViewById(R.id.bar);
        bar.addTab(bar.newTab().setText("Localisation"));
        bar.addTab(bar.newTab().setText("Data"));
        bar.addTab(bar.newTab().setText("Beacons"));
        bar.getTabAt(0).select();

        bar.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                beaconManager.unbind(Localisation3.this);
                if (bar.getSelectedTabPosition()==1){
                    Intent beaconsDetection = new Intent(Localisation3.this, Data2.class);
                    startActivity(beaconsDetection);
                }
                else if (bar.getSelectedTabPosition()==2){
                    Intent selection = new Intent(Localisation3.this, Beacons.class);
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
                beaconManager.bind(Localisation3.this);
                stopScan.setEnabled(true);
                scan.setEnabled(false);
            }
        });
        stopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Unbinding activity to the BeaconService
                beaconManager.unbind(Localisation3.this);
                scan.setEnabled(true);
                stopScan.setEnabled(false);
            }
        });

        coordinates = findViewById(R.id.coordinates);
        area = findViewById(R.id.area);

        try {
            //Getting data from the dataset
            source = new ConverterUtils.DataSource(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "data_beacons" + File.separator + "training_coordinates_smooth.csv");

            //Model for X
            Log.i(TAG, "Donnees importees");
            dataX = source.getDataSet();
            Log.i(TAG, "Donnees chargees");
            dataX.deleteAttributeAt(5);
            dataX.setClassIndex(dataX.numAttributes() - 1);

            //Splitting data in a training set and a testing set
            dataX.randomize(new java.util.Random(0));
            int trainsizeX = (int) Math.round(dataX.numInstances() * 0.8);
            int testsizeX = dataX.numInstances() - trainsizeX;
            Instances trainX = new Instances(dataX, 0, trainsizeX);
            Instances testX = new Instances(dataX, trainsizeX, testsizeX);

            //Creating and training the model
            modelX = new IBk(6);
            modelX.buildClassifier(dataX);

            //Evaluation evalX = new Evaluation(trainX);
            //evalX.evaluateModel(modelX, testX);

            //Model for Y
            dataY = source.getDataSet();
            Log.i(TAG, "Donnees chargees");
            dataY.deleteAttributeAt(4);
            dataY.setClassIndex(dataY.numAttributes() - 1);

            //Splitting data in a training set and a testing set
            dataY.randomize(new java.util.Random(0));
            int trainsizeY = (int) Math.round(dataY.numInstances() * 0.8);
            int testsizeY = dataY.numInstances() - trainsizeY;
            Instances trainY = new Instances(dataY, 0, trainsizeY);
            Instances testY = new Instances(dataY, trainsizeY, testsizeY);


            modelY = new IBk(6);
            modelY.buildClassifier(dataY);

            //Evaluation evalY = new Evaluation(trainY);
            //evalY.evaluateModel(modelY, testY);

            //Log.i(TAG, evalX.toSummaryString());
            //Log.i(TAG, evalY.toSummaryString());

        } catch (Exception e) {
            Log.e(TAG, "ERROR : "+e.getMessage());
            e.printStackTrace();
        }

        map = new MapMaker();
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

                Log.i(TAG, "Activite courante : " + this.toString());

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

                if (beaconsList.size() + 1 == dataX.numAttributes()) {
                    ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(beaconsList.keySet());
                    ArrayList<Integer> listRSSI = new ArrayList<Integer>();

                    for (int i = 0; i < beaconsList.size(); i++){
                        listRSSI.add(beaconsList.get(keys.get(i)));
                    }
                    switch (MODE) {
                        case 1:
                            Log.i(TAG, "mode 1 : simple");
                            //Creating the instance which will be gave to the model
                            instance = new DenseInstance(beaconsList.size() + 1);
                            instance.setDataset(dataX);
                            for (int i = 0; i < beaconsList.size(); i++) {
                                instance.setValue(i,listRSSI.get(i));
                            }
                            break;
                        case 2:
                            Log.i(TAG, "mode 2 : rssi average");
                            //Getting the mean of the rssi
                            ArrayList<Double> mean;
                            if(rssiAverage.queueSize()>=rssiAverage.size()){
                                rssiAverage.pollFirst();
                            }
                            rssiAverage.addLast((ArrayList<Integer>)listRSSI.clone());
                            mean = rssiAverage.RSSIMean();

                            //Creating the instance which will be gave to the model
                            instance = new DenseInstance(beaconsList.size() + 1);
                            instance.setDataset(dataX);
                            for (int i = 0; i < beaconsList.size(); i++) {
                                instance.setValue(i, mean.get(i));
                            }
                    }


                    //Classifying the instance and displaying it
                    Localisation3.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int resultX = (int) round(modelX.classifyInstance(instance));
                                int resultY = (int) round(modelY.classifyInstance(instance));
                                coordinates.setText("Coordinates : "+resultX+"; "+resultY);
                                area.setText("Area : "+map.getArea(resultX,resultY, 0));
                            } catch (Exception e) {
                                Log.e(TAG, "ERROR : " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    Localisation3.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                coordinates.setText("ERROR : Number of detected beacons is wrong (" + String.valueOf(beaconsList.size()) + " instead of " + String.valueOf(dataX.numAttributes() - 1) + ")");
                                area.setText("ERROR : Number of detected beacons is wrong (" + String.valueOf(beaconsList.size()) + " instead of " + String.valueOf(dataX.numAttributes() - 1) + ")");
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
