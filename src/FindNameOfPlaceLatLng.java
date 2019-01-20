package com.example.combiningprojects;

import android.location.Location;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by brend on 17/01/2017.
 */

public class FindNameOfPlaceLatLng
{
    public interface AsyncResponse
    {
        void FindNameOfPlaceLatLng_ProcessFinish(String output) throws JSONException;
    }

    public static class FindNameOfPlaceLatLngAsync extends AsyncTask<Location, Void, String>
    {
        public AsyncResponse delegate = null;
        String returnData = "";
        JSONArray returnedOutput;
        double lat;
        double lng;

        @Override
        protected String doInBackground(Location... location) {
            lat = location[0].getLatitude();
            lng = location[0].getLongitude();

           //CustomLogger.println("///////////////////////////////////////////////////////////////////////");
           //CustomLogger.println("///////////////////////////////////////////////////////////////////////");
           //CustomLogger.println("REQUESTED LOCATION NAME");
           //CustomLogger.println("///////////////////////////////////////////////////////////////////////");
           //CustomLogger.println("///////////////////////////////////////////////////////////////////////");

            try {
                returnedOutput = HTTPConnector.OpenConnectionParseGeo(new HTTPConnectionData(HTTPConnector.getGeoUrl(lat,lng)));

                JSONObject rec2 = returnedOutput.getJSONObject(0);
                JSONArray address_components = rec2.getJSONArray("address_components");
                JSONObject jsonObject = address_components.getJSONObject(1); //change int to get next next long name
                returnData = jsonObject.getString("long_name");

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return returnData;
        }

        @Override
        protected void onPostExecute(String data)
        {
            super.onPostExecute(data);

            try {
                delegate.FindNameOfPlaceLatLng_ProcessFinish(data);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
