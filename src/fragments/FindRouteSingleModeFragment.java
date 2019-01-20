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

import com.example.combiningprojects.FindNameOfPlaceLatLng;
import com.example.combiningprojects.MainActivity;
import com.example.combiningprojects.MapsActivity;
import com.example.combiningprojects.R;

/**
 * Created by brend on 01/04/2017.
 */

public class FindRouteSingleModeFragment extends Fragment implements View.OnClickListener {

    EditText busRoute;
    MainActivity mainActivity;
    Location userloc;
    Button button;
    EditText destination;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mainActivity = new MainActivity();
        userloc = mainActivity.getUserLocation();

        View view = inflater.inflate(R.layout.find_route_singlemode_fragment_layout, container, false);

        TextView planAheadSingleHeadingText = (TextView) view.findViewById(R.id.planAheadSingleHeadingText);
        TextView planAheadSingleSearchText = (TextView) view.findViewById(R.id.planAheadSingleSearchText);
        button = (Button) view.findViewById(R.id.planAheadSingleGoButton);
        button.setOnClickListener(this);

        destination = (EditText) view.findViewById(R.id.searchFieldplanAheadSingle);

        Typeface custom_font = Typeface.createFromAsset(getActivity().getAssets(),  "fonts/BlenderPro-Book_0.otf");

        planAheadSingleHeadingText.setTypeface(custom_font);
        planAheadSingleSearchText.setTypeface(custom_font);
        button.setTypeface(custom_font);

        return view;
    }

    @Override
    public void onClick(View view) {
        //Starts the search for nearby stops to a destination
            Intent startMaps = new Intent(getActivity(), MapsActivity.class);
            String text = destination.getText().toString();
            startMaps.putExtra("destination", text);
            startMaps.putExtra("userLat", userloc.getLatitude());
            startMaps.putExtra("userLng", userloc.getLongitude());
            startMaps.putExtra("multiMode", false);
            startMaps.putExtra("mapMode", MapsActivity.MapMode.FindNearestStopAnyRoute_ForDestination);
            startMaps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(startMaps);
    }
}
