package com.example.combiningprojects;

import java.io.Serializable;

/**
 * Created by brend on 02/02/2017.
 */

public class BusStopDescriptor implements Serializable
{
    String serviceCode;
    int routeIndex;
    int stopIndex;
    int globalStopID;

    BusStopDescriptor(String _serviceCode, int _routeIndex, int _stopIndex, int _globalStopID)
    {
        serviceCode = _serviceCode;
        routeIndex = _routeIndex;
        stopIndex = _stopIndex;
        globalStopID = _globalStopID;
    }

    @Override
    public String toString()
    {
        return "Service Code: " + serviceCode + ", RouteIndx: " + routeIndex +
               ", StopIndx: " + stopIndex;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == null)
        {
            return false;
        }
        if(getClass() != obj.getClass())
        {
            return false;
        }
        BusStopDescriptor otherDescriptor = (BusStopDescriptor) obj;
        if(     serviceCode.equals(otherDescriptor.serviceCode) &&
                routeIndex == otherDescriptor.routeIndex &&
                stopIndex == otherDescriptor.stopIndex &&
                globalStopID == otherDescriptor.globalStopID)
        {
            return true;
        }
        return false;
    }

    // TODO - Add getters for the variables.
    public String GetServiceCode(){return serviceCode;}
    public int GetRouteIndex(){return routeIndex;}
    public int GetStopIndex(){return stopIndex;}
    public int GetGlobalStopID(){return globalStopID;}
}