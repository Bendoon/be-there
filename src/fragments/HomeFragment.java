package com.example.combiningprojects.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.example.combiningprojects.R;

import org.w3c.dom.Text;

import static android.R.attr.button;

public class HomeFragment extends Fragment implements View.OnClickListener {
        Button button;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment

            View view = inflater.inflate(R.layout.home_fragment_layout, container, false);
            TextView welcomeText = (TextView) view.findViewById(R.id.welcomeText);
            TextView instructionsText = (TextView) view.findViewById(R.id.instructionsText);
            TextView dijkstraText = (TextView) view.findViewById(R.id.dijkstraText);

            final Animation animation = new AlphaAnimation(1, 0.3f); // Change alpha from fully visible to invisible
            animation.setDuration(1000); // duration - half a second
            animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
            animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
            animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
            button = (Button) view.findViewById(R.id.letsGoButton);
            button.setOnClickListener(this);
            button.startAnimation(animation);

            Typeface custom_font = Typeface.createFromAsset(getActivity().getAssets(),  "fonts/BlenderPro-Book_0.otf");

            welcomeText.setTypeface(custom_font);
            instructionsText.setTypeface(custom_font);
            dijkstraText.setTypeface(custom_font);
            button.setTypeface(custom_font);

            return view;
        }

    @Override
    public void onClick(View v)
    {
        Fragment fragment = null;
        Class fragmentClass = null;

        fragmentClass = FindNearestStopFragment.class;

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
        v.clearAnimation();
    }
}