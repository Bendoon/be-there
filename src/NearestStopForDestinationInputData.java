package com.example.combiningprojects;

import android.location.Location;

public class NearestStopForDestinationInputData
{
    Location userLocation;
    private String destination;
    private Boolean multiMode;
    public Boolean useDijkstras;

    public NearestStopForDestinationInputData(Location _userLocation, String _destination, boolean _multiMode, boolean _useDijkstras) {
        destination = _destination;
        userLocation = _userLocation;
        multiMode = _multiMode;
        useDijkstras = _useDijkstras;
    }

    public Location GetUserLocation() {return userLocation;}
    public String GetDestination() {return destination;}
    public boolean GetMultiMode() {return multiMode;}
    public boolean GetDijkstra() {return useDijkstras;}
}