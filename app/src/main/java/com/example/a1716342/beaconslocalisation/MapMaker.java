package com.example.a1716342.beaconslocalisation;


public class MapMaker {

    /*
    This class allows to get the area matching with the input coordinates
    It works for the office only, and for one segmentation only
    */

    public MapMaker(){}

    public int getArea(double x, double y, double z){
        if(y<3.4){
            if (x<6){
                return 1;
            } else{
                return 2;
            }
        }
        else {
            if (x > 3.85 && x < 7.85) {
                return 3;
            }
            else {
                return 4;
            }
        }
    }
}