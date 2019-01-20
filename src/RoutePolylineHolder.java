package com.example.combiningprojects;

import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brend on 28/03/2017.
 */

public class RoutePolylineHolder
{
    public class PolyLineWithMetaData
    {
        PolylineOptions polyLineOptions;
        public boolean isWalking;

        PolyLineWithMetaData(PolylineOptions _polylineOptions, boolean _isWalking)
        {
            polyLineOptions = _polylineOptions;
            isWalking = _isWalking;
        }
    }

    List<PolyLineWithMetaData> polyLinesWithMetaData = new ArrayList<PolyLineWithMetaData>();

    public void AddPolyLineOptions(PolylineOptions polylineOptions, boolean isWalking)
    {
        PolyLineWithMetaData polyLineWithMetaData = new PolyLineWithMetaData(polylineOptions, isWalking);
        polyLinesWithMetaData.add(polyLineWithMetaData);
    }

    public List<PolyLineWithMetaData> GetPolyLines()
    {
        return polyLinesWithMetaData;
    }
}
