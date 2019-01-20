package com.example.combiningprojects;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by brend on 15/01/2017.
 */
//TODO Make this class only store stops that have not already been stored, as duplicates are in the massive list.
//TODO Store more than one stopref to allow multiple routes

//Finds nearby bus stops to a destination provided by the user
public class BusStopHelper
{
        //Filters out every public service code from the list of nearest hits.
        public static  ArrayList<String> getUniquePublicServiceCodes(BusStopDescriptor[] nearestStops)
        {
            int count = 0;
            ArrayList<String> uniqueServiceCodes = new ArrayList<String>();
            //Once we have our list of ten stops we are going to filter out the different public service codes until we have all the unique service codes from the list.
            for(int p = 0; p < nearestStops.length; ++p)
            {
                if(nearestStops[p] != null)
                {
                    //if there is nothing in the array then add a bus stop as the starting point
                    if(uniqueServiceCodes.isEmpty())
                    {
                        uniqueServiceCodes.add(BusServiceMap.GetStopFromDescriptor(nearestStops[p]).getPublicServiceCode());
                        //uniqueServiceCodes.add(nearestStops[p]);
                    }
                    //we need to check if the service code is already in the list
                    else
                    {
                        boolean toAdd = true;
                        for( int n = 0; n < uniqueServiceCodes.size(); ++n )
                        {
                            //if none of the public service codes of bus stops that have been added match this one then add it
                            if(uniqueServiceCodes.get(n).equals(BusServiceMap.GetStopFromDescriptor(nearestStops[p]).getPublicServiceCode()))
                            {
                                toAdd = false;
                                //if at any point this is hit then its already in the list so break from loop
                                break;
                            }
                        }
                        if(toAdd)
                        {
                            uniqueServiceCodes.add(BusServiceMap.GetStopFromDescriptor(nearestStops[p]).getPublicServiceCode());
                        }
                    }
                }
            }
            return uniqueServiceCodes;
        }

    //Finds the top X number of busstops near a location as the crow flies
    public static BusStopDescriptor[] crowFliesNearestBusStops(int n, Location location)
    {
        //TODO - this may cause errors as I am setting setDistanceFromUserLocation for the user stops and the destination stops and just overwriting should have seperate for destiantion.
        int counter = 0;
        int numStopsRecorded = 0;
        float distance;
        BusStopDescriptor[] nearestStops = new BusStopDescriptor[n]; //array for holding the top n nearest stop(s).

        //Loop over the data of all the bus stops stored and find the distance as the crow flies to the nearest stops
        //TODO get this to store multiple routes
        for (int i = 0; i < BusServiceMap.hashMap.size(); ++i)
        {
            String currentServiceCode = PopulateBusStopsNewAsync.busRoutes[i];
            if (BusServiceMap.hashMap.isEmpty())
            {
                break;
            }
            for(int k = 0; k < BusServiceMap.hashMap.get(currentServiceCode).routes.size(); ++k)
            {

                for (int p = 0; p < BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.size(); ++p) {
                    if (BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.get(p) == null) {
                        break;
                    }
                    Location currentStop = new Location("currentStop");

                    BusStop theBusStop = BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.get(p);

                    currentStop.setLatitude(theBusStop.getLat());
                    currentStop.setLongitude(theBusStop.getLng());

                    //Get as the crow flies distance
                    distance = location.distanceTo(currentStop);

                    //CustomLogger.println(counter + ") Distance from user location to " +
                    //        theBusStop.getName()
                    //        + " is: " + distance);

                    theBusStop.setDistanceFromUserLocation(distance,location);

                    if (numStopsRecorded < nearestStops.length) {
                        // Initial list has not been filled yet
                        // Just add it, we haven't added the minimum quota yet

                        BusStopDescriptor descriptorToAdd = new BusStopDescriptor(currentServiceCode, k, p, theBusStop.getGlobalStopID());
                        if(!IsDescriptorInArray(nearestStops, descriptorToAdd, false))
                        {
                            nearestStops[numStopsRecorded] = descriptorToAdd; //Service code, route index and stop index
                            ++numStopsRecorded;
                        }
                    } else {
                        // Need to find the furtherst away one and replace it
                        int furthestDistanceFoundIdx = 0;
                        float furthestDistanceFound = BusServiceMap.GetStopFromDescriptor(nearestStops[furthestDistanceFoundIdx]).getDistanceFromUserLocation(location);

                        for (int m = 1; m < nearestStops.length; ++m) {
                            float tempDistance = BusServiceMap.GetStopFromDescriptor(nearestStops[m]).getDistanceFromUserLocation(location);
                            if (tempDistance > furthestDistanceFound) {
                                furthestDistanceFoundIdx = m;
                                furthestDistanceFound = tempDistance;
                            }
                        }

                        // If the new stop is closer than our furthest away one, replace it with the new one
                        if (theBusStop.getDistanceFromUserLocation(location) < furthestDistanceFound)
                        {
                            BusStopDescriptor descriptorToAdd = new BusStopDescriptor(currentServiceCode, k, p, theBusStop.getGlobalStopID());
                            if(!IsDescriptorInArray(nearestStops, descriptorToAdd, false))
                            {
                                nearestStops[furthestDistanceFoundIdx] = descriptorToAdd;
                            }
                        }

                    }
                    counter++;
                }
            }
        }
        return nearestStops;
    }

    static Boolean IsDescriptorInArray(BusStopDescriptor[] descriptors, BusStopDescriptor descriptorToFind,  Boolean careWhichRoute)
    {
        for(int x = 0; x < descriptors.length; ++x)
        {
            if( descriptors[x] != null &&
                descriptors[x].serviceCode.equals(descriptorToFind.serviceCode) &&
                descriptors[x].globalStopID == descriptorToFind.globalStopID)
            {
                if(!careWhichRoute)
                {
                    // It's already in the list, we don't care if it's the same route or not
                    return true;
                }
                else
                {
                    // Is the route the same one?
                    if(descriptors[x].routeIndex == descriptorToFind.routeIndex)
                    {
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
            }
        }

        return false;
    }

    //Get the three best possibilities for the route, best will always be 0
    public static List<BusStopPair> orderBusStopPairs(List<BusStopPair> busRoutes, Location userLoc, Location destLoc)
    {
        List<BusStopPair> sortedBusStopPairs = new ArrayList<>();
        //Create the list and cast it so we can compare
        List<BusStopPair> orderedBestRoutes = busRoutes;

        //Assign all the routes a total walking time
        for(int k=0;k<busRoutes.size();++k)
        {
            busRoutes.get(k).SetTotalWalkingTimeForOriginAndDestination(BusServiceMap.GetStopFromDescriptor(busRoutes.get(k).GetOriginStop()).getWalkingTimeSeconds(userLoc)
                                                                        + BusServiceMap.GetStopFromDescriptor(busRoutes.get(k).GetDestinationStop()).getWalkingTimeSeconds(destLoc));
        }

        //Sort these in order of total walking time, lowest to highest
        Collections.sort(orderedBestRoutes);

        //take up to the first 3
        int possibileRoutes = orderedBestRoutes.size();
        if(possibileRoutes > 3)
        {
            orderedBestRoutes = orderedBestRoutes.subList(0,3);
        }

        return orderedBestRoutes;
    }

    //Finds the nearest stop on a route from a location
    // TODO - This should return a BusStopDescriptor instead of a BusStop
    public static BusStopDescriptor FindNearestStop(String busRoute, Location loc) throws JSONException
    {
       return FindNearestStop(busRoute, loc, -1);
    }

    // Uses the distances set within each BusStop element to return the top hit up to a limit of numStopsLimit
    //TODO This is fucking up our logic as it loops over all the routes instead of just our route index
    private static BusStopDescriptor[] GetNearestBusStops(BusService busService, int numStopsLimit, Location loc, int specificRouteLookup)
    {
        int startRouteIdx = 0;
        int endRouteIdx = busService.routes.size() - 1;

        if(specificRouteLookup >= 0 )
        {
            startRouteIdx = specificRouteLookup;
            endRouteIdx = startRouteIdx;
        }

        int numStopsRecorded = 0;

        if(numStopsLimit == 0)
        {
            // Just going to return the full list, but sorted
            numStopsLimit = busService.routes.size();
        }

        //BusStop[] nearestStopsInternal = new BusStop[numStopsLimit];
        BusStopDescriptor[] nearestStopsInternal = new BusStopDescriptor[numStopsLimit];

        for(int p = startRouteIdx; p <= endRouteIdx; ++p )
        {
            for (int i = 0; i < busService.routes.get(p).stops.size(); ++i)
            {
                BusStop currentBusStop = busService.routes.get(p).stops.get(i);

                if (numStopsRecorded < nearestStopsInternal.length)
                {
                    // Initial list has not been filled yet
                    // Just add it, we haven't added the minimum quota yet
                    //nearestStopsInternal[numStopsRecorded] = busService.routes.get(p).stops.get(i);

                    nearestStopsInternal[numStopsRecorded] = new BusStopDescriptor( currentBusStop.getPublicServiceCode(),
                                                                                    p, i, currentBusStop.getGlobalStopID());
                    ++numStopsRecorded;
                }
                else
                {
                    // Need to find the furtherst away one and replace it
                    int furthestDistanceFoundIdx = 0;
                    //float furthestDistanceFound = nearestStopsInternal[furthestDistanceFoundIdx].getDistanceFromUserLocation();
                    float furthestDistanceFound = BusServiceMap.GetStopFromDescriptor(nearestStopsInternal[furthestDistanceFoundIdx]).getDistanceFromUserLocation(loc);

                    for (int k = 1; k < nearestStopsInternal.length; ++k) {
                        //float tempDistance = nearestStopsInternal[k].getDistanceFromUserLocation();
                        float tempDistance = BusServiceMap.GetStopFromDescriptor(nearestStopsInternal[k]).getDistanceFromUserLocation(loc);
                        if (tempDistance > furthestDistanceFound) {
                            furthestDistanceFoundIdx = k;
                            furthestDistanceFound = tempDistance;
                        }
                    }

                    // If the new stop is closer than our furthest away one, replace it with the new one
                    if (currentBusStop.getDistanceFromUserLocation(loc) < furthestDistanceFound) {
                        //nearestStopsInternal[furthestDistanceFoundIdx] = busService.routes.get(p).stops.get(i);
                        nearestStopsInternal[furthestDistanceFoundIdx] = new BusStopDescriptor( currentBusStop.getPublicServiceCode(),
                                                                                                p, i, currentBusStop.getGlobalStopID() );
                    }
                }
            }

            if(specificRouteLookup >= 0)
            {
                break;
            }
        }

        return nearestStopsInternal;
    }

    public static BusStopPair GetBestValidRoute(Location userLocation, Location destination, String serviceCode) throws JSONException {
        //Grab the service and all routes
        BusService theService = BusServiceMap.hashMap.get(serviceCode);

        List<BusStopPair> routeOptions = new ArrayList<BusStopPair>();

        //Loop over all the routes on the particular service
        for(int routeIndex = 0; routeIndex < theService.GetRouteCount(); ++routeIndex)
        {
            //Grab the nearest stops descriptors for the user and dest
            BusStopDescriptor nesrestStopToUser = FindNearestStop(serviceCode,userLocation, routeIndex);
            BusStopDescriptor nearestStopToDest = FindNearestStop(serviceCode,destination, routeIndex);

            //Check for going the right way, toute validity.
            if(nesrestStopToUser.stopIndex < nearestStopToDest.stopIndex)
            {
                // Valid route!
                // TODO - This is where you could put time restrictions

                BusStopPair validPairing = new BusStopPair(nesrestStopToUser, nearestStopToDest);
                routeOptions.add(validPairing);
                //return validPairing;
            }
        }

        if(routeOptions.size() > 0)
        {
            if(routeOptions.size() == 1)
            {
                return routeOptions.get(0);
            }
            else
            {
                // Pick the one that involves the least walking

                routeOptions = orderBusStopPairs(routeOptions, userLocation, destination);
                return routeOptions.get(0);
            }
        }

        return null;
    }

    //Finds the nearest stop on a route from a location
    // TODO - This should return a BusStopDescriptor instead of a BusStop
    public static BusStopDescriptor FindNearestStop(String busRoute, Location loc, int routeIndex) throws JSONException {
        //Intilizations
        String walkingTime = "";
        BusService busService;
        BusStopDescriptor nearestHitReturnDescriptor = null;
        //End of Initilizations

        //CustomLogger.println("FindNearest : User lat = " + loc.getLatitude());
        //CustomLogger.println("FindNearest : User lng = " + loc.getLongitude());


        busService = BusServiceMap.hashMap.get(busRoute.toUpperCase());

        //TODO - DONT NEED TO DO THIS AGAIN FOR SEARCHING FOR THE DESTINATION AS THIS IS ALL POPULATED IN CROWFLIES AS NIALL SAID
        // Populate the distances from the user location to all the stops as the crow flies
        // This will be used to filter down the amount of web queries we need for actual walking distance.
        float distanceCrowFlies;
        // HACK: if its search for destination we dont need to populate this again as the crowfliesnearest is already doing it.
        if (routeIndex == -1)
        {
            for (int n = 0; n < busService.routes.size(); ++n)
            {
                for (int p = 0; p < busService.GetRoute(n).stops.size(); ++p)
                {
                    double latitude = busService.GetRoute(n).stops.get(p).getLat();
                    double longitude = busService.GetRoute(n).stops.get(p).getLng();

                    Location currentStop = new Location("currentStop");
                    currentStop.setLatitude(latitude);
                    currentStop.setLongitude(longitude);
                    distanceCrowFlies = loc.distanceTo(currentStop);

                    busService.GetRoute(n).stops.get(p).setDistanceFromUserLocation(distanceCrowFlies, loc);
                }
            }
        }
        //TODO CHANGE THIS TO 5 TO GET MORE USAGE OUT OF THIS
        //TODO CHANGED TO 1 for 250 requests per day
        // Get the closest 10 stops as the crow flies from the user location
        int numberToFilterTo = 1;
        BusStopDescriptor[] nearestStopsDescriptors = GetNearestBusStops(busService, numberToFilterTo, loc, routeIndex);

        //Now we want to find the actual walking distance from the ten stops.
        //Build the list of the params to send in the request (maximum 25 per request)
        String mode = "&mode=walking";
        String destinations = "&destinations=";
        String comma = ",";
        String pipline = "|";
        String googleParams = loc.getLatitude() + comma + loc.getLongitude() + destinations;
        int distance;
        for (int i = 0; i < nearestStopsDescriptors.length; ++i) {
            double latitude = BusServiceMap.GetStopFromDescriptor(nearestStopsDescriptors[i]).getLat();
            double longitude = BusServiceMap.GetStopFromDescriptor(nearestStopsDescriptors[i]).getLng();

            Location currentStop = new Location("currentStop");
            currentStop.setLatitude(latitude);
            currentStop.setLongitude(longitude);

            googleParams += currentStop.getLatitude() + comma + currentStop.getLongitude() + pipline;
        }

        googleParams += mode;

        //Send the request for the data
        String result2 = "";
        try {
            result2 = HTTPConnector.OpenConnection(new HTTPConnectionData(
                    "https://maps.googleapis.com/maps/api/distancematrix/json?origins=", googleParams));
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        //Parse the data
        for (int n = 0; n < nearestStopsDescriptors.length; ++n) {
            JSONObject json2 = new JSONObject(result2);
            JSONArray rows = json2.getJSONArray("rows");
            JSONObject rec2 = rows.getJSONObject(0);
            JSONArray elements = rec2.getJSONArray("elements");
            JSONObject element = elements.getJSONObject(n); //grab n instead now
            JSONObject durationObject = element.getJSONObject("duration");
            // This is actually the number of seconds to walk between the two locations
            distance = durationObject.getInt("value");
            walkingTime = durationObject.getString("text");

            // Set as walking distance (seconds it would take)
            BusServiceMap.GetStopFromDescriptor(nearestStopsDescriptors[n]).setDistanceFromUserLocation(distance, loc);
            BusServiceMap.GetStopFromDescriptor(nearestStopsDescriptors[n]).setLocationDependentVariables(distance, walkingTime, loc);
        }

        // Distance to user location is now walkable distance.
        // Get the ULTIMATE nearest!!1111!!!11!
        for (int p = 0; p <= nearestStopsDescriptors.length - 1; p++) {
            if (nearestHitReturnDescriptor == null) {
                nearestHitReturnDescriptor = nearestStopsDescriptors[p];
            } else {
                if (BusServiceMap.GetStopFromDescriptor(nearestStopsDescriptors[p]).getDistanceFromUserLocation(loc) <
                        BusServiceMap.GetStopFromDescriptor(nearestHitReturnDescriptor).getDistanceFromUserLocation(loc)) {
                    nearestHitReturnDescriptor = nearestStopsDescriptors[p];
                }
            }

            //CustomLogger.println("Distance from user location to " + BusServiceMap.GetStopFromDescriptor(nearestStopsDescriptors[p]).getName() + " is: "
            //        + BusServiceMap.GetStopFromDescriptor(nearestStopsDescriptors[p]).getDistanceFromUserLocation(loc));
        }

        // for(int i = 0; i < stopsInfo.size()-1; i++){
        //     CustomLogger.println(stopsInfo.get(i).getDistanceFromUserLocation());
        //}

        return nearestHitReturnDescriptor;
    }

    public static void populateCrowDistanceToAllStops(Location location)
    {
        float distance = 0;
        int counter = 0;

        for (int i = 0; i < BusServiceMap.hashMap.size(); ++i) {
            String currentServiceCode = PopulateBusStopsNewAsync.busRoutes[i];
            if (BusServiceMap.hashMap.isEmpty()) {
                break;
            }
            for (int k = 0; k < BusServiceMap.hashMap.get(currentServiceCode).routes.size(); ++k) {

                for (int p = 0; p < BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.size(); ++p) {
                    if (BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.get(p) == null) {
                        break;
                    }
                    Location currentStop = new Location("currentStop");

                    BusStop theBusStop = BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.get(p);

                    currentStop.setLatitude(theBusStop.getLat());
                    currentStop.setLongitude(theBusStop.getLng());

                    //Get as the crow flies distance
                    distance = location.distanceTo(currentStop);

                    //CustomLogger.println(counter + ") Distance from user location to " +
                     //       theBusStop.getName()
                     //       + " is: " + distance);

                    theBusStop.setDistanceFromUserLocation(distance, location);
                }
            }
        }
    }

}