package com.example.combiningprojects;

import android.location.Location;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

//Finds the nearest stops for a specific route eg 220
public class FindNearestStopForSpecificRoute_CurrentLocation extends AsyncTask<NearestStopForRouteData, Void, BusStop>
{
    public interface AsyncResponse
    {
        void FindNearestStopForSpecificRoute_CurrentLocation_ProcessFinish(BusStop output) throws JSONException;
    }

    //use to find the location nearest
    protected static Location loc; // store the users lat,lng as a location
    protected double userLat; //user lat
    protected double userLng; //user lng
    protected String busRoute; //desired route

    public AsyncResponse delegate = null;

    @Override
    protected BusStop doInBackground(NearestStopForRouteData... nearestStopForRouteData)
    {
        busRoute = nearestStopForRouteData[0].GetPublicServiceCode();
        userLat = nearestStopForRouteData[0].GetLat();
        userLng = nearestStopForRouteData[0].GetLng();

        if (userLat == Double.MAX_VALUE) {
            throw new AssertionError("Latitude has not been inited correctly");
        }
        if (userLng == Double.MAX_VALUE) {
            throw new AssertionError("Longitude has not been inited correctly");
        }

        loc = new Location("User Location");
        loc.setLongitude(userLng);
        loc.setLatitude(userLat);

        BusStop nearestStopOnRoute = null;

        try {
            nearestStopOnRoute = BusServiceMap.GetStopFromDescriptor(BusStopHelper.FindNearestStop(busRoute, loc));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //CustomLogger.println("******** NEAREST BUS CLASS **********");

        return nearestStopOnRoute;
    }

    @Override
    protected void onPostExecute(BusStop nearestHit) {
        super.onPostExecute(nearestHit);

        try {
            delegate.FindNearestStopForSpecificRoute_CurrentLocation_ProcessFinish(nearestHit);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

