package com.example.combiningprojects;

/**
 * Created by brend on 10/02/2017.
 */

public class BusStopPair implements Comparable<BusStopPair> {

    // TODO - These could become BusStopDescriptor
    BusStopDescriptor orginStop;
    BusStopDescriptor destinationStop;
    int totalWalkingTimeForOriginAndDestination;

    public BusStopPair(BusStopDescriptor _orginStop, BusStopDescriptor _destinationStop)
    {
        orginStop = _orginStop;
        destinationStop = _destinationStop;
    }

    //Getters
    public BusStopDescriptor GetOriginStop(){return orginStop;}
    public BusStopDescriptor GetDestinationStop(){return destinationStop;}

    //Setters
    public void SetTotalWalkingTimeForOriginAndDestination(int _totalWalkingTimeForOriginAndDestination)
    {
        this.totalWalkingTimeForOriginAndDestination = _totalWalkingTimeForOriginAndDestination;
    }

    @Override
    public int compareTo(BusStopPair o) {
        return this.totalWalkingTimeForOriginAndDestination - o.totalWalkingTimeForOriginAndDestination;
    }
}
