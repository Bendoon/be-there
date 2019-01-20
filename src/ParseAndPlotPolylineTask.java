package com.example.combiningprojects;

import android.util.Log;
/*
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.PatternItem;
*/
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A class to parse the Google Places in JSON format
 */
public class ParseAndPlotPolylineTask //extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>>
{
    // Parsing the data in non-ui thread
    String isWalking;

    protected PolylineOptions doBlocking(String jsonData, int color) throws JSONException
    {
        boolean isWalking = false;

        if(jsonData.contains("WALKING"))
        {
            isWalking = true;
        }

        JSONObject jObject;
        List<List<HashMap<String, String>>> routes = null;

        jObject = new JSONObject(jsonData);
        //Log.d("ParseAndPlotPoly", jsonData.toString());
        DataParser parser = new DataParser();
        //Log.d("ParseAndPlotPoly", parser.toString());

        // Starts parsing data
        routes = parser.parse(jObject);
        //Log.d("ParseAndPlotPolylineTask","Executing routes");
        //Log.d("ParseAndPlotPolylineTask",routes.toString());

        //return routes;

        ArrayList<LatLng> points;
        PolylineOptions lineOptions = null;

        // Traversing through all the routes
        for (int i = 0; i < routes.size(); i++) {
            points = new ArrayList<>();
            lineOptions = new PolylineOptions();

            // Fetching i-th route
            List<HashMap<String, String>> path = routes.get(i);

            // Fetching all the points in i-th route
            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }

            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points);

            //if it isnt a marker then we make the trail red
            if (!isWalking)
            {
                lineOptions.zIndex(0);
                lineOptions.width(10);
            }
            else
            {
                lineOptions.zIndex(1);
                lineOptions.width(20);
            }

            lineOptions.color(color);

            //Log.d("onPostExecute", "onPostExecute lineoptions decoded");
        }

        return lineOptions;

        /*
        // Drawing polyline in the Google Map for the i-th route
        if (lineOptions != null) {
            //if it is a marker store the polyline to remove it
            if (MapsActivity.isMarker) {
                MapsActivity.polylineMarker = MapsActivity.mMap.addPolyline(lineOptions);
            }
            //else its a line for the bus route
            else if (!MapsActivity.isMarker) {
                MapsActivity.polylineForMap = MapsActivity.mMap.addPolyline(lineOptions);
            }
            //MapsActivity.polyline.isClickable();
            else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
        */
    }
}

