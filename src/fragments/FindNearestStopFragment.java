package com.example.combiningprojects.fragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.IntentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.combiningprojects.MainActivity;
import com.example.combiningprojects.MapsActivity;
import com.example.combiningprojects.PopulateBusStopsNewAsync;
import com.example.combiningprojects.R;

/**
 * Created by brend on 01/04/2017.
 */

public class FindNearestStopFragment extends Fragment implements View.OnClickListener {

    EditText busRoute;
    MainActivity mainActivity;
    Location userloc;
    Button button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mainActivity = new MainActivity();
        userloc = mainActivity.getUserLocation();

        View view = inflater.inflate(R.layout.find_nearest_stop_fragment_layout, container, false);

        TextView findNearestStopText = (TextView) view.findViewById(R.id.findNearestStopText);
        TextView nearestStopSearchText = (TextView) view.findViewById(R.id.nearestStopSearchText);
        button = (Button) view.findViewById(R.id.nearestStopGoButton);
        button.setOnClickListener(this);

        busRoute = (EditText) view.findViewById(R.id.searchFieldNearestStop);

        Typeface custom_font = Typeface.createFromAsset(getActivity().getAssets(),  "fonts/BlenderPro-Book_0.otf");

        findNearestStopText.setTypeface(custom_font);
        nearestStopSearchText.setTypeface(custom_font);
        button.setTypeface(custom_font);
        return view;
    }

    //Method to check if the user is in fact actually looking for a real route
    public boolean isRoute(String busRoute)
    {
        boolean isRoute = false;
        for(int n = 0; n < PopulateBusStopsNewAsync.busRoutes.length; ++n)
        {
            if(busRoute.equalsIgnoreCase(PopulateBusStopsNewAsync.busRoutes[n]))
            {
                isRoute = true;
                break;
            }
        }
        return isRoute;
    }

    @Override
    public void onClick(View v) {
        if (isRoute(busRoute.getText().toString()))
        {
            Intent startMaps = new Intent(getActivity(), MapsActivity.class);
            String text = busRoute.getText().toString();
            startMaps.putExtra("busRoute", text);
            startMaps.putExtra("userLat", userloc.getLatitude());
            startMaps.putExtra("userLng", userloc.getLongitude());
            startMaps.putExtra("mapMode", MapsActivity.MapMode.FindNearestStopForSpecificRoute_CurrentLocation);
            startMaps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
            getActivity().startActivity(startMaps);
        }
        else
        {
            Toast.makeText(getActivity(),"Please enter a valid public service code",Toast.LENGTH_LONG).show();
        }
    }
}
