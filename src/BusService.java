package com.example.combiningprojects;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by brend on 02/02/2017.
 */

    // A Bus Service is comporsed of multiple routes
    // Usually this will be two routes, one going each direction.
    // There may be more routes if the service varies due to time of day or day of the week, bank holidays etc.

class BusService implements Serializable
{
    class Route implements Serializable
    {
        public ArrayList<BusStop> stops = new ArrayList<BusStop>();

		// transient as we don't wnat to to be saved.
        public transient BusStop[] stopsBasicArray;

        public void CreateStopsBasicArray()
        {
            stopsBasicArray = new BusStop[stops.size()];
            stops.toArray(stopsBasicArray); // fill the array
        }
    }

    BusService(String _serviceID)
    {
        serviceID = _serviceID;
    }

    String serviceID;
    ArrayList<Route> routes = new ArrayList<Route>();

    public String GetServiceID()
    {
        return serviceID;
    }

    public void AddRoute()
    {
        routes.add(new Route());
    }

    public void AddStopToRoute(int routeIndex, BusStop busStop)
    {
        routes.get(routeIndex).stops.add(busStop);
    }

    public Route GetRoute(int routeIndex)
    {
        return routes.get(routeIndex);
    }

    public int GetRouteCount()
    {
        return routes.size();
    }
}