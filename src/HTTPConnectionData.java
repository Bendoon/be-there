package com.example.combiningprojects;

import org.json.JSONObject;

/**
 * Created by brend on 27/11/2016.
 */

public class HTTPConnectionData
{
    public String url;
    public JSONObject paramsJSON;
    public String paramsString;

    public HTTPConnectionData(String _url, JSONObject _paramsJSON)
    {
        url = _url;
        paramsJSON = _paramsJSON;
    }

    public HTTPConnectionData(String _url, String _paramsString)
    {
        url = _url;
        paramsString = _paramsString;
    }

    public HTTPConnectionData(String _url)
    {
        url = _url;
    }
}