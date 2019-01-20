package com.example.combiningprojects;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Debug;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

//Finds the nearest stop for any route to a destination
public class FindNearestStopAnyRoute_ForDestination extends AsyncTask<NearestStopForDestinationInputData, Void, NearestStopForDestinationOutputData>
{
    public interface AsyncResponse
    {
        void FindNearestStopAnyRoute_ForDestination_ProcessFinish(NearestStopForDestinationOutputData output) throws JSONException, IOException, InterruptedException;
    }

    public AsyncResponse delegate = null;
    JSONArray returnedOutput;
    Location destinationLocation;
    Boolean allowMultipleBuses;
    Boolean useDijkstrasAlgor;
    protected static Location userLoc;

    @Override
    protected NearestStopForDestinationOutputData doInBackground(NearestStopForDestinationInputData... nearestStopForDestinationInputData) {

        System.out.println("Started findNearestStopAnyRoute_ForDestination::doInBackground(): " + (System.nanoTime() / 1000000000.0));

        allowMultipleBuses = nearestStopForDestinationInputData[0].GetMultiMode();
        useDijkstrasAlgor = nearestStopForDestinationInputData[0].GetDijkstra();
        destinationLocation = new Location("User Destination");
        userLoc = nearestStopForDestinationInputData[0].GetUserLocation();
        useDijkstrasAlgor = nearestStopForDestinationInputData[0].GetMultiMode();
        List<Vertex2> path = null;
        List<List<Vertex2>> listOfPaths = new ArrayList<>();

        NearestStopForDestinationOutputData outputData = null;

        try
        {
            //stopWatch.StartTimeInterval();

            returnedOutput = HTTPConnector.OpenConnectionParseGeo(new HTTPConnectionData(HTTPConnector.getGeoUrl(nearestStopForDestinationInputData[0].GetDestination())));

            JSONObject rec2 = returnedOutput.getJSONObject(0);
            JSONObject location = rec2.getJSONObject("geometry").getJSONObject("location");

            double lat = location.getDouble("lat");
            double lng = location.getDouble("lng");

            destinationLocation.setLatitude(lat);
            destinationLocation.setLongitude(lng);

            //stopWatch.StopTimeInterval("Time spent in OpenConnectionParseGeo()");

            //CustomLogger.println("Destination lat and lng are : " + lat + " " +lng);
            if(useDijkstrasAlgor)
            {
                //stopWatch.StartTimeInterval();
                BusStopHelper.populateCrowDistanceToAllStops(userLoc);
                //stopWatch.StopTimeInterval("Time spent in populateCrowDistanceToAllStops()");
                // CalculateRouteMultipleBuses() should just return a List<List<Vertex2>> and be asigned directly to listOfPaths
                listOfPaths = CalculateRouteMultipleBuses(userLoc,destinationLocation);
                //listOfPaths.add(0,CalculateRouteMultipleBuses(userLoc,destinationLocation));
                outputData = new NearestStopForDestinationOutputData(destinationLocation,listOfPaths);
            }
            else
            {
                //Lets now find the X nearestStops to the users location
                BusStopDescriptor[] crowFliesNearestBusStopsToUser;
                crowFliesNearestBusStopsToUser = BusStopHelper.crowFliesNearestBusStops(100, userLoc);
                ArrayList<String> serviceThatGoNearUser = BusStopHelper.getUniquePublicServiceCodes(crowFliesNearestBusStopsToUser);

                //Lets find the nearest X stops to the userNodeIndx destination
                BusStopDescriptor[] crowFliesNearestBusStopsToDest;
                crowFliesNearestBusStopsToDest = BusStopHelper.crowFliesNearestBusStops(100, destinationLocation);
                ArrayList<String> servicesThatGoNearDest = BusStopHelper.getUniquePublicServiceCodes(crowFliesNearestBusStopsToDest);

                //Call to findNearestStop for each route to find the best match
                ArrayList<String> commonServiceCodes = intersection(servicesThatGoNearDest ,serviceThatGoNearUser);

                //commonServiceCodes = new ArrayList<>();
                //commonServiceCodes.add("216");

                //BusStopPair[] routeOptions = new BusStopPair[commonServiceCodes.size()];
                List<BusStopPair> routeOptions = new ArrayList<BusStopPair>();
                //Find the nearest stop for the common route/routes
                for(int i = 0; i < commonServiceCodes.size(); ++i)
                {
                    // Check for the ordering of the origin and destination pairing, in the new system, the origin point must be BEFORE the destination in the route array
                    // If the origin comes after, the route is going the wrong way and we should discard the route
                    BusStopPair busStopPair = BusStopHelper.GetBestValidRoute(userLoc,destinationLocation,commonServiceCodes.get(i));
                    if(busStopPair != null)
                    {
                        routeOptions.add(busStopPair);
                    }
                }

                //Get the three best routes to show to the userNodeIndx, slot 0 will contain optimal route
                routeOptions = BusStopHelper.orderBusStopPairs(routeOptions,userLoc,destinationLocation);

                //listOfPaths = getPathTest(routeOptions);
                listOfPaths = getPath(routeOptions);

                //TODO routeoptions into list of busStops or something else (vertex etc.) pass something that has all info to render it

                //return the data
                outputData = new NearestStopForDestinationOutputData(destinationLocation,listOfPaths);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return outputData;
    }

    private <T> ArrayList<T> intersection(ArrayList<T> list1, ArrayList<T> list2) {
        ArrayList<T> list = new ArrayList<T>();

        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }

    @Override
    protected void onPostExecute(NearestStopForDestinationOutputData outputData)
    {
        super.onPostExecute(outputData);

        try {
            delegate.FindNearestStopAnyRoute_ForDestination_ProcessFinish(outputData);
        }
        catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Vertex2> nodes;
    //private List<Edge> edges;
    int maxServicesToProcess = 0;
    private int userNodeIndx = 0;
    private int destinationNodeIndx = -1;
    float walkingMultiplier = 5f;
    //StopWatch stopWatch = new StopWatch();

    //public com.example.combiningprojects.FindShortestPathForDestination.AsyncResponse delegate = null;

    protected List<List<Vertex2>> CalculateRouteMultipleBuses(Location userLoc, Location destinationLocation)
    {
        List<List<Vertex2>> listOfPaths = new ArrayList<>();
		/*
        Location eastCork = new Location("EastCork");
        eastCork.setLatitude(51.896214);
        eastCork.setLongitude(-8.4321745);

        Location westCork = new Location("WestCork");
        westCork.setLatitude(51.8891272);
        westCork.setLongitude(-8.5334355);

        float refDistance = eastCork.distanceTo(westCork);

        double distance1 = distanceLatLng1(eastCork.getLatitude(), eastCork.getLongitude(), westCork.getLatitude(), westCork.getLongitude());
        double distance2 = distanceLatLng2(eastCork.getLatitude(), eastCork.getLongitude(), westCork.getLatitude(), westCork.getLongitude());
        double distance3 = distanceLatLng3(eastCork.getLatitude(), eastCork.getLongitude(), westCork.getLatitude(), westCork.getLongitude());
		*/

            /*
            //////////////////////////////////////////////////////////////////
            //                                                             //
            //      CODE FOR SHOWING GRAPH IS DIRECTIONAL                  //
            //                                                             //
            /////////////////////////////////////////////////////////////////

            nodes = new ArrayList<Vertex2>();
            edges = new ArrayList<Edge>();

            nodes.add(new Vertex2(0, "Node_0", null));
            nodes.add(new Vertex2(1, "Node_1", null));
            nodes.add(new Vertex2(2, "Node_2", null));
            nodes.add(new Vertex2(3, "Node_3", null));

            //Source node -> destination node with a cost
            addLane("Edge_0", 0, 1, 100, false);
            addLane("Edge_1", 1, 2, 100, false);
            addLane("Edge_2", 2, 3, 100, false);
            addLane("Edge_0", 3, 2, 100, false);
            addLane("Edge_1", 2, 1, 100, false);
            addLane("Edge_2", 1, 0, 100, false);

            // Lets check from userNodeIndx location to a specific node
            graph = new Graph(nodes, edges);
            DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);

            //dijkstra.execute(nodes.get(0));
            //LinkedList<Vertex2> path = dijkstra.getPath(nodes.get(3));

            dijkstra.execute(nodes.get(3));
            LinkedList<Vertex2> path = dijkstra.getPath(nodes.get(0));

            assertNotNull(path);
            assertTrue(path.size() > 0);

            for (Vertex2 vertex : path)
            {
                System.out.println(vertex);
                BusStopDescriptor descriptor = vertex.getDescriptor();
                if(descriptor != null){
                    BusStop busStop = BusServiceMap.GetStopFromDescriptor(descriptor);
                    System.out.println("Bus Stop name:"+busStop.getName() + " bus stop distance from userNodeIndx: " + busStop.getDistanceFromUserLocation(userLoc)+
                            "Public service code for this stop is : " + busStop.getPublicServiceCode());
                }
            }
            return path;
*/

        //stopWatch.StartTimeInterval();
        //Lets now find the X nearestStops to the users location
        BusStopDescriptor[] crowFliesNearestBusStopToDestination;
        crowFliesNearestBusStopToDestination = BusStopHelper.crowFliesNearestBusStops(1, destinationLocation);
        int nearestStopToDestinationNodeIndex = -1;
        //stopWatch.StopTimeInterval("!!! Time spent crowFliesNearestBusStopToDestination");

        // Every bus stop must be loaded into graph of bustopsDescriptors at the start. 0 being the userNodeIndx
        nodes = new ArrayList<Vertex2>();
        //edges = new ArrayList<Edge>();
        int count = 0;
        Vertex2 location;
        //add the userNodeIndx node
        location = new Vertex2("UserNode_" + count, null);
        nodes.add(location);
        System.out.println("Actual UserLocation: " + userLoc);
        count++;
        //maxServicesToProcess = 5;
        maxServicesToProcess = BusServiceMap.hashMap.size();


        //Start the timer
        //stopWatch.StartTimeInterval();

        //Loop over the whole hashmap of stops
        for (int i = 0; i < maxServicesToProcess; i++)
        {
            //Grab the service code to grab the stops from.
            String currentServiceCode = PopulateBusStopsNewAsync.busRoutes[i];
            if (BusServiceMap.hashMap.isEmpty()) {
                break;
            }
            //Loop over the routes for that service
            for (int k = 0; k < BusServiceMap.hashMap.get(currentServiceCode).routes.size(); ++k) {
                //Loop over the stops for that service
                for (int p = 0; p < BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.size(); ++p) {
                    //Grab the bus stop that we are on
                    BusStop theBusStop = BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.get(p);
                    //Create a descriptor from the stop
                    BusStopDescriptor descriptorToAdd = new BusStopDescriptor(currentServiceCode, k, p, theBusStop.getGlobalStopID());
                    //Add the descriptor as a vertex
                    location = new Vertex2("Node_" + count, descriptorToAdd);
                    nodes.add(location);

                    if(crowFliesNearestBusStopToDestination[0].globalStopID == theBusStop.getGlobalStopID())
                    {
                        nearestStopToDestinationNodeIndex = count;
                    }

                    count++;
                }
            }
        }


        System.out.println("Node Count is: " + nodes.size());

        //stopWatch.StopTimeInterval("Time spent adding vertices");


        //Start the timer
        //stopWatch.StartTimeInterval();

        //Add edge to all the bus stops on the same route to each other (Driving)
        int edgeCounter = 1;
        for (int i = 0; i < maxServicesToProcess; ++i) {
            String currentServiceCode = PopulateBusStopsNewAsync.busRoutes[i];
            if (BusServiceMap.hashMap.isEmpty()) {
                break;
            }

            // IGNORE THIS
            //boolean inFilterOutList = filterList.contains(currentServiceCode);

            for (int k = 0; k < BusServiceMap.hashMap.get(currentServiceCode).routes.size(); ++k) {

                for (int p = 0; p < (BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.size()) - 1; ++p) {
                    //Grab the two neighbouring stops
                    BusStop firstBusStop = BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.get(p);
                    BusStop secondBusStop = null;

                    float distanceBetweenStops;
                    //Set the lat and lng of the first stop
                    Location firstLocationBusStop = new Location("First stop");
                    firstLocationBusStop.setLatitude(firstBusStop.getLat());
                    firstLocationBusStop.setLongitude(firstBusStop.getLng());

                    Location secondLocationBusStop = null;
                    //If there is in another bus stop to the right, add it as a neighbour
                    //if(p != BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.size()-1)
                    //{
                    secondBusStop = BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.get(p + 1);
                    //Set the lat and lng of the second stop
                    secondLocationBusStop = new Location("Second stop");
                    secondLocationBusStop.setLatitude(secondBusStop.getLat());
                    secondLocationBusStop.setLongitude(secondBusStop.getLng());

                    //TODO MAKE THIS TIME INSTEAD OF CROW FLIES
                    //Get as the crow flies distance between the two stops
                    distanceBetweenStops = firstLocationBusStop.distanceTo(secondLocationBusStop);
/*
                        BusStopDescriptor firstStopDescriptor = new BusStopDescriptor(firstBusStop.getPublicServiceCode(), k, p, firstBusStop.getGlobalStopID());
                        Vertex2 firstStopNode = nodes.get(edgeCounter+p);
                        assertTrue (firstStopDescriptor.equals(firstStopNode.getDescriptor()));

                        BusStopDescriptor secondStopDescriptor = new BusStopDescriptor(secondBusStop.getPublicServiceCode(), k, p+1, secondBusStop.getGlobalStopID());
                        Vertex2 secondStopNode = nodes.get(edgeCounter+p+1);
                        assertTrue (secondStopDescriptor.equals(secondStopNode.getDescriptor()));
*/
                    // IGNORE THIS
                    //Add the edge between the two nodes
                    //if(!inFilterOutList)
                    {
                        addLane(currentServiceCode + p, edgeCounter + p, edgeCounter + p + 1, distanceBetweenStops, false);
                    }
                    //}
                }
                edgeCounter += BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.size();
            }
        }

        //stopWatch.StopTimeInterval("Time spent adding consecutive driving edges");

        //stopWatch.StartTimeInterval();
        //Link userNodeIndx to nodes within walkable distance
        LinkNodeToServiceStops(userNodeIndx,userLoc,0,false,false);
        //TODO make sure there is always a connection like the destination
        //stopWatch.StopTimeInterval("Time spent connecting userNodeIndx node");

        //Start the timer
        //stopWatch.StartTimeInterval();

        if(allowMultipleBuses)
        {
            int numRoutes;
            int numStops;
            int equivalentNodeIndx = 1;
            Location currentStopLocation = new Location("currentStop");
            String currentServiceCode;
            BusStop stopTogetDistanceTo;

            BusService currentService;
            BusService.Route currentRoute;

            for (int i = 0; i < maxServicesToProcess; ++i)
            {
                currentServiceCode = PopulateBusStopsNewAsync.busRoutes[i];

                currentService = BusServiceMap.hashMap.get(currentServiceCode);

                numRoutes = currentService.routes.size();

                for (int k = 0; k < numRoutes; ++k)
                {
                    currentRoute = currentService.routes.get(k);

                    numStops = currentRoute.stops.size();

                    for (int p = 0; p < numStops; ++p)
                    {
                        //stopTogetDistanceTo = BusServiceMap.GetStopFromDescriptor(nodes.get(equivalentNodeIndx).getDescriptor());

                        stopTogetDistanceTo = currentRoute.stopsBasicArray[p];

                        currentStopLocation.setLatitude(stopTogetDistanceTo.getLat());
                        currentStopLocation.setLongitude(stopTogetDistanceTo.getLng());

                        LinkNodeToServiceStops(equivalentNodeIndx,currentStopLocation,i+1,true,false);

                        //BusStopDescriptor descriptor = new BusStopDescriptor(currentBusStop.getPublicServiceCode(), k, p, currentBusStop.getGlobalStopID());
                        //Vertex2 currentNode = nodes.get(equivalentNodeIndx);
                        //assertTrue (descriptor.equals(currentNode.getDescriptor()));

                        //Keep this last
                        ++equivalentNodeIndx;
                    }
                }
            }
        }


        //stopWatch.StopTimeInterval("Time spent adding walking edges between stops on different services");
        System.out.println("PairCounter: " + pairCounter);
        System.out.println("Successful Pairs: " + successfulPairCounter);

        //add the destination node
        location = new Vertex2("DestinationNode_" + count, null);
        nodes.add(location);
        System.out.println("DestinationLocation: " + destinationLocation);
        destinationNodeIndx = nodes.size()-1;

        //stopWatch.StartTimeInterval();
        //Link destinationNodeIndx to nodes within walkable distance
        int connectionsMadeToDestination = LinkNodeToServiceStops(destinationNodeIndx,destinationLocation,0,false,true);

        //always make sure there is a connection
        if(connectionsMadeToDestination == 0)
        {
            addLane("onlyDestinationLink",nearestStopToDestinationNodeIndex,destinationNodeIndx,0,true);
            CustomLogger.println2("Added special connection as we had none to the destination");
        }

        addLane("directWalk", userNodeIndx, destinationNodeIndx, userLoc.distanceTo(destinationLocation)*walkingMultiplier, true);

        //stopWatch.StopTimeInterval("Time spent connecting destination node");

        // *** OPTIMAL PATH CALCULATION ***
        //make array workable
        //stopWatch.StartTimeInterval();
        copyArrayToWorkOn();
        //stopWatch.StopTimeInterval("Time spent in copyArrayToWorkOn()");
        // CALCULATE AND ADD TO LIST!!!!
        //stopWatch.StartTimeInterval();
        listOfPaths.add(getPath());
        //stopWatch.StopTimeInterval("Time spent in getPath()");

        // *** ALTERNATE PATH 1 CALCULATION ***
        //now remove the service code from the list
        //stopWatch.StartTimeInterval();
        detachServiceCodeFromList(listOfPaths, 0);
        //stopWatch.StopTimeInterval("Time spent in detachServiceCodeFromList()");
        //make array workable
        //stopWatch.StartTimeInterval();
        copyArrayToWorkOn();
        //stopWatch.StopTimeInterval("Time spent in copyArrayToWorkOn()");
        //reset the min distance on all nodes to infinity
        //stopWatch.StartTimeInterval();
        resetNodesMinDistance();
        //stopWatch.StopTimeInterval("Time spent in resetNodesMinDistance()");
        // CALCULATE AND ADD TO LIST!!!!
        //stopWatch.StartTimeInterval();
        listOfPaths.add(getPath());
        //stopWatch.StopTimeInterval("Time spent in getPath()");

        // *** ALTERNATE PATH 2 CALCULATION ***
        //now remove the service code from the list
        //stopWatch.StartTimeInterval();
        detachServiceCodeFromList(listOfPaths, 1);
        //stopWatch.StopTimeInterval("Time spent in detachServiceCodeFromList()");
        //make array workable
        //stopWatch.StartTimeInterval();
        copyArrayToWorkOn();
        //stopWatch.StopTimeInterval("Time spent in copyArrayToWorkOn()");
        //reset the min distance on all nodes to infinity
        //stopWatch.StartTimeInterval();
        resetNodesMinDistance();
        //stopWatch.StopTimeInterval("Time spent in resetNodesMinDistance()");
        //get alt path 2
        //stopWatch.StartTimeInterval();
        listOfPaths.add(getPath());
        //stopWatch.StopTimeInterval("Time spent in getPath()");


        // Populate filterList with the bus service to unhook/disallow
        // Need to grab the Node array indices fore the service you're interested in.
        // See the code below for "//Increase the equivalentNodeIndx by the number of stops we skip" in LinkNodeToServiceStops()
        // You know the order the stops were added to the node list, so you can revisit them.
        // Or for a hack, loop over all the nodes and check the descriptors for the service code to filter out ;)
        // You can;t completely clear/delete the entire adjacency list. You need to selectively remove the consecutive driving connections ONLY.
        // You'll be able to figure this out by looking at the source and destination nodes and seeing if they are the same service code, this means walking connections will remain.

        // Re-run Dijkstras with the now modified lanes and hey presto, new path geenrated.

        // This should return a list of paths
        return listOfPaths;
    }

    public void resetNodesMinDistance()
    {
        //make all ndoes min distance infinity again
        for(int i = 0; i < nodes.size(); ++i)
        {
            nodes.get(i).minDistance = Double.POSITIVE_INFINITY;
        }
    }

    public List<Vertex2> getPath()
    {
        List<Vertex2> path;

        //stopWatch.StartTimeInterval();
        Dijkstra.computePaths(nodes.get(userNodeIndx));
        //stopWatch.StopTimeInterval("Time spent in dijkstra code computePaths() block was");

        //stopWatch.StartTimeInterval();
        path = Dijkstra.getShortestPathTo(nodes.get(destinationNodeIndx));
        //stopWatch.StopTimeInterval("Time spent in dijkstra code getShortestPathTo() block was");

        assertNotNull(path);
        assertTrue(path.size() > 0);

        /*
        for (Vertex2 vertex : path)
        {
            System.out.print(vertex);
            BusStopDescriptor descriptor = vertex.getDescriptor();
            if(descriptor != null){
                BusStop busStop = BusServiceMap.GetStopFromDescriptor(descriptor);
                System.out.print(" - Bus Stop name: "+busStop.getName() + " bus stop distance from userNodeIndx: " + busStop.getDistanceFromUserLocation(userLoc)+
                        " Public service code for this stop is : " + busStop.getPublicServiceCode()+"\n");
            }
            else
            {
                System.out.print("\n");
            }
        }
        */


        return path;
    }

    public void copyArrayToWorkOn()
    {
        for(int x = 0; x < nodes.size(); ++x)
        {
            // Copy adjacenciesPublic in adjacencies so it can be worked on
            nodes.get(x).adjacencies = new Edge2[nodes.get(x).adjacenciesPublic.size()];
            nodes.get(x).adjacenciesPublic.toArray(nodes.get(x).adjacencies); // fill the array
        }
    }

    private void detachServiceCodeFromList(List<List<Vertex2>> listOfPaths, int indexToRemove)
    {
        //use to skip the stops we dont need
        int equivalentNodeIndx = 1;

        //the service to detach
        String serviceToDetach = listOfPaths.get(indexToRemove).get(1).getDescriptor().GetServiceCode();

        //Increase the equivalentNodeIndx by the number of stops we skip
        int numRoutes;
        String currentServiceCode;
        for(int p = 0; p < BusServiceMap.hashMap.size(); ++p)
        {
            currentServiceCode = PopulateBusStopsNewAsync.busRoutes[p];

            if(currentServiceCode == serviceToDetach)
            {
                //if the first service found is the one to detach then break the equivalent node index is 1
                break;
            }
            else
            {
                //we need to keep looking for a start position
                numRoutes = BusServiceMap.hashMap.get(currentServiceCode).routes.size();
                for (int k = 0; k < numRoutes; ++k)
                {
                    equivalentNodeIndx += BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.size();
                }
            }
        }

        //Grab the first node we want to disconnect
        Vertex2 currentVertex;// = nodes.get(equivalentNodeIndx);
        Vertex2 nextVertex;
        Edge2 edge;

        //start from the node we need and loop to the last vertex of that service code
        for(int i = equivalentNodeIndx; i < (nodes.size() - 1); ++i)
        {
            currentVertex = nodes.get(i);
            nextVertex = nodes.get(i + 1);
            if(nextVertex.getDescriptor().GetServiceCode() != serviceToDetach)
            {
                // Last stop of a service route isn't connected to any more stops on that route, so we don't need to do anything
                break;
            }
            /*
            //cut the neighbours for driving only
            for(int p = 0; p < currentVertex.adjacenciesPublic.size(); ++p)
            {
                Edge2 edge = nodes.get(equivalentNodeIndx).adjacenciesPublic.get(p);
                if(edge.target.getDescriptor().GetServiceCode() == currentVertex.getDescriptor().GetServiceCode())
                {
                    currentVertex.adjacenciesPublic.remove(p);
                }
            }
            */

            if(currentVertex.adjacenciesPublic.size() > 0)
            {
                edge = currentVertex.adjacenciesPublic.get(0);
                //System.out.println("Removing connection from " + currentVertex.getDescriptor().toString() + " --> " + edge.target.getDescriptor().toString());
                //remove the first link
                currentVertex.adjacenciesPublic.remove(0);
            }
        }
    }

    int pairCounter = 0;
    int successfulPairCounter = 0;
    float maxWalkTimeForConnect = 2000;
    private int LinkNodeToServiceStops(int nodeToLinkIdx, Location nodeToLinkLocation, int startServiceIdx, boolean addReverseLane, Boolean invertLanes)
    {
        int lanesMade = 0;
        Vertex2 nodeToLink = nodes.get(nodeToLinkIdx);

        int equivalentNodeIndx = 1;
        //Increase the equivalentNodeIndx by the number of stops we skip
        int numRoutes;
        String currentServiceCode;
        for(int p = 0; p < startServiceIdx; ++p)
        {
            currentServiceCode = PopulateBusStopsNewAsync.busRoutes[p];
            numRoutes = BusServiceMap.hashMap.get(currentServiceCode).routes.size();
            for (int k = 0; k < numRoutes; ++k)
            {
                equivalentNodeIndx += BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.size();
            }
        }

        int numStops;
        Vertex2 possibleLinkableNode;
        //Location currentStopLocation = new Location("currentStop");
        float multiplierToUse = walkingMultiplier;
        Boolean bothBusStops;
        float timeToWalk;
        BusService currentService;
        BusStop stopTogetDistanceTo;
        BusService.Route currentRoute;

        /*
        int errorCounter = 0;
        float error1 = 0f;
        float error2 = 0f;
        float error3 = 0f;
        */

        //loop over all services
        for (int i = startServiceIdx; i < maxServicesToProcess; ++i)
        {
            currentServiceCode = PopulateBusStopsNewAsync.busRoutes[i];

            currentService = BusServiceMap.hashMap.get(currentServiceCode);

            //loop over the routes
            numRoutes = currentService.routes.size();

            for (int k = 0; k < numRoutes; ++k)
            {
                currentRoute = currentService.routes.get(k);

                //loop over all the stops
                numStops = currentRoute.stops.size();

                for (int p = 0; p < numStops; ++p)
                {
                    possibleLinkableNode = nodes.get(equivalentNodeIndx);

                    //BusStop stopTogetDistanceTo = BusServiceMap.GetStopFromDescriptor(possibleLinkableNode.getDescriptor());
                    stopTogetDistanceTo = currentRoute.stopsBasicArray[p];

                    //currentStopLocation.setLatitude(stopTogetDistanceTo.getLat());
                    //currentStopLocation.setLongitude(stopTogetDistanceTo.getLng());

                    //Got two bus stops
                    if(nodeToLink.getDescriptor() != null && possibleLinkableNode.getDescriptor() != null)
                    {
                        bothBusStops = true;
                    }
                    else
                    {
                        bothBusStops = false;
                    }

                    //float timeToWalkOld = nodeToLinkLocation.distanceTo(currentStopLocation) * multiplierToUse;

                    /*
                    timeToWalk = (float)distanceLatLng1(nodeToLinkLocation.getLatitude(), nodeToLinkLocation.getLongitude(), stopTogetDistanceTo.getLat(), stopTogetDistanceTo.getLng());
                    timeToWalk *= multiplierToUse;
                    error1 += (timeToWalkOld - timeToWalk);
                    */


                    timeToWalk = (float)distanceLatLng2(nodeToLinkLocation.getLatitude(), nodeToLinkLocation.getLongitude(), stopTogetDistanceTo.getLat(), stopTogetDistanceTo.getLng());
                    timeToWalk *= multiplierToUse;
                    //error2 += (timeToWalkOld - timeToWalk);

                    /*
                    timeToWalk = (float)distanceLatLng3(nodeToLinkLocation.getLatitude(), nodeToLinkLocation.getLongitude(), stopTogetDistanceTo.getLat(), stopTogetDistanceTo.getLng());
                    timeToWalk *= multiplierToUse;
                    error3 += (timeToWalkOld - timeToWalk);

                    timeToWalk = timeToWalkOld;
                    */

                    //Location.distanceBetween(nodeToLinkLocation.getLatitude(), nodeToLinkLocation.getLongitude(), stopTogetDistanceTo.getLat(), stopTogetDistanceTo.getLng(), timeToWalk2);
                    //timeToWalk2[0] *= multiplierToUse;
                    //timeToWalk = timeToWalk2[0];
                    //error2 += (timeToWalkOld - timeToWalk2[0]);

                    //++errorCounter;

                    if (timeToWalk <= (maxWalkTimeForConnect * multiplierToUse))
                    {
                        if(bothBusStops)
                        {
                            if(nodeToLink.getDescriptor().GetServiceCode() != possibleLinkableNode.getDescriptor().GetServiceCode())
                            {
                                //add fixed amount given that they will be switching services and have to wait for bus
                                timeToWalk += 1300; //equates to 5minutes with bus travelling at 16km/h
                            }
                        }

                        //if(successfulPairCounter < 5000)
                        //{
                        if(invertLanes)
                        {
							// Inline code is quite a bit faster here!
                            //addLane("walkingEdge_" + equivalentNodeIndx, equivalentNodeIndx, nodeToLinkIdx, timeToWalk, true);
                            nodes.get(equivalentNodeIndx).adjacenciesPublic.add(new Edge2(nodes.get(nodeToLinkIdx), timeToWalk));
                            ++lanesMade;
                            if(addReverseLane)
                            {
								// Inline code is quite a bit faster here!
                                //addLane("walkingEdge_" + equivalentNodeIndx, nodeToLinkIdx, equivalentNodeIndx, timeToWalk, true);
                                nodes.get(nodeToLinkIdx).adjacenciesPublic.add(new Edge2(nodes.get(equivalentNodeIndx), timeToWalk));
                                ++lanesMade;
                            }
                        }
                        else
                        {
							// Inline code is quite a bit faster here!
                            //addLane("walkingEdge_" + equivalentNodeIndx, nodeToLinkIdx, equivalentNodeIndx, timeToWalk, true);
                            nodes.get(nodeToLinkIdx).adjacenciesPublic.add(new Edge2(nodes.get(equivalentNodeIndx), timeToWalk));
                            ++lanesMade;
                            if(addReverseLane)
                            {
								// Inline code is quite a bit faster here!
                                //addLane("walkingEdge_" + equivalentNodeIndx, equivalentNodeIndx, nodeToLinkIdx, timeToWalk, true);
                                nodes.get(equivalentNodeIndx).adjacenciesPublic.add(new Edge2(nodes.get(nodeToLinkIdx), timeToWalk));
                                ++lanesMade;
                            }
                        }
                        ++successfulPairCounter;
                        //}
                        //BusStop currentBusStop = BusServiceMap.hashMap.get(currentServiceCode).routes.get(k).stops.get(p);
                        //BusStopDescriptor descriptor = new BusStopDescriptor(currentBusStop.getPublicServiceCode(), k, p, currentBusStop.getGlobalStopID());
                        //Vertex2 currentNode = nodes.get(equivalentNodeIndx);
                        //assertTrue (descriptor.equals(currentNode.getDescriptor()));
                    }
                    //Keep this last
                    ++pairCounter;
                    ++equivalentNodeIndx;
                }
            }
        }

        /*
        error1 /= errorCounter;
        error2 /= errorCounter;
        error3 /= errorCounter;

        CustomLogger.println2("Error1: " + error1);
        CustomLogger.println2("Error2: " + error2);
        CustomLogger.println2("Error3: " + error3);
        */

        return lanesMade;
    }

    // http://jonisalonen.com/2014/computing-distance-between-coordinates-can-be-simple-and-fast/
    // ϕ  is latitude, λ longitude
    // Modified Euclidean Distance
    double distanceLatLng1(double lat1, double lng1, double lat2, double lng2)
    {
        //double deglen = 110.25; // KM
        double deglen = 110250; // Metre
        double x = lat1 - lat2;
        double y = (lng1 - lng2) * Math.cos(Math.toRadians(lat2));
        return (deglen * Math.sqrt(x * x + y * y));
    }

    // http://www.movable-type.co.uk/scripts/latlong.html
    // Equirectangular approximation
    double distanceLatLng2(double lat1, double lng1, double lat2, double lng2)
    {
        double lat1Rads = Math.toRadians(lat1);
        double lng1Rads = Math.toRadians(lng1);
        double lat2Rads = Math.toRadians(lat2);
        double lng2Rads = Math.toRadians(lng2);

        int R = 6371000; // metres
        double x = (lng2Rads - lng1Rads) * Math.cos((lat1Rads + lat2Rads) / 2);
        double y = (lat2Rads - lat1Rads);
        return Math.sqrt(x * x + y * y) * R;
    }

    // http://www.movable-type.co.uk/scripts/latlong.html
    // Law of cosines
    double distanceLatLng3(double lat1, double lng1, double lat2, double lng2)
    {
        double lat1Rads = Math.toRadians(lat1);
        double lat2Rads = Math.toRadians(lat2);
        double deltaLong = Math.toRadians(lng2-lng1);
        double R = 6371e3; // gives d in metres
        double distance = Math.acos( Math.sin(lat1Rads)*Math.sin(lat2Rads) + Math.cos(lat1Rads)*Math.cos(lat2Rads) * Math.cos(deltaLong) ) * R;

        if(Double.isNaN(distance))
        {
            // This happens if the two lats and 2 longs are the same, flaoting poitn error
            // can make Math.acos evaluate soemthignt aht is beyond 1.0 which casues the NaN.
            // I saw this with a 1.02 value.
            distance = 0;
        }

        return distance;
    }

    //Could add a bool here to let us know if its a walking edge or not?
    private void addLane(String laneId, int sourceLocNo, int destLocNo,
                         float duration, boolean walkingEdge)
    {
        Vertex2 sourceVertex = nodes.get(sourceLocNo);
        Vertex2 destnVertex = nodes.get(destLocNo);

        //Edge lane = new Edge(laneId,sourceVertex, destnVertex, duration, walkingEdge);
        //edges.add(lane);

        sourceVertex.adjacenciesPublic.add(new Edge2(destnVertex, duration));


        BusStopDescriptor sourceDescriptor = sourceVertex.getDescriptor();
        BusStopDescriptor dstnDescriptor = destnVertex.getDescriptor();

/*
        //System.out.println( BusServiceMap.GetStopFromDescriptor(sourceDescriptor).getName()+ " -> " + BusServiceMap.GetStopFromDescriptor(dstnDescriptor).getName());
        //its a bus stop
        if(sourceDescriptor != null && dstnDescriptor != null)
        {
            CustomLogger.println2(sourceDescriptor.toString() + "(" + sourceLocNo + ") -> " + dstnDescriptor.toString() + "(" + destLocNo + ")" + " walking: " + walkingEdge + " Cost: " + duration);
        }
        else if(sourceDescriptor == null && dstnDescriptor == null)
        {
            CustomLogger.println2(sourceVertex.name + " (" + sourceLocNo + ") -> " + destnVertex.name + " (" + destLocNo + ")" + " walking: " + walkingEdge + " Cost: " + duration);
        }
        else if(sourceDescriptor == null)
        {
            CustomLogger.println2(sourceVertex.name + " (" + sourceLocNo + ") -> " + dstnDescriptor.toString() + "(" + destLocNo + ")" + " walking: " + walkingEdge + " Cost: " + duration);
        }
        else
        {
            CustomLogger.println2(sourceDescriptor.toString() + " (" + sourceLocNo + ") -> " + destnVertex.name + "(" + destLocNo + ")" + " walking: " + walkingEdge + " Cost: " + duration);
        }
*/
    }
/*
    public LinkedList<Vertex2> getPath(List<BusStopPair> routeOptions)
    {
        LinkedList<Vertex2> path = new LinkedList<>();
        String currentServiceCode = routeOptions.get(0).GetOriginStop().serviceCode;
        int routeIndex = routeOptions.get(0).GetOriginStop().GetRouteIndex();
        int originIndex = routeOptions.get(0).GetOriginStop().stopIndex;

        //Add the user location
        Vertex2 location;
        //add the userNodeIndx node
        location = new Vertex2(0, "Node_"+ 0,null);
        path.add(location);

        for(int i = 0; i < 1; ++i)
        {
            int stopIndex = routeOptions.get(i).GetDestinationStop().GetStopIndex();
            for(int p = originIndex; p <= stopIndex; ++p)
            {
                BusStop theBusStop = BusServiceMap.hashMap.get(currentServiceCode).routes.get(routeIndex).stops.get(originIndex);
                //Create a descriptor from the stop
                BusStopDescriptor descriptorToAdd = new BusStopDescriptor(currentServiceCode, routeIndex, originIndex, theBusStop.getGlobalStopID());

                //add the userNodeIndx node
                location = new Vertex2(p, "Node_"+ p,descriptorToAdd);
                path.add(location);
                originIndex++;
            }
        }

        //add the desintation location
        //add the destination location node
        location = new Vertex2(originIndex, "Node_"+ originIndex,null);
        path.add(location);

        return path;
    }
*/
    // Used for Step 2 to modify data structure to that used for step 3
    public List<List<Vertex2>> getPath(List<BusStopPair> routeOptions)
    {
        List<List<Vertex2>> listOfPaths = new ArrayList<>();
        List<Vertex2> path;

        String currentServiceCode;
        int routeIndex;
        int originIndex;
        int stopIndex;

        for(int i = 0; i < routeOptions.size(); ++i)
        {
            //Create a new path to add
            path = new ArrayList<>();
            //Add the user location
            Vertex2 location;
            location = new Vertex2("Node_"+ 0,null);
            path.add(location);
            //Initialize indexs
            currentServiceCode = routeOptions.get(i).GetOriginStop().serviceCode;
            stopIndex = routeOptions.get(i).GetDestinationStop().GetStopIndex();
            routeIndex = routeOptions.get(i).GetOriginStop().GetRouteIndex();
            originIndex = routeOptions.get(i).GetOriginStop().stopIndex;

            for(int p = originIndex; p <= stopIndex; ++p)
            {
                BusStop theBusStop = BusServiceMap.hashMap.get(currentServiceCode).routes.get(routeIndex).stops.get(originIndex);
                //Create a descriptor from the stop
                BusStopDescriptor descriptorToAdd = new BusStopDescriptor(currentServiceCode, routeIndex, originIndex, theBusStop.getGlobalStopID());

                //add the userNodeIndx node
                location = new Vertex2("Node_"+ p,descriptorToAdd);
                path.add(location);
                originIndex++;
            }
            //add the desintation location
            location = new Vertex2("Node_"+ originIndex,null);
            path.add(location);
            //add to the list
            listOfPaths.add(i,path);
        }

        return listOfPaths;
    }
}




