package com.example.combiningprojects;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.PatternItem;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        FindNearestStopForSpecificRoute_CurrentLocation.AsyncResponse,
        FindNearestStopAnyRoute_ForDestination.AsyncResponse,
        FindNameOfPlaceLatLng.AsyncResponse,
        //GoogleMap.OnMarkerClickListener,
        PolylineGatherer_AsyncTask.AsyncResponse
{
    public enum MapMode
    {
        FindNearestStopForSpecificRoute_CurrentLocation,
        FindNearestStopAnyRoute_ForDestination,
    }

    public static GoogleMap mMap = null;
    BusStop nearestHitForSpecificRoute = null;

    public Polyline polylineForMap;
    List<Polyline> polyLinesList;
    PolylineOptions lineOptions = null;

    ArrayList<LatLng> MarkerPoints;
    public static boolean isMarker = false;

    protected ManipulateURL manipulateURL;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;

    double userLat;
    double userLng;
    LatLng userLatLng;

    String destination;
    String busRoute;
    String userLocationName;

    MapMode mapMode;

    Location userLocation;

    List<List<HashMap<String, String>>> routes = new ArrayList<>() ;

    boolean backFromMap;
    boolean isAlernativeRoutes;

    ArrayList<ArrayList<Marker>> markerList;
    boolean alternativeMode;
    View alternativeRoutesButton;
    boolean allowMultipleBuses;
    boolean useDijkstras;

    Toast toast;

    private ProgressBar spinner;

    public List<List<Vertex2>> listOfPaths;

    static List<List<BusStop>> changingStopsToAddMarkersToo; // One list of markers for each route we plot

    FindNearestStopForSpecificRoute_CurrentLocation findNearestStopForSpecificRoute_CurrentLocation = null;
    FindNearestStopAnyRoute_ForDestination findNearestStopAnyRoute_ForDestination = null;
    FindNameOfPlaceLatLng.FindNameOfPlaceLatLngAsync findNameOfPlaceLatLng = null;
    NearestStopForDestinationOutputData nearestStopForDestinationOutputData = null;
    List<RoutePolylineHolder> polyLineGatherer_OutputData = new ArrayList<RoutePolylineHolder>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //DISABLE ALL USER INPUT
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        //Inits
        MarkerPoints = new ArrayList<>();
        //manipulateURL = new ManipulateURL();
        lineOptions = new PolylineOptions();
        backFromMap = false;
        markerList = new ArrayList<>();
        markerList.add( new ArrayList<Marker>() );
        markerList.add( new ArrayList<Marker>() );
        markerList.add( new ArrayList<Marker>() );
        alternativeMode = false;
        isAlernativeRoutes = false;
        alternativeRoutesButton = findViewById(R.id.alternativeRoutesButton);
        toast = null;
        spinner = (ProgressBar) findViewById(R.id.progressBar2);
        //end of inits

        //Start the spinner as we need to load
        spinner.setVisibility(View.VISIBLE);

        //Get the intent passed in and grab the data
        Intent mapsStart = getIntent();
        userLat = mapsStart.getDoubleExtra("userLat", Double.MAX_VALUE);
        userLng = mapsStart.getDoubleExtra("userLng", Double.MAX_VALUE);
        destination = mapsStart.getStringExtra("destination");
        allowMultipleBuses = mapsStart.getBooleanExtra("allowMultipleBuses",false);
        useDijkstras = mapsStart.getBooleanExtra("useDijkstras",false);
        busRoute = mapsStart.getStringExtra("busRoute");
        mapMode = (MapMode)mapsStart.getSerializableExtra("mapMode");
        polyLinesList = new ArrayList<>();
        changingStopsToAddMarkersToo = new ArrayList<>();
        changingStopsToAddMarkersToo.add(new ArrayList<BusStop>());
        changingStopsToAddMarkersToo.add(new ArrayList<BusStop>());
        changingStopsToAddMarkersToo.add(new ArrayList<BusStop>());
        //end of grabbing info from intent

        //Set the users location
        userLocation = new Location("userLocation");
        userLocation.setLatitude(userLat);
        userLocation.setLongitude(userLng);

        userLatLng = new LatLng(userLocation.getLatitude(),userLocation.getLongitude());
        //end of grabbing users location

        //grab user location name
        findNameOfPlaceLatLng = new FindNameOfPlaceLatLng.FindNameOfPlaceLatLngAsync();
        findNameOfPlaceLatLng.delegate = this;
        findNameOfPlaceLatLng.execute(userLocation);

        //Debug for checking user location
        //CustomLogger.println("MapActivity : User lat = " + userLat);
        //CustomLogger.println("MapActivity : User lng = " + userLng);
        //end of debug

        //Assertion error check for making sure the lat and lng are init
        if(userLat == Double.MAX_VALUE)
        {
            throw new AssertionError("Latitude has not been inited correctly");
        }
        if(userLng == Double.MAX_VALUE)
        {
            throw new AssertionError("Longitude has not been inited correctly");
        }
        //end of assertion error check

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    StopWatch masterTimer = new StopWatch();
    StopWatch secondaryTimer = new StopWatch();

    /**
     * Manipulates the map once available.
     * This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //set the camera straight away
        mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(8));

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        masterTimer.StartTimeInterval();

        //Decide how the map is going to populated depending on what was passed through
        if(mapMode == MapMode.FindNearestStopForSpecificRoute_CurrentLocation) {
            alternativeRoutesButton.setVisibility(View.GONE);
            findNearestStopForSpecificRoute_CurrentLocation = new FindNearestStopForSpecificRoute_CurrentLocation();
            findNearestStopForSpecificRoute_CurrentLocation.delegate = this;
            findNearestStopForSpecificRoute_CurrentLocation.execute(new NearestStopForRouteData(userLat,userLng,busRoute));
        }
        else if(mapMode == MapMode.FindNearestStopAnyRoute_ForDestination) {
            secondaryTimer.StartTimeInterval();
            findNearestStopAnyRoute_ForDestination = new FindNearestStopAnyRoute_ForDestination();
            findNearestStopAnyRoute_ForDestination.delegate = this;

            System.out.println("Called findNearestStopAnyRoute_ForDestinatio::execute(): " + (System.nanoTime() / 1000000000.0));

            findNearestStopAnyRoute_ForDestination.execute(new NearestStopForDestinationInputData(userLocation, destination, allowMultipleBuses, useDijkstras));
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    //Set control for what happens when back is pressed.
    @Override
    public void onBackPressed() {
        //cancel the toast if there is one
        if(toast != null)
        {
            toast.cancel();
        }
        //Set the info so the main activity knows its coming back from the maps activity
        backFromMap = true;
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("backFromMap", backFromMap);
        startActivity(intent);
    }

    //FindNearestStopAnyRoute_ForDestination class return
    @Override
    public void FindNearestStopAnyRoute_ForDestination_ProcessFinish(NearestStopForDestinationOutputData output) throws JSONException
    {
        secondaryTimer.StopTimeInterval("*** Time spent in FindNearestStopAnyRoute_ForDestination");

        //Get the list of routes and store the rest of the output
        nearestStopForDestinationOutputData = output;

        listOfPaths = nearestStopForDestinationOutputData.listOfPaths;

        /*
        // Debug fill the listOfPaths with test data
        listOfPaths.clear();
        listOfPaths.add(0, new LinkedList<Vertex>());

        listOfPaths.get(0).clear();

        // Add user location
        listOfPaths.get(0).add(new Vertex(0, "UserLoc", null));

        BusService.Route the201Route = BusServiceMap.hashMap.get("201").GetRoute(0);
        for(int x = 0; x < the201Route.stops.size() / 3; ++x)
        {
            BusStopDescriptor descriptor = new BusStopDescriptor("201", 0, x, the201Route.stops.get(x).getGlobalStopID());

            listOfPaths.get(0).add(new Vertex(listOfPaths.get(0).size(), "TestVertex" + listOfPaths.get(0).size(), descriptor));
        }

        BusService.Route the203Route = BusServiceMap.hashMap.get("203").GetRoute(0);
        for(int x = 0; x < the203Route.stops.size(); ++x)
        {
            BusStopDescriptor descriptor = new BusStopDescriptor("203", 0, x, the203Route.stops.get(x).getGlobalStopID());

            listOfPaths.get(0).add(new Vertex(listOfPaths.get(0).size(), "TestVertex" + listOfPaths.get(0).size(), descriptor));
        }

        // Add destination location
        listOfPaths.get(0).add(new Vertex(listOfPaths.get(0).size(), "DestLoc", null));
        */

        //Set toast for saying which route to take
        //createToast("Bus route "+ listOfPaths.get(0).get(1).getDescriptor().GetServiceCode() + " is the optimal route to your destination");

        //Init the map drawing
        InitMapDrawing();
    }

    //FindNearestStopForSpecificRoute_CurrentLocation class return
    @Override
    public void FindNearestStopForSpecificRoute_CurrentLocation_ProcessFinish(BusStop output) throws JSONException {
        //Store the nearest bus stop to a specific route
        nearestHitForSpecificRoute = output;
        //Init the map drawing
        InitMapDrawing();
    }

    //FindNameOfPlaceLatLng class return
    @Override
    public void FindNameOfPlaceLatLng_ProcessFinish(String output) throws JSONException {
        //Store the name of the users location
        userLocationName = output;
        //CustomLogger.println("User Location Name is: "+userLocationName);
    }

    @Override
    public void PolylineGatherer_ProcessFinish(List<RoutePolylineHolder> output) throws JSONException, IOException, InterruptedException
    {
        secondaryTimer.StopTimeInterval("*** Time spent PolylineGatherer");

        // We are now ready to start drawing polylines
        //polyLineGatherer_OutputData = output;
        polyLineGatherer_OutputData.add(output.get(0));

        /*
        // Always draw the optimal route
        DrawRoute(polyLineGatherer_OutputData.get(currentRouteIndex));

        CreateMarkers(false);
        SwitchToMarkerSet(currentRouteIndex);
        */

        CreateMarkers(false, currentRouteIndex);
        SwitchToRoute(currentRouteIndex);

        //finally hide the loading spinner
        spinner.setVisibility(View.GONE);
        //Give user control
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        masterTimer.StopTimeInterval("****** Total Time");
    }

    void CreateMarkers(boolean createVisible, int routeIndex)
    {
        // Creates ALL the markers from changingStopsToAddMarkersToo and sets their visibility
        //Add the stops that you need to change buses for on multi mode
        //for(int x = 0; x < changingStopsToAddMarkersToo.size(); ++x)
        {
            List<BusStop> currentListMarkers = changingStopsToAddMarkersToo.get(routeIndex);
            if (currentListMarkers.size() > 0)
            {
                for (int i = 0; i < currentListMarkers.size(); ++i)
                {
                    BusStop currentStop = currentListMarkers.get(i);

                    String message = "*UNDEFINED*";

                    boolean skipNext = false;

                    if(i < (currentListMarkers.size() - 1))
                    {
                        BusStop nextStop = currentListMarkers.get(i + 1);

                        if(currentStop.getPublicServiceCode().equals(nextStop.getPublicServiceCode()))
                        {
                            message = "Board the " + currentListMarkers.get(i).getPublicServiceCode() + " here";
                        }
                        else
                        {
                            if(currentStop.getGlobalStopID() == nextStop.getGlobalStopID())
                            {
                                // We have got off at a stop and just waiting there for a different bus service, no walking.
                                message = "Depart the " + currentStop.getPublicServiceCode() + " and board the " + nextStop.getPublicServiceCode() + " here";

								// 2 markers get added at the same location, we want to combine the messaging into one and skip the 2nd marker
                                skipNext = true;
                            }
                            else
                            {
                                message = "Depart the " + currentStop.getPublicServiceCode() + " here";
                            }
                        }
                    }
                    else
                    {
                        // Reached last bus stop, walking from here to the final destination
                        message = "Depart the " + currentStop.getPublicServiceCode() + " and walk to " + destination;
                    }

                    Marker newMarker = mMap.addMarker(  new MarkerOptions()
                                                .position(new LatLng(currentListMarkers.get(i).getLat(), currentListMarkers.get(i).getLng()))
                                                .title(currentListMarkers.get(i).getName())
                                                .visible(true)
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                                                newMarker.setSnippet(message);

                    newMarker.setVisible(createVisible);

                    // TODO - Store these markers in a list per route so you cna turn them on and off when necessary
                    markerList.get(routeIndex).add(newMarker);

                    if(skipNext)
                    {
                        ++i;
                    }
                }
            }
        }
    }

    void DrawRoute(RoutePolylineHolder routePolylineHolder)
    {
        for(int x = 0; x < routePolylineHolder.GetPolyLines().size(); ++x)
        {
            DrawPolyline(routePolylineHolder.GetPolyLines().get(x));
        }
    }

    void DrawPolyline(RoutePolylineHolder.PolyLineWithMetaData polyLineWithMetaData)
    {
        // Drawing polyline in the Google Map for the i-th route
        if (polyLineWithMetaData.polyLineOptions != null)
        {
            if (!MapsActivity.isMarker)
            {
                //polylineForMap = MapsActivity.mMap.addPolyline(polyLineWithMetaData.polyLineOptions);
                //add all walking and drivings lane for this particular route
                polyLinesList.add(MapsActivity.mMap.addPolyline(polyLineWithMetaData.polyLineOptions));

                List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(20));

                if(polyLineWithMetaData.isWalking)
                {
                    polyLinesList.get(polyLinesList.size() - 1).setPattern(pattern);
                }
            }
        }
    }

    int currentRouteIndex = 0;


    //onClick for the alternative routes button
    public void showAlternativeRoutes(View view)
    {
        // Keep within range and loop around
        if(listOfPaths.size() > 1)
        {
            // Move to the next one
            ++currentRouteIndex;
            currentRouteIndex %= listOfPaths.size();

            if(polyLineGatherer_OutputData.size() > currentRouteIndex)
            {
                // We have the data ready to display

                SwitchToRoute(currentRouteIndex);
            }
            else
            {
                // Kick off async task to gather the polyline data
                // Turn spinner on
                // Block user input
                // When ProcessFinish gets called, it needs to swithc to the new polyline
                spinner.setVisibility(View.VISIBLE);

                //DISABLE ALL USER INPUT
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                KickOffLineGatherer(currentRouteIndex);
            }
        }
    }

    void SwitchToRoute(int routeIndex)
    {
        //remove all other polylines
        for( int i = 0; i < polyLinesList.size(); ++i )
        {
            polyLinesList.get(i).remove();
        }
        // and clear the list
        polyLinesList.clear();

        DrawRoute(polyLineGatherer_OutputData.get(routeIndex));

        SwitchToMarkerSet(routeIndex);

        if(routeIndex == 0)
        {
            //createToast("Displaying Optimal Route");
            if(mapMode == MapMode.FindNearestStopAnyRoute_ForDestination)
            {
                //createToast("Bus route "+ listOfPaths.get(0).get(1).getDescriptor().GetServiceCode() + " is the optimal route to your destination");
                createToast("Optimal Route: " + listOfPaths.get(0).get(1).getDescriptor().GetServiceCode());
                //"Optimal Route: 220 -> 201 -> 203";
                //"Optimal Route: Walk towards Mt Argyll Station and board the 220"
            }

        }
        else
        {
            createToast("Displaying Alternative Route " + routeIndex + ": " + listOfPaths.get(routeIndex).get(1).getDescriptor().GetServiceCode());
            //"Alternative Route: 220 -> 201 -> 203";
            //"Alternative Route: Walk towards Mt Argyll Station and board the 220"
        }
    }

    void SwitchToMarkerSet(int markerSetIdx)
    {
        // Set visible for markerSetIdx

        //Set invisible for all others

        boolean visible = false;
        for(int x = 0; x < markerList.size(); ++x)
        {
            if (x == markerSetIdx)
            {
                visible = true;
            }
            else
            {
                visible = false;
            }

            List<Marker> currentMarkerSet = markerList.get(x);
            for(int markerIdx = 0; markerIdx < currentMarkerSet.size(); ++markerIdx)
            {
                currentMarkerSet.get(markerIdx).setVisible(visible);
            }
        }
    }

    public void createToast(String toastText)
    {
        //If there is a toast displaying, cancel it
        if(toast != null)
        {
            toast.cancel();
        }
        //Make the new toast and show it
        toast = Toast.makeText(this,toastText,Toast.LENGTH_LONG);
        toast.show();
    }

    //Method for adding a marker with certain variables attached
    public Marker addMarker(LatLng position, String title, Float bitmap,String snippet, boolean isVisible)
    {
        Marker marker;
        //Add the marker with seperate colours and other settings
        marker = mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(title)
                .visible(isVisible)
                .icon(BitmapDescriptorFactory.defaultMarker(bitmap)));
                marker.setSnippet(snippet);

        return marker;
    }

    public List<BusStop> getOriginDestBusStops(int pathIndex)
    {
        List<BusStop> returnList = new ArrayList<>();

        BusStopDescriptor currentStopDescriptor = listOfPaths.get(pathIndex).get(1).getDescriptor();
        BusStop originStop = BusServiceMap.GetStopFromDescriptor(currentStopDescriptor);

        returnList.add(0,originStop);

        currentStopDescriptor = listOfPaths.get(pathIndex).get(listOfPaths.get(pathIndex).size()-2).getDescriptor();
        BusStop destinationStop = BusServiceMap.GetStopFromDescriptor(currentStopDescriptor);

        returnList.add(0,destinationStop);

        return returnList;
    }

    //Initialise the map drawing with markers and other content.
    void InitMapDrawing()
    {
        if(mapMode == MapMode.FindNearestStopForSpecificRoute_CurrentLocation)
        {
            //Set the destination as a latlng
            LatLng nearestStopToUserForSpecificRoute = new LatLng(nearestHitForSpecificRoute.getLat(),
                    nearestHitForSpecificRoute.getLng());


            //ADD THE MARKER FOR THE USERS LOCATION
            addMarker(  userLatLng,userLocationName,
                        BitmapDescriptorFactory.HUE_ROSE,
                        "YOUR LOCATION",true);

            //TODO - BUG HERE WITH GRABBING THE WALKINGTIMEMINUTES, ITS OUTPUTTING MULTIPLE THINGS IN THE HASHMAP NOT SURE WHY
            //ADD THE MARKER TO THE NEAREST STOP TO THE USERS LOCATION
            addMarker(  nearestStopToUserForSpecificRoute,nearestHitForSpecificRoute.getName(),
                        BitmapDescriptorFactory.HUE_YELLOW,
                        "Walking time to this stop : " + nearestHitForSpecificRoute.getWalkingTimeMinutes(FindNearestStopForSpecificRoute_CurrentLocation.loc) ,true);

            //nearestHitForSpecificRoute.locationBasedDataHashMap.get(userLocation).walkingTimeMinutes

            Location nearestStopToUserForSpecificRoute_Location = new Location("nearestStopToUserForSpecificRoute");
            nearestStopToUserForSpecificRoute_Location.setLatitude(nearestStopToUserForSpecificRoute.latitude);
            nearestStopToUserForSpecificRoute_Location.setLongitude(nearestStopToUserForSpecificRoute.longitude);

            // Grab the first route data
            PolylineGathererInputData inputData = new PolylineGathererInputData(userLocation, nearestStopToUserForSpecificRoute_Location, null, 0);
            PolylineGatherer_AsyncTask lineGathererTask = new PolylineGatherer_AsyncTask();
            lineGathererTask.delegate = this;
            lineGathererTask.execute(inputData);

            //move map camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
            //finally hide the loading spinner
            spinner.setVisibility(View.GONE);
            createToast("Nearest stop: " + nearestHitForSpecificRoute.getName());
            //Give user control
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
        else if(mapMode == MapMode.FindNearestStopAnyRoute_ForDestination)
        {

            ////////////////////////////////////////////////////////////////////
            //                                                                //
            //          These markers will always appear for step 2 & 3       //
            //                                                                //
            ///////////////////////////////////////////////////////////////////
            //MARKER FOR USERS CURRENT LOCATION
            //addMarker(userLatLng,userLocationName,BitmapDescriptorFactory.HUE_ROSE,"YOUR LOCATION",true);

            //MARKER FOR THE DESTINATION
            addMarker(  new LatLng( nearestStopForDestinationOutputData.GetDestLatLng().getLatitude(),
                                    nearestStopForDestinationOutputData.GetDestLatLng().getLongitude()),
                                    destination.toUpperCase(),BitmapDescriptorFactory.HUE_VIOLET,null,true);
            /*
            //ADD THE MARKER TO THE NEAREST STOP TO BOARD
            List<BusStop> returnList = new ArrayList<>();
            returnList = getOriginDestBusStops(0);
            optimalDestinationMarker = addMarker(new LatLng( returnList.get(0).getLat(),
                                                        returnList.get(0).getLng()),
                                                        returnList.get(0).getName(),BitmapDescriptorFactory.HUE_YELLOW,
                                                        "Stop to depart service",true);

            //MARKER FOR THE NEAREST STOP TO GET OFF AT NEAR THE DESTINATION
            optimalOriginMarker = addMarker(new LatLng(    returnList.get(1).getLat(),
                                                                returnList.get(1).getLng()),
                                                                returnList.get(1).getName().toUpperCase(),BitmapDescriptorFactory.HUE_YELLOW,
                                                                "Walking time to this stop :" + returnList.get(1).getWalkingTimeMinutes(FindNearestStopAnyRoute_ForDestination.userLoc),true);
            */
            // +", " + busroutes.get(1).GetOriginStop().GetServiceCode() + ", " + busroutes.get(2).GetOriginStop().GetServiceCode() -- FOR OTHER ROUTES TO PRINTED BUT IT HAD A BUG WHERE THERE SOMETIMES WAS NOT 3

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////CODE FOR DISPLAYING THE ROUTE POLYLINE////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            secondaryTimer.StartTimeInterval();
            // Grab the first route data
            KickOffLineGatherer(0);

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////END OF CODE FOR DISPLAYING THE ROUTE POLYLINE/////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            //finally hide the loading spinner
            //spinner.setVisibility(View.GONE);

        }
    }

    void KickOffLineGatherer(int routeIndex)
    {
        //LatLng origin = new LatLng(Double.parseDouble(routes.get(0).get(0).get("lat")),Double.parseDouble(routes.get(0).get(0).get("lng")));
        listOfPaths = nearestStopForDestinationOutputData.listOfPaths;
        //printPath(listOfPaths, routeIndex);

        PolylineGathererInputData inputData = new PolylineGathererInputData(userLocation, nearestStopForDestinationOutputData.GetDestLatLng(), listOfPaths, routeIndex);
        PolylineGatherer_AsyncTask lineGathererTask = new PolylineGatherer_AsyncTask();
        lineGathererTask.delegate = this;
        lineGathererTask.execute(inputData);
    }

    public static void AddMarkerForRoute(int routeIndex, BusStop theStop)
    {
        changingStopsToAddMarkersToo.get(routeIndex).add(theStop);
    }
}