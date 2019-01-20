package com.example.combiningprojects;
//import android.graphics.Color;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import junit.framework.Assert;

import org.json.JSONException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertTrue;

public class PolylineGatherer_AsyncTask extends AsyncTask<PolylineGathererInputData, Void, List<RoutePolylineHolder>>
{
    public interface AsyncResponse
    {
        void PolylineGatherer_ProcessFinish(List<RoutePolylineHolder> output) throws JSONException, IOException, InterruptedException;
    }

    ManipulateURL.FetchUrl fetchUrl = new ManipulateURL.FetchUrl();

    List<RoutePolylineHolder> routesPolyLine = new ArrayList<>();
    public AsyncResponse delegate = null;

    @Override
    protected List<RoutePolylineHolder> doInBackground(PolylineGathererInputData... inputData)
    {
        List<List<Vertex2>> listOfPaths = inputData[0].listOfPaths;

        List<RoutePolylineHolder> allRoutesPolyLinesHolders = new ArrayList<RoutePolylineHolder>();

        if(listOfPaths == null)
        {
            RoutePolylineHolder routePolyLineHolder = null;
            try
            {
                routePolyLineHolder = ExtractRoutePolyLines(inputData[0], 0);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            allRoutesPolyLinesHolders.add(routePolyLineHolder);
        }
        else {
            //for (int x = 0; x < listOfPaths.size(); ++x)
            {
                try
                {
                    RoutePolylineHolder routePolyLineHolder = ExtractRoutePolyLines(inputData[0], inputData[0].pathToProcessIdx);
                    allRoutesPolyLinesHolders.add(routePolyLineHolder);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return allRoutesPolyLinesHolders;
    }

    //StopWatch stopwatch = new StopWatch();

    public RoutePolylineHolder ExtractRoutePolyLines(PolylineGathererInputData inputData, int pathIndex) throws IOException, JSONException, InterruptedException {

        //stopwatch.StartTimeInterval();

        List<List<Vertex2>> listOfPaths = inputData.GetListOfPaths();

        RoutePolylineHolder routePolyLineHolder = new RoutePolylineHolder();

        if(listOfPaths == null)
        {
            // Just get a single route from user location to destiantion
            // Dijkstras failed to get a path, so we just make them walk :D
            routePolyLineHolder.AddPolyLineOptions(requestPolylineOptionsData(inputData.GetUserLocation(), inputData.GetDestination(), "walking", "", 2, GetColorForTravel(TravelMode.walking, 0)), true);
        }
        else
        {
            //grab the path we want to display
            List<Vertex2> path = listOfPaths.get(pathIndex);

            int maxElementsPerRequest = 10;
            int numAdded = 0;
            Location[] temp = new Location[maxElementsPerRequest];
            /*
                // DEBUG TEST CODE!!! Make the route stops smaller
                int resizeCapacity = 10;
                while(routes.get(0).size() > resizeCapacity)
                {
                    // remove from the end
                    routes.get(0).remove(routes.get(0).size() - 1);
                }
            */


            //TODO ADD IN THE USER LOCATION AS THE START AND THE DESTINATION IN DIFFERENT COLOUR POSSIBLY FOR WALKING OR DOTTED
            //BusStopDescriptor originDescriptor = busroutes.get(0).GetOriginStop();
            //int startIndex = originDescriptor.GetStopIndex();
            //int endIndex = busroutes.get(0).GetDestinationStop().GetStopIndex();

            //BusService.Route route = BusServiceMap.hashMap.get(originDescriptor.GetServiceCode()).routes.get(originDescriptor.GetRouteIndex());

            //Add the user location first as the starting point.
            BusStop walkingDestBusStopFromUser = BusServiceMap.GetStopFromDescriptor(path.get(1).getDescriptor());
            Location walkingDestFromUser = new Location("walkingDestFromUser");
            walkingDestFromUser.setLatitude(walkingDestBusStopFromUser.getLat());
            walkingDestFromUser.setLongitude(walkingDestBusStopFromUser.getLng());

            // Add the first walking polyline - User location to the first bus stop
            routePolyLineHolder.AddPolyLineOptions(requestPolylineOptionsData(inputData.GetUserLocation(), walkingDestFromUser, "walking", "", 2, GetColorForTravel(TravelMode.walking, 0)), true);

            BusStopDescriptor prevDescriptor = null;
            BusStopDescriptor nextDescriptor = null;

            //boolean isWalking = true;
            boolean isWalking = false;
            boolean forceEndLine = false;

            int lineColor = Color.BLACK; // We don't expect to see this color.

            int drivingColourSwitcher = 0;

            for (int i = 1; i <= path.size() - 2; ++i) {
                BusStopDescriptor currentDescriptor = path.get(i).getDescriptor();
                nextDescriptor = path.get(i + 1).getDescriptor();

                // Add current location to the temp buffer that will eventually be sent off in a HTTP request to get the polyline
                BusStop currentStop = BusServiceMap.GetStopFromDescriptor(currentDescriptor);
                Location location = new Location("current");
                location.setLatitude(currentStop.getLat());
                location.setLongitude(currentStop.getLng());
                temp[numAdded] = location;
                ++numAdded;

                TravelMode currentTravelMode = GetTravelMode(prevDescriptor, currentDescriptor);
                TravelMode nextTravelMode = GetTravelMode(currentDescriptor, nextDescriptor);

                /*
                System.out.println("Path Element: " + i);

                if(prevDescriptor == null)
                {
                    System.out.println("prevDescriptor is null");
                }
                else
                {
                    System.out.println("prevDescriptor: " + prevDescriptor.toString());
                }

                if(currentDescriptor == null)
                {
                    System.out.println("currentDescriptor is null");
                }
                else
                {
                    System.out.println("currentDescriptor: " + currentDescriptor.toString());
                }

                if(nextDescriptor == null)
                {
                    System.out.println("nextDescriptor is null");
                }
                else
                {
                    System.out.println("nextDescriptor: " + nextDescriptor.toString());
                }

                System.out.println("currentTravelMode: " + currentTravelMode);
                System.out.println("currentTravelMode: " + nextTravelMode);
                System.out.println("");
                */

                if(currentTravelMode != nextTravelMode)
                {
                    if(i > 1) // We don't want to go in here on the first iteration of the loop when it switches from walkign to driving.
                    {
                        // Need to force end the polyline as we are switching mode and want to draw the lines separate colours.
                        forceEndLine = true;

                        if (currentTravelMode == TravelMode.drivingChangedBus || currentTravelMode == TravelMode.walking) {
                            ++drivingColourSwitcher;
                        }
                    }

                    if(currentTravelMode == TravelMode.drivingChangedBus)
                    {
                        // Need to set this to driving so google api gets passed the correct string, if we need to know about drivingChangedBus, the logic needs to go above.
                        currentTravelMode = TravelMode.driving;
                    }

                    //MapsActivity.changingStopsToAddMarkersToo.add(currentStop);
                    MapsActivity.AddMarkerForRoute(pathIndex, currentStop);
                }

                if (    numAdded == maxElementsPerRequest ||    // We've filled the maxiumum number of elements in a single request
                        (i == (path.size() - 2)) ||
                        forceEndLine )                        // or we have reached the end of all the points we need to process
                {
                    // Make the query based off of temp
                    // element 0 is origin and element (numAdded - 1) is destiantion.
                    // elemenets 1 to (numAdded - 2) are the via waypoints

                    LatLng originThisRequest = new LatLng(temp[0].getLatitude(), temp[0].getLongitude());
                    LatLng destThisRequest = new LatLng(temp[numAdded - 1].getLatitude(), temp[numAdded - 1].getLongitude());

                    // Create the waypoint part of the URL
                    String waypointsParam = "&waypoints=";
                    for (int x = 1; x < (numAdded - 1); ++x) {
                        if (x > 1)
                        {
                            // Subsequent waypoints need the | before them. The first one does not.
                            waypointsParam += "|";
                        }

                        waypointsParam += "via:" + temp[x].getLatitude() + "," + temp[x].getLongitude();
                    }

                    //Now request and draw the polyline
                    Location originThisRequestLocation = new Location("origin");
                    originThisRequestLocation.setLatitude(originThisRequest.latitude);
                    originThisRequestLocation.setLongitude(originThisRequest.longitude);

                    Location destThisRequestLocation = new Location("destination");
                    destThisRequestLocation.setLatitude(destThisRequest.latitude);
                    destThisRequestLocation.setLongitude(destThisRequest.longitude);

                    routePolyLineHolder.AddPolyLineOptions( requestPolylineOptionsData( originThisRequestLocation,
                                                                                        destThisRequestLocation,
                                                                                        currentTravelMode.toString(),
                                                                                        waypointsParam,
                                                                                        numAdded,
                                                                                        GetColorForTravel(currentTravelMode, drivingColourSwitcher)
                                                                                        ), currentTravelMode == TravelMode.driving.walking);

                    //TODO only works for the graph one as i didnt add in the user and destination to the other one.
                    if (i < path.size() - 2) // We haven't reached the end
                    {
                        // Get us ready for the next run through
                        // The origin of the next request is the destiantion of the previous
                        temp[0] = temp[numAdded - 1];
                        numAdded = 1;
                    }

                    forceEndLine = false;
                }

                // Get us ready for the next run through
                prevDescriptor = currentDescriptor;
            }

            //Debug code for addding markers to all stops
                /*
                for(int k = 0; k<routes.get(0).size();++k)
                {
                    Location location = new Location("current");
                    location.setLatitude(Double.parseDouble(routes.get(0).get(k).get("lat")));
                    location.setLongitude(Double.parseDouble(routes.get(0).get(k).get("lng")));

                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .title("Bus Stop #" + k)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))).showInfoWindow();
                }
                */

            //Add the destination location last
            //the last bus stop which we should walk from
            BusStop walkingDestFromLastBusStop = BusServiceMap.GetStopFromDescriptor(path.get(path.size() - 2).getDescriptor());
            Location walkingDestFromLastBusStopLocation = new Location("walkingDestFromLastBusStop");
            walkingDestFromLastBusStopLocation.setLatitude(walkingDestFromLastBusStop.getLat());
            walkingDestFromLastBusStopLocation.setLongitude(walkingDestFromLastBusStop.getLng());
    /*
            Location desintationLocation = new Location("ChosenDestination");
            desintationLocation.setLatitude(nearestStopForDestinationOutputData.destLatLng.getLatitude());
            desintationLocation.setLongitude(nearestStopForDestinationOutputData.destLatLng.getLongitude());
            */


            routePolyLineHolder.AddPolyLineOptions(requestPolylineOptionsData(inputData.GetDestination(), walkingDestFromLastBusStopLocation, "walking", "", 2, GetColorForTravel(TravelMode.walking, 0)), true);
        }

        //stopwatch.StopTimeInterval("Time spent in ExtractRoutePolyLines()");

        return routePolyLineHolder;
    }

    int GetColorForTravel(TravelMode travelMode, int whichDrivingColour)
    {
        int lineColor = 0;

        if(travelMode == TravelMode.walking)
        {
            //lineColor = Color.CYAN;
            lineColor = 0xFFed403c; // ARGB
        }
        else if(travelMode == TravelMode.driving)
        {
            if(whichDrivingColour % 2 == 0) {
                //lineColor = Color.RED;
                lineColor = 0xFF006e6f; // ARGB
            }
            else
            {
                //lineColor = Color.GREEN;
                lineColor = 0xFF00acac; // ARGB
            }
        }

        return lineColor;
    }

    enum TravelMode
    {
        unknown,

        // DO NOT CHANGE!
        // These are important, they get converted to strings for use with Google API
        walking,
        driving,

        drivingChangedBus
    }

    TravelMode GetTravelMode(BusStopDescriptor startDesciptor, BusStopDescriptor endDescriptor)
    {
        if( startDesciptor == null && // User/Dest location
            endDescriptor != null)
        {
            return TravelMode.walking;
        }
        else if(startDesciptor != null &&
                endDescriptor != null)
        {
            // We're on a bus (probably), let's make sure we're not walking between bus services
            if(startDesciptor.GetServiceCode().equals(endDescriptor.GetServiceCode()))
            {
                return TravelMode.driving;
            }
            else
            {
                if(startDesciptor.globalStopID != endDescriptor.globalStopID)
                {
                    // Walk as they're different stops
                    return TravelMode.walking;
                }
                else
                {
                    return TravelMode.drivingChangedBus;
                }
            }
        }
        else if(	startDesciptor != null &&
                endDescriptor == null) // Destination location
        {
            return TravelMode.walking;
        }

        assertTrue(false);

        return TravelMode.unknown;
    }

    public PolylineOptions requestPolylineOptionsData(Location startPoint, Location destination , String travelMethod, String waypointsParam, int counter, int color) throws IOException, JSONException, InterruptedException {
        ManipulateURL.FetchUrl FetchUrl = new ManipulateURL.FetchUrl();
        String url = ManipulateURL.getDirectionsUrl(new LatLng(startPoint.getLatitude(),startPoint.getLongitude()), new LatLng(destination.getLatitude(),destination.getLongitude()), travelMethod,waypointsParam);
        return FetchUrl.DoBlocking(url,counter,color);
    }

    public PolylineOptions requestPolylineOptionsData(LatLng startLatLng, LatLng destinationLatLng, String travelMethod, String waypointsParam, int color) throws IOException, JSONException, InterruptedException {
        Location originThisRequestLocation = new Location("origin");
        originThisRequestLocation.setLatitude(startLatLng.latitude);
        originThisRequestLocation.setLongitude(startLatLng.longitude);

        Location destThisRequestLocation = new Location("destination");
        destThisRequestLocation.setLatitude(destinationLatLng.latitude);
        destThisRequestLocation.setLongitude(destinationLatLng.longitude);

        return requestPolylineOptionsData(originThisRequestLocation, destThisRequestLocation, travelMethod, waypointsParam, 2, color);
    }

    @Override
    protected void onPostExecute(List<RoutePolylineHolder> result)
    {
        super.onPostExecute(result);

        try {
            delegate.PolylineGatherer_ProcessFinish(result);
        }
        catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}