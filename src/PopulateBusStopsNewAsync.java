package com.example.combiningprojects;

import android.location.Location;
import android.os.AsyncTask;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

//Class to populate the bus stops on boot
//TODO STORE THIS IS PREFERENCES, POSSIBLY ADD IT INTO THE DOWNLOAD.
public class PopulateBusStopsNewAsync extends AsyncTask<Void, Void, String>
{
    public interface AsyncResponse
    {
        void PopulateBusStopsNew_ProcessFinish() throws JSONException;
    }

    // public static ArrayList<ArrayList<BusStop>> stops;
    public AsyncResponse delegate = null;
    public static String[] busRoutes = new String[]{"201", "202", "203", "205", "206", "207", "207A", "208", "209","209A",
            "214", "215", "215A", "216", "219", "220", "220X", "221", "223", "226", "226A" };
    List<List<Integer>> variantIds = new ArrayList<>();

    //"240"

    @Override
    protected String doInBackground(Void... params) {
        //dubLinkedBusData(busRoutes,count);

        //Grab the variant ids for this service route
        for(int p = 0; p < busRoutes.length; ++p) {
            // Create the service
            BusService currentService = new BusService(busRoutes[p]);

            JSONObject variantsJsonParams = new JSONObject();
            try {
                variantsJsonParams.put("searchString", busRoutes[p]);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONArray variantIdArray;

            try {
                variantIdArray = HTTPConnector.OpenConnectionParseRTPI(new HTTPConnectionData("http://www.rtpi.ie/connectservice.svc/GetPublicServicesForCriteria", variantsJsonParams));
                //loop and add all the variant ids to the list
                for (int j = 0; j < variantIdArray.length(); ++j) {

                    JSONObject rec = variantIdArray.getJSONObject(j);
                    JSONArray serviceVariantIds = rec.getJSONArray("ServiceVariantIds");
                    String publicServiceCode = rec.getString("PublicServiceCode");
                    //Filtering out the 220x incase of 220 etc.. The 220x will be dealt with seperately
                    if(serviceVariantIds.length() > 0 && publicServiceCode.equalsIgnoreCase(currentService.GetServiceID()))
                    {
                        variantIds.add(j, new ArrayList<Integer>());
                        for (int k = 0; k < serviceVariantIds.length(); ++k) {
                            if (rec.getInt("OperatorId") == 2) {
                                variantIds.get(j).add(k, serviceVariantIds.getInt(k));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Do one request for each variant id grabbed and thats a different route
            for (int l = 0; l < variantIds.size(); ++l) {
                List<JSONArray> stopNames = new ArrayList<>();
                for (int k = 0; k < variantIds.get(l).size(); ++k) {
                    JSONArray singleVariant = new JSONArray();
                    singleVariant.put(variantIds.get(l).get(k));

                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("publicServiceCode", busRoutes[p]);
                        jsonObject.put("operatorID", 2);
                        jsonObject.put("depotID", 1);
                        jsonObject.put("meshStops", 1);
                        jsonObject.put("serviceVariantIDs", singleVariant);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        stopNames.add(HTTPConnector.OpenConnectionParseRTPI(new HTTPConnectionData("http://www.rtpi.ie/connectservice.svc/GetStopsForService", jsonObject)));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (stopNames.size() > 0)
                {
                    int longestStopNamesIdx = CalculateLongestStopNames(stopNames);
                    try {
                        if (longestStopNamesIdx >= 0) {
                            addRouteFromStopNames(stopNames.get(longestStopNamesIdx), currentService);
                        } else {
                            System.out.println("Ignoring as variant was empty");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
                BusServiceMap.hashMap.put(currentService.GetServiceID(), currentService);
                variantIds.clear();
            }

        return "bleh";
    }

    public static void PostProcessData()
    {
        for (int i = 0; i < BusServiceMap.hashMap.size(); ++i)
        {
            String currentServiceCode = PopulateBusStopsNewAsync.busRoutes[i];
            //loop over the routes
            int numRoutes = BusServiceMap.hashMap.get(currentServiceCode).routes.size();
            for (int k = 0; k < numRoutes; ++k)
            {
                //loop over all the stops
                BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).CreateStopsBasicArray();
            }
        }
    }

    @Override
    protected void onPostExecute(String test)
    {
        //fix for below as made another asyncresponse
        super.onPostExecute(test);

        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);

        //System.out.println("onPostExecute: " + seconds);

        try {
            delegate.PopulateBusStopsNew_ProcessFinish();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static int CalculateLongestStopNames(List<JSONArray> stopNames)
    {
        int longestStopNamesIdx = -1;
        int longestStopNamesFound = -1;
        for(int i = 0; i < stopNames.size(); ++i)
        {
            if(stopNames.get(i).length() > longestStopNamesFound)
            {
                longestStopNamesFound = stopNames.get(i).length();
                longestStopNamesIdx = i;
            }
        }
        return longestStopNamesIdx;
    }

    static void addRouteFromStopNames(JSONArray stopNames, BusService currentService) throws JSONException
    {
        if (stopNames.length() > 0) {
            // Add a new route, we populate it below
            currentService.AddRoute();
            // The new route is last in the array (we just added it), so the index will be count - 1
            int routeIndex = currentService.GetRouteCount() - 1;


            //CustomLogger.println("Recieved stopnames for publicservicecode " + busRoutes[p]);
            //CustomLogger.println("******BUS ROUTE" + busRoutes[p] + "*******");

            //loop and add all the stops to a list
            for (int j = 0; j < stopNames.length(); ++j) {
                //need to not add all the name to stop2[0] need to change when the service number changes..
                Location currentStop = new Location("currentStop");

                JSONObject rec = stopNames.getJSONObject(j);
                double latitude = rec.getDouble("Latitude");
                double longitude = rec.getDouble("Longitude");
                String stopName = rec.getString("StopNameLong");
                int stopSequenceIndex = rec.getInt("StopSequenceIndex");
                int globalStopID = rec.getInt("PublicAccessCode");

                currentService.AddStopToRoute(routeIndex, new BusStop(currentService.GetServiceID(), stopName, latitude, longitude, stopSequenceIndex, globalStopID));
            }
        }
    }

    public static void dubLinkedBusData(String[] busRoutes, int count)
    {
        // stops = new ArrayList<ArrayList<BusStop>>();
        // A)
        //loop over and get request all the stops for every service code in busRoutes
        for (int i = 0; i < busRoutes.length; ++i)
        {
            String serviceCode = busRoutes[i];

            // Create the service
            BusService currentService = new BusService(busRoutes[i]);

            try {
                //Grab the data for each service code on a loop
                String data = ManipulateURL.downloadUrl("https://data.dublinked.ie/cgi-bin/rtpi/routeinformation?routeid="+serviceCode+"&operator=BE&format=json");
                JSONObject resultDuBlinked = new JSONObject(data);
                //grab the results array from the data for each route
                JSONArray jsonArray = resultDuBlinked.getJSONArray("results");
                // B)
                // Loop over the list of routes for a particular bus service
                for (int k = 0; k < jsonArray.length(); k++) {
                    //Change the route data
                    JSONObject stopsInfo = jsonArray.getJSONObject(k);
                    //grab the stops array from the filtered data
                    JSONArray stopsArray = stopsInfo.getJSONArray("stops");

                    // Add a new route, we populate it below
                    currentService.AddRoute();

                    // The new route is last in the array (we just added it), so the index will be count - 1
                    int routeIndex = currentService.GetRouteCount() - 1;

                    // C)
                    for(int p = 0; p < stopsArray.length(); p++)
                    {
                        //grab stop p from the array
                        JSONObject jsonObject = stopsArray.getJSONObject(p);
                        //parse the name,lat and lng of each bus stop
                        String stopName = jsonObject.getString("fullname");
                        double latitude = jsonObject.getDouble("latitude");
                        double longitude = jsonObject.getDouble("longitude");
                        // Add the stop to the route
                        currentService.AddStopToRoute(routeIndex, new BusStop(busRoutes[i], stopName, latitude, longitude, 0, 0));
                        //stops.get(i).add(count, new BusStop(busRoutes[i], stopName, latitude, longitude));
                        //increment the count to assign the next bus stop to the next slot
                        count++;
                    }
                    BusServiceMap.hashMap.put(currentService.GetServiceID(), currentService);
                }
                count = 0;
                //System.out.println("count =" +count);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
/*
            // A)
            // Loop over the RTPI data and populate our custom data types
            for(int z = 0; z < servicesAvailable.Count; ++z) // Lopp over all the services jsonArray.length()
            {
                // Create the service
                BusService currentService = new BusService(servicesAvailable[z].serviceID);
                // B)
                for(int x = 0; x < servicesAvailable[z].routesOnService.Count; ++x) // Loop over each route on the service
                {
                    // Add a new route, we populate it below
                    currentService.AddRoute();

                    // The new route is last in the array (we just added it), so the index will be count - 1
                    int routeIndex = currentService.GetRouteCount() - 1;
                    // C)
                    for(int y = 0; y < stopsOnRoute.Count; ++y) // Loop over each stop on the route
                    {
                        // Add the stop to the route
                        currentService.AddStopToRoute(routeIndex, stopsOnRoute[y]);
                    }
                }

                BusServiceMap.hashMap.put(currentService.GetServiceID(), currentService);
            }
*/
    }

    public static void RemoveDuplicateRoutes()
    {
        //loop over the whole hashmap
        for(int i = 0; i < BusServiceMap.hashMap.size(); ++i)
        {
            //Grab the service code to start with
            String currentServiceCode = PopulateBusStopsNewAsync.busRoutes[i];
            BusService currentService = BusServiceMap.hashMap.get(currentServiceCode);

            //If there is more than 2 routes then we know there is some duplicates
            if(currentService.routes.size() > 2)
            {
                //overall count of routes within the service
                int initialRouteCount = currentService.GetRouteCount();
                //grab the current route to check against
               // BusService.Route currentRoute = currentService.GetRoute(routeCount);

                for(int k = initialRouteCount-1; k >= 0; --k)
                {
                    BusService.Route potentialRouteToDelete = currentService.GetRoute(k);
                    for(int p = 0; p < currentService.GetRouteCount(); ++p )
                    {
                        //dont check against itself
                        if(k != p)
                        {
                            //now loop over all the other routes and check
                            BusService.Route routeToCheckAgainst = currentService.GetRoute(p);

                            boolean isSubset = routeIsSubset(potentialRouteToDelete,routeToCheckAgainst);
                            if(isSubset)
                            {
                                currentService.routes.remove(potentialRouteToDelete);
                                //routeCount = currentService.GetRouteCount()-1;
                            }
                        }
                    }
                }
            }
        }
    }

    public static Boolean routeIsSubset(BusService.Route potentialSubSet, BusService.Route referenceStops)
    {
        Boolean isSubset = false;
        BusStop stopToFind = potentialSubSet.stops.get(0);
        int referenceStartIdx = -1;

        for(int x = 0; x < referenceStops.stops.size(); ++x)
        {
            if(referenceStartIdx >= 0)
            {
                // Have found the start of the potential subset, checking the rest of the elements
                int potentialSubsetIdx = x - referenceStartIdx;

                if( referenceStops.stops.get(x).getGlobalStopID() ==
                    potentialSubSet.stops.get(potentialSubsetIdx).getGlobalStopID())
                {
                    if(potentialSubsetIdx == (potentialSubSet.stops.size() -1))
                    {
                        // Reached the end successfully
                        isSubset = true;
                        break;
                    }
                }
                else
                {
                    //Found a mismatch
                    isSubset = false;
                    break;
                }
            }
            else
            {
                // Have yet to find the start of the potential subset
                if( referenceStops.stops.get(x).getGlobalStopID() ==
                        stopToFind.getGlobalStopID())
                {
                    //Found the start of the potential subset
                    referenceStartIdx = x;
                }
            }
        }
        return isSubset;
    }
}