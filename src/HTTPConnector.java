package com.example.combiningprojects;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by brend on 23/11/2016.
 */

public class HTTPConnector {
//TODO SHOULD USE THIS FOR PARRAMTER AND URL BUILDING WITH ENCODING
    public String getUrl(JSONObject jsonObject) {
        JSONObject json = jsonObject;
        String sParams;
        sParams = json.toString();
/*
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&";

        // Output format
        String output = "json";

        // Mode
        String mode = "&mode=walking";
*/
        // Building the url to the web service
        String url = "http://www.rtpi.ie/connectservice.svc/GetStopsForService";

        return url;
    }

    //Build a useable url for the geocode api
    public static String getGeoUrl(String address) throws UnsupportedEncodingException {
        final String params = "+Cork+country=IE";
        String geoUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=";
        String returnUrl = geoUrl+ URLEncoder.encode(address, "UTF-8")+params;
        return returnUrl;
    }

    //Build a useable url for the geocode api
    public static String getGeoUrl(double lat, double lng){
        final String params = lat+","+lng;
        String geoUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=";
        String returnUrl = geoUrl+params;
        return returnUrl;
    }

    //Open a connection with the passed through data
    static String OpenConnection(HTTPConnectionData connectionData) throws IOException {
        String urlActual = connectionData.url;

        if(connectionData.paramsString != null && !connectionData.paramsString.isEmpty()) {
            urlActual += connectionData.paramsString+MainActivity.key;
        }

        URL url = new URL(urlActual);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("USER-AGENT", "Mozilla/5.0");
        connection.setRequestProperty("ACCEPT-LANGUAGE", "en-US,en;0.5");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        final StringBuilder output = new StringBuilder("Request URL " + url);

        if(connectionData.paramsJSON != null) {
            // write parameter to post
            OutputStream urlParam = connection.getOutputStream();
            // use buffer
            BufferedWriter buff = new BufferedWriter(new OutputStreamWriter(urlParam));
            buff.write(connectionData.paramsJSON.toString());
            buff.flush();
            buff.close();
            // close the Stream
            urlParam.close();

            output.append(System.getProperty("line.separator") + "Request Parameters " + connectionData.paramsJSON.toString());
        }

        int responseCode = connection.getResponseCode();

        //Debug printouts
        //CustomLogger.println("this is the error stream " + connection.getErrorStream() + "--------------------\n");
        //CustomLogger.println("Responde Code: " + responseCode);

        output.append(System.getProperty("line.separator") + "Response Code " + responseCode);

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line = "";
        StringBuilder responseOutput = new StringBuilder();
        while ((line = br.readLine()) != null) {
            responseOutput.append(line + "\n");
        }
        br.close();

        String result = responseOutput.toString();
        //CustomLogger.println("result reads : " + result);

        return result;
    }

    static JSONArray OpenConnectionParseRTPI(HTTPConnectionData connectionData) throws IOException {
        String result = OpenConnection(connectionData);

        return parseRtpi(result);
    }

    static JSONArray OpenConnectionParseGeo(HTTPConnectionData connectionData) throws IOException, JSONException {
        String result = OpenConnection(connectionData);

        return parseGeo(result);
    }

    /*
    public static class OpenConnectionAsync extends AsyncTask<HTTPConnectionData, Void, String> {
        public AsyncResponse delegate = null;

        boolean shouldParseRTPI = false;

        @Override
        protected String doInBackground(HTTPConnectionData... connectionData){
            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = OpenConnection(connectionData[0]);

                if(connectionData[0].paramsJSON != null)
                {
                    shouldParseRTPI = true;
                }
            } catch (Exception e) {
                CustomLogger.println("Error in FetchUrltest");
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(shouldParseRTPI) {
                try {
                    delegate.processFinish(parseRtpi(result));
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{ //parse geo location
                try {
                    delegate.processFinish(parseGeo(result));
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    */

    //Parse the result from the RTPI requests
    public static JSONArray parseRtpi(String rawData)
    {
        //CustomLogger.println("***********entered parseRTPI************");
        String newData = rawData;
        JSONObject json;
        JSONArray stopNames = new JSONArray();

        //variable
        if (rawData.contains("MapStopDetailedResponse")) {
            try {
                newData = newData.replaceAll("\"d\":","");
                newData = newData.replaceAll("#DALShared.DataClasses.Connect","");
                newData = newData.replaceAll("\"__type\":\"MapStopDetailedResponse:\",","");
                newData = newData.replaceAll("\\\\", "");
                newData = newData.substring(1, newData.length() - 1);
                json = new JSONObject(newData);
                stopNames = json.getJSONArray("Stops");
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(rawData.contains("ServiceVariantIds")){
            try {
                newData = newData.replaceAll("\"d\":","");
                newData = newData.replaceAll("#DALShared.DataClasses.Connect","");
                newData = newData.replaceAll("\\\\", "");
                newData = newData.substring(1, newData.length() - 1);
                json = new JSONObject(newData);
                stopNames = json.getJSONArray("FoundServices");
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                newData = newData.replaceAll("\"d\":","");
                newData = newData.replaceAll("#DALShared.DataClasses.Connect","");
                newData = newData.replaceAll("\"__type\":\"StopServiceResponse:\",","");
                newData = newData.replaceAll("\\\\", "");
                newData = newData.substring(1, newData.length() - 1);
                json = new JSONObject(newData);
                stopNames = json.getJSONArray("AllServices");
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }
        //end variable
        return stopNames;
    }

    public static JSONArray parseGeo(String rawData) throws JSONException {
        //CustomLogger.println("***********entered parseGEO************");
        String newData = rawData;
        JSONObject json;
        JSONArray stopNames = new JSONArray();

        json = new JSONObject(rawData);

        JSONArray results = json.getJSONArray("results");

        return results;
    }

}
