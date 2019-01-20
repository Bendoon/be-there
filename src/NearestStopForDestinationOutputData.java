package com.example.combiningprojects;

import android.location.Location;

import java.util.HashMap;
import java.util.List;

public class NearestStopForDestinationOutputData {

    public List<List<HashMap<String, String>>> routes;
    public String publicServiceCode;
    public Location destLatLng;
    public List<BusStopPair> busRoutes;
    public List<List<Vertex2>> listOfPaths;

    public NearestStopForDestinationOutputData( Location destLatLng, List<List<Vertex2>> listOfPaths )
    {
        this.destLatLng = destLatLng;
        this.listOfPaths = listOfPaths;
    }

    public List<List<HashMap<String, String>>> GetRoutes() {return routes;}
    public String GetPublicServiceCode() {return publicServiceCode;}
    public Location GetDestLatLng() {return destLatLng;}
    public List<BusStopPair> GetBusRoutes(){return busRoutes;}
}