package com.example.combiningprojects;

import android.location.Location;

import java.util.LinkedList;
import java.util.List;

public class PolylineGathererInputData
{
    Location userLocation;
    private Location destinationLocation;
    List<List<Vertex2>> listOfPaths;
    int pathToProcessIdx;

    public PolylineGathererInputData(Location _userLocation, Location _destinationLocation, List<List<Vertex2>> _listOfPaths, int _pathToProcessIdx) {
        destinationLocation = _destinationLocation;
        userLocation = _userLocation;
        listOfPaths = _listOfPaths;
        pathToProcessIdx = _pathToProcessIdx;
    }

    public Location GetUserLocation() {return userLocation;}
    public Location GetDestination() {return destinationLocation;}
    public List<List<Vertex2>> GetListOfPaths() {return listOfPaths;}
}