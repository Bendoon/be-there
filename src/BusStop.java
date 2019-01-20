package com.example.combiningprojects;

import android.location.Location;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by brend on 12/11/2016.
 */
//Class for holding a single bus stop with name,latitude,longitude and the current distance from user location.
public class BusStop implements Serializable{

        private String name;
        private String publicServiceCode;
        private double lat;
        private double lng;
        private int stopSequenceIndex;
        private int globalStopID;

        HashMap<Location, LocationBasedData> locationBasedDataHashMap = new HashMap<Location, LocationBasedData>();

        public BusStop(String publicServiceCode, String name, double lat, double lng, int stopSequenceIndex, int _globalStopID)
        {
            this.publicServiceCode = publicServiceCode;
            this.name = name;
            this.lat = lat;
            this.lng = lng;
            this.stopSequenceIndex = stopSequenceIndex;
            this.globalStopID = _globalStopID;
        }

        // getters
        public String getName()
        {
            return name;
        }
        public String getPublicServiceCode()
    {
        return publicServiceCode;
    }
        public double getLat()
        {
            return lat;
        }
        public double getLng() { return lng; }
        public double getStopSequenceIndex() { return stopSequenceIndex; }
        public int getGlobalStopID() { return globalStopID; }


        // setters
        public void setLat(double lat)
        {
            this.lat = lat;
        }
        public void setLng(double lng)
        {
            this.lng = lng;
        }
        public void setName(String name)
        {
            this.name = name;

        }

        public void setDistanceFromUserLocation(float distanceFromUserLocation, Location loc)
        {
            LocationBasedData returnData = locationBasedDataHashMap.get(loc);
            if(returnData == null)
            {
                returnData = new LocationBasedData();
                locationBasedDataHashMap.put(loc,returnData);
            }
            returnData.distanceFromUserLocation = distanceFromUserLocation;
        }

        public void setLocationDependentVariables(int walkingTimeSeconds,String walkingTimeMinutes, Location loc )
        {
            LocationBasedData locationBasedData = GetLocationBasedData(loc);
            locationBasedData.walkingTimeMinutes = walkingTimeMinutes;
            locationBasedData.walkingTimeSeconds = walkingTimeSeconds;
        }

        LocationBasedData GetLocationBasedData(Location loc)
        {
            LocationBasedData returnData = locationBasedDataHashMap.get(loc);
            return returnData;
        }

        public float getDistanceFromUserLocation(Location loc)
        {
            return GetLocationBasedData(loc).distanceFromUserLocation;
        }

        public int getWalkingTimeSeconds(Location loc)
        {
            return GetLocationBasedData(loc).walkingTimeSeconds;
        }
        public String getWalkingTimeMinutes(Location loc)
        {
            return GetLocationBasedData(loc).walkingTimeMinutes;
        }
}
