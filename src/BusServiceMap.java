package com.example.combiningprojects;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by brend on 02/02/2017.
 */

public class BusServiceMap implements Serializable
{
    public static HashMap<String, BusService> hashMap = new HashMap<String, BusService>();

    public static BusStop GetStopFromDescriptor(BusStopDescriptor descriptor)
    {
        /*
        if(descriptor == null)
        {
            System.out.print("Bleh");
        }
        */
        BusService busService = hashMap.get(descriptor.serviceCode);
        BusService.Route route = busService.GetRoute(descriptor.routeIndex);
        BusStop stop = route.stops.get(descriptor.stopIndex);

        return stop;
    }
}
