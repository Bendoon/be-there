package com.example.combiningprojects;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brend on 19/11/2016.
 */

public class ManipulateURL {
     private static int requestCounter = 0;
        //getDirectonsUrl
        public static String getDirectionsUrl(LatLng origin, LatLng dest, String modeUser, String params) {

            // Origin of route
            String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

            // Destination of route
            String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

            // Building the parameters to the web service
            String parameters = str_origin + "&" + str_dest + "&";

            // Output format
            String output = "json";

            // Mode
            String mode = "&mode="+modeUser;

            //params
            String parameters2=params;

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + mode + parameters2;

            return url;
        }

        /**
         * A method to download json data from url
         */
        public static String downloadUrl(String strUrl) throws IOException {
            SSLUtilities.trustAllHostnames();
            SSLUtilities.trustAllHttpsCertificates();

            String data = "";
            InputStream iStream = null;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(strUrl);

                // Creating an http connection to communicate with url
                urlConnection = (HttpURLConnection) url.openConnection();

                // Connecting to url
                urlConnection.connect();

                // Reading data from url
                iStream = urlConnection.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

                StringBuffer sb = new StringBuffer();

                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                data = sb.toString();
                //Log.d("downloadUrl", data.toString());
                br.close();

            } catch (Exception e) {
                Log.d("Exception", e.toString());
            } finally {
                iStream.close();
                urlConnection.disconnect();
            }
            return data;
        }

        /*
        // Fetches data from url passed
        public static class FetchUrl extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... url) {

                // For storing data from web service
                String data = "";

                try {
                    // Fetching the data from web service
                    data = downloadUrl(url[0]);
                    Log.d("Background Task data", data.toString());
                } catch (Exception e) {
                    Log.d("Background Task", e.toString());
                }
                return data;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                String isWalking = "false";

                if(result.contains("WALKING"))
                {
                    isWalking = "true";
                }

                ParseAndPlotPolylineTask parseAndPlotPolylineTask = new ParseAndPlotPolylineTask();

                // Invokes the thread for parsing the JSON data
                parseAndPlotPolylineTask.execute(result, isWalking);
            }
        }
        */

    static long timeStartLimitCheck = -1;

    static class requestCountWithTimeStamp
    {
        public int requestCount;
        public long timeMade;

        public requestCountWithTimeStamp(int _requestCount, long _timeMade)
        {
            requestCount = _requestCount;
            timeMade = _timeMade;
        }
    }

    static List<requestCountWithTimeStamp> requestsWithTimeStamps = new ArrayList<>();

    static final int maxRequestsPerSecond = 40;
    static final float timeWindowToWorkWith = 1.1f;
    //static StopWatch stopWatch = new StopWatch();
    public static class FetchUrl
    {
        public PolylineOptions DoBlocking(String url, int counter, int color) throws IOException, JSONException, InterruptedException
        {
            PruneRequestCountList(timeWindowToWorkWith);
            float requestsMadeLastSecond = calculateTotalRequestsInList();
            int requestsRemaining = maxRequestsPerSecond - (int)Math.ceil(requestsMadeLastSecond);

            //System.out.println("request required: " + counter + ", requestsMadeLastSecond: " + requestsMadeLastSecond + ", Requests Remaining: " + requestsRemaining);

            /*
            while(requestsRemaining < counter)
            {
                // Need to wait for it to reset
                PruneRequestCountList(timeWindowToWorkWith);
                requestsMadeLastSecond = calculateTotalRequestsInList();
                requestsRemaining = maxRequestsPerSecond - (int)Math.ceil(requestsMadeLastSecond);

                //System.out.println("Not enough requests left in the time window, requests Required: " + counter + ", Requests Remaining: " + requestsRemaining);

                Thread.sleep(50);
            }
            */

            /*
            // Fetching the data from web service
            requestCounter += counter;
            //if there has been 50 requests let it sleep for a period if we are getting too many requests per second
            if(requestCounter >= 50)
            {
                long timeSinceFirstRequest = System.nanoTime() - timeStartLimitCheck;

                Thread.sleep(1000);
                requestCounter -= 50;
            }
            */


            //stopWatch.StartTimeInterval();
            String data = downloadUrl(url);
            //stopWatch.StopTimeInterval("Time spent downloadUrl()");

            int infiniteLockProtect = 0;
            //int maxLoops = 5;
            int maxLoops = 100;

            while(data.contains("error") && infiniteLockProtect < maxLoops)
            {
                System.out.println("*** We hit an error ***");

                //Thread.sleep(1500);
                Thread.sleep(50);

                // Attempt to grab the data again
                //stopWatch.StartTimeInterval();
                data = downloadUrl(url);
                //stopWatch.StopTimeInterval("Time spent in error block downloadUrl()");

                ++infiniteLockProtect;
            }

            requestsWithTimeStamps.add(0, new requestCountWithTimeStamp(counter, System.nanoTime()));

            ParseAndPlotPolylineTask parseAndPlotPolylineTask = new ParseAndPlotPolylineTask();
            // Invokes the thread for parsing the JSON data
            PolylineOptions returnLineOptions = parseAndPlotPolylineTask.doBlocking(data, color);

            return returnLineOptions;
        }

        static final double oneSecondInNano = 1000000000.0;
        void PruneRequestCountList(float timeWindowToKeep)
        {
            long currentTime = System.nanoTime();

            int removeUpToIndex = -1;
            for(int x = 0; x < requestsWithTimeStamps.size(); ++x)
            {
                if(((currentTime - requestsWithTimeStamps.get(x).timeMade) / oneSecondInNano) > timeWindowToKeep)
                {
                    //System.out.println("Pruning element: " + x + ", currentTime: " + (currentTime / oneSecondInNano) + ", requestedTime: " + (requestsWithTimeStamps.get(x).timeMade / oneSecondInNano));
                    requestsWithTimeStamps.remove(x);
                }
            }
        }

        float calculateTotalRequestsInList()
        {
            float total = 0f;
            for(int x = 0; x < requestsWithTimeStamps.size(); ++x)
            {
                total += requestsWithTimeStamps.get(x).requestCount;
            }

            return total;
        }
    }
}
