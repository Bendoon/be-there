package com.example.combiningprojects;

public class NearestStopForRouteData {
    private double lng;
    private double lat;
    private String publicServiceCode; // The bus route ID, 220x etc

    public NearestStopForRouteData(double _lat, double _lng, String _publicServiceCode) {
        lng = _lng;
        lat = _lat;
        publicServiceCode = _publicServiceCode;
    }

    public double GetLng() {return lng;}
    public double GetLat() {return lat;}
    public String GetPublicServiceCode() {return publicServiceCode;}
}