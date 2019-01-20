package com.example.combiningprojects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by brend on 22/01/2017.
 */

public class SerilizationStoreAndRetrieveData implements Serializable {
    Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

    public void saveObject(HashMap<String, BusService> stops){
        try
        {
            if(isSDPresent)
            {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("/sdcard/saveDataForBÉThere.bin"))); //Select where you wish to save the file...
                //ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.MEDIA_MOUNTED) + "/saveDataForBÉThere.bin")));
                // java.io.File xmlFile = new java.io.File(Environment
                //         .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                //         + "/Filename.xml");

                oos.writeObject(stops); // write the class as an 'object'
                oos.flush(); // flush the stream to insure all of the information was written to 'save_object.bin'
                oos.close();// close the stream
            }
            else
            {
                //TODO CREATE POP UP TO USER
                //CustomLogger.println("No SD card inserted");
            }
        }
        catch(Exception ex)
        {
            //Log.v("Save Error : ",ex.getMessage());
            ex.printStackTrace();
        }
    }

    public Object loadSerializedObject(File f)
    {
        try
        {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            Object o = ois.readObject();
            return o;
        }
        catch(Exception ex)
        {
            //Log.v("Read Error : ",ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    public Object loadSerializedGraph(File f)
    {
        try
        {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            Object o = ois.readObject();
            return o;
        }
        catch(Exception ex)
        {
            //Log.v("Read Error : ",ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }
}
