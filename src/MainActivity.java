package com.example.combiningprojects;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.combiningprojects.fragments.FindNearestStopFragment;
import com.example.combiningprojects.fragments.FindRouteMultiModeFragment;
import com.example.combiningprojects.fragments.FindRouteSingleModeFragment;
import com.example.combiningprojects.fragments.HomeFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements  GoogleApiClient.ConnectionCallbacks,
                                                                GoogleApiClient.OnConnectionFailedListener,
                                                                LocationListener,
                                                                PopulateBusStopsNewAsync.AsyncResponse,
                                                                NavigationView.OnNavigationItemSelectedListener{

    public static final String key = "&key=AIzaSyBENuy17orRkXiqCrYfKve7Hu5RSiWe1JM";
    private static final boolean useDebugLocation = true;
    private static Boolean populatedBusStops = false;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    SerilizationStoreAndRetrieveData serilizationStoreAndRetrieveData;

    private GoogleApiClient googleApiClient;

    private LocationRequest locationRequest;

    public static LatLng home;

    public static Double myLatitude;
    public static Double myLongitude;

    public static Location userLoc;

    private ProgressBar spinner;

    PopulateBusStopsNewAsync populateBusStopsNewAsync = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userLoc = new Location("UserLocation");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Fragment fragment = null;
        Class fragmentClass = null;
        fragmentClass = HomeFragment.class;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendEmail();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        spinner = (ProgressBar) findViewById(R.id.progressBar1);
        serilizationStoreAndRetrieveData = new SerilizationStoreAndRetrieveData();

        Intent mapBack = getIntent();
        boolean backFromMap;
        backFromMap = mapBack.getBooleanExtra("backFromMap",false);

        if(!backFromMap)
        {
            try {
                checkForSaveData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!populatedBusStops) {
            spinner.setVisibility(View.VISIBLE);
        } else {
            spinner.setVisibility(View.GONE);
        }

        if (useDebugLocation)
        {
            SetDebugLocation();
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if(!googleApiClient.isConnected())
        {
            googleApiClient.connect();
        }

        //If it is the first time being ran poputlate the bus stops
        if(!populatedBusStops)
        {
            StartPopulateBusStopsAsync();
        }
    }

    protected void sendEmail() {
        Log.i("Send email", "");

        String[] TO = {"brendan.muldoon@umail.ucc.ie"};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");

        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "BÉ THERE FEEDBACK");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            finish();
            Log.i("Finished sending", "");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this,
                    "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public Location getUserLocation()
    {
        userLoc.setLatitude(myLatitude);
        userLoc.setLongitude(myLongitude);
        return userLoc;
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;
        Class fragmentClass = null;
        if (id == R.id.home) {
            fragmentClass = HomeFragment.class;
        } else if (id == R.id.nearestStop) {
            fragmentClass = FindNearestStopFragment.class;
        } else if (id == R.id.planAheadSingle) {
            fragmentClass = FindRouteSingleModeFragment.class;
        } else if (id == R.id.planAheadMulti) {
            fragmentClass = FindRouteMultiModeFragment.class;
        } else if (id == R.id.nav_share) {
            fragmentClass = FindRouteMultiModeFragment.class;
        } else if (id == R.id.nav_send) {
            fragmentClass = FindRouteMultiModeFragment.class;
        }
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void PopulateTestServiceData()
    {
        /////////////////////////////////////
        //                                 //
        //   Test code for mock hashmap    //
        /////////////////////////////////////

        BusServiceMap.hashMap.clear();

        BusService service1 = new BusService("Service1");
        service1.AddRoute();
        service1.AddStopToRoute(0,new BusStop("Service1", "A", 0.0, 0.0, 0, 0));
        service1.AddStopToRoute(0,new BusStop("Service1", "B", 0.0, 0.0, 1, 1));
        service1.AddStopToRoute(0,new BusStop("Service1", "C", 0.0, 0.0, 2, 2));
        BusServiceMap.hashMap.put("Service1", service1);

        BusService service2 = new BusService("Service2");
        service2.AddRoute();
        service2.AddStopToRoute(0,new BusStop("Service2", "E", 0.0, 0.0, 0, 3));
        service2.AddStopToRoute(0,new BusStop("Service2", "F", 0.0, 0.0, 1, 4));
        service2.AddStopToRoute(0,new BusStop("Service2", "G", 0.0, 0.0, 2, 5));
        BusServiceMap.hashMap.put("Service2", service2);

        BusService service3 = new BusService("Service3");
        service3.AddRoute();
        service3.AddStopToRoute(0,new BusStop("Service3", "I", 0.0, 0.0, 0, 6));
        service3.AddStopToRoute(0,new BusStop("Service3", "J", 0.0, 0.0, 1, 7));
        service3.AddStopToRoute(0,new BusStop("Service3", "K", 0.0, 0.0, 2, 8));
        BusServiceMap.hashMap.put("Service3", service3);

        PopulateBusStopsNewAsync.busRoutes = new String[]{"Service1","Service2","Service3"};

        /*
        BusService testService = BusServiceMap.hashMap.get("201");
        BusServiceMap.hashMap.clear();
        BusServiceMap.hashMap.put("201",testService);
        PopulateBusStopsNewAsync.busRoutes = new String[]{"201"};
        */
    }

    void SetDebugLocation()
    {
        //My house
        myLatitude = 51.892768;
        myLongitude = -8.502867;

        //Western Gateway
        //myLatitude = 51.893112;
        //myLongitude = -8.500418;

        //CIT
        //myLatitude = 51.885393;
        //myLongitude = -8.534421;

        //Western Road castlewhite apartments
        //myLatitude = 51.894115;
        //myLongitude = -8.497608;

        //Aidans House
        //myLatitude = 51.8708800;
        //myLongitude = -8.3957250;

        //South mall
        //myLatitude = 51.897026;
        //myLongitude = -8.470556;

        //Lukes house
        //myLatitude = 51.8717920;
        //myLongitude = -8.4399820;

        //Start of Mount Oval route
        //myLatitude = 51.893241;
        //myLongitude =  -8.465802;
    }

    //Populate the database of bus stops
    public void StartPopulateBusStopsAsync()
    {
        populateBusStopsNewAsync = new PopulateBusStopsNewAsync();
        populateBusStopsNewAsync.delegate = this;
        populateBusStopsNewAsync.execute();
    }

    //Check for the save data on the SD card or other platform
    public void checkForSaveData() throws Exception {
        if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    isStoragePermissionGranted();
                    //System.out.println("Permission is granted");
                    //File write logic here
                }
            }
            else
            {
                isStoragePermissionGranted();
            }
            //Check if there is a save file on the external memory
            //If there is load the stops from it, else populate the stops.
            File file = new File("/sdcard/saveDataForBÉThere.bin");

            if (file.isDirectory())
                file = file.getParentFile();
            if (file.exists()){
                BusServiceMap.hashMap = (HashMap<String, BusService>)serilizationStoreAndRetrieveData.
                        loadSerializedObject(new File("/sdcard/saveDataForBÉThere.bin")); //get the serialized object from the sdcard and cast it into the custom data.
                if(BusServiceMap.hashMap != null)
                {
                    OnCompletionPopulateBusStops();
                    //System.out.println("Stops Load Success");
                }
                else
                {
                    //System.out.println("Stops Load Fail");
                    //throw new Exception("Could not load the saved data even though it exists, have you changed the format?");

                    System.out.println("Could not load the saved data even though it exists, have you changed the format?");
                    boolean deleted = file.delete();
                    if(deleted)
                    {
                        System.out.println("Deleted out of data save data, it should now be re-created");
                    }
                    else
                    {
                        throw new Exception("Could not delete the saved data!");
                    }
                }
            }
        }
    }

    public void settingsrequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000); //5 * 1000 i was using for both
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //CustomLogger.println("Permission is granted");
                return true;
            } else {
                //CustomLogger.println("Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            //CustomLogger.println("Permission is granted");
            return true;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    private static final int TAG_CODE_PERMISSION_LOCATION = 1;
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                            TAG_CODE_PERMISSION_LOCATION);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //CustomLogger.println("ON CONNECTION FAILED READS : " + connectionResult);

        //TODO NEED TO TOAST OR SOMETHING TO THE USER HERE TO NOTIFY ABOUT DIFFERENT ERRORS RECIEVED
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // set the message
        builder.setMessage("You need to update Google Play Services in order for this APP to work.");
    }

    @Override
    public void onLocationChanged(Location location) {
        JSONObject test = new JSONObject();

       myLatitude = location.getLatitude();
       myLongitude = location.getLongitude();

        if (useDebugLocation) {
            SetDebugLocation();
        }

        try {
            test.put("point", location);
        } catch (JSONException e) {
            e.printStackTrace();
        }

       //CustomLogger.println("My Latitude = " + myLatitude);
       //CustomLogger.println("My Longitude = " + myLongitude);

        //CustomLogger.println("Lat = " + myLatitude + " Lng = " + myLongitude);
    }

    @Override
    protected void onStart() {
        super.onStart();
        settingsrequest();
        if(!googleApiClient.isConnected())
        {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MapsActivity.isMarker = false;
        if(googleApiClient.isConnected())
        {
            requestLocationUpdates();
        }
        else
        {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void PopulateBusStopsNew_ProcessFinish() throws JSONException
    {
        spinner.setVisibility(View.GONE);
        serilizationStoreAndRetrieveData.saveObject(BusServiceMap.hashMap);
        OnCompletionPopulateBusStops();
    }

    void OnCompletionPopulateBusStops()
    {
        populatedBusStops = true;
        //PopulateTestServiceData();
        PopulateBusStopsNewAsync.RemoveDuplicateRoutes();

        PopulateBusStopsNewAsync.PostProcessData();
    }
}

