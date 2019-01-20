package com.example.combiningprojects;

/**
 * Created by brend on 04/04/2017.
 */

import java.util.PriorityQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

class Vertex2 implements Comparable<Vertex2>
{
    public final String name;
    private BusStopDescriptor descriptor;
    public ArrayList<Edge2> adjacenciesPublic = new ArrayList<>();
    public Edge2[] adjacencies;
    public double minDistance = Double.POSITIVE_INFINITY;
    public Vertex2 previous;
    public Vertex2(String argName, BusStopDescriptor _descriptor) { name = argName;descriptor = _descriptor;}
    public String toString() { return name; }
    @Override
    public int compareTo(Vertex2 other)
    {
        return Double.compare(minDistance, other.minDistance);
    }

    public BusStopDescriptor getDescriptor() {
        return descriptor;
    }
}

class Edge2
{
    public final Vertex2 target;
    public final double weight;
    public Edge2(Vertex2 argTarget, double argWeight)
    { target = argTarget; weight = argWeight; }
}

public class Dijkstra
{
    public static void computePaths(Vertex2 source)
    {
        source.minDistance = 0.;
        PriorityQueue<Vertex2> vertexQueue = new PriorityQueue<Vertex2>();
        vertexQueue.add(source);

        while (!vertexQueue.isEmpty()) {
            Vertex2 u = vertexQueue.poll();

            // Visit each edge exiting u
            for (Edge2 e : u.adjacencies)
            {
                Vertex2 v = e.target;
                double weight = e.weight;
                double distanceThroughU = u.minDistance + weight;
                if (distanceThroughU < v.minDistance) {
                    vertexQueue.remove(v);

                    v.minDistance = distanceThroughU ;
                    v.previous = u;
                    vertexQueue.add(v);
                }
            }
        }
    }

    public static List getShortestPathTo(Vertex2 target)
    {
        List path = new ArrayList();
        for (Vertex2 vertex = target; vertex != null; vertex = vertex.previous)
            path.add(vertex);

        Collections.reverse(path);
        return path;
    }

    public static void main(String[] args)
    {
        // mark all the vertices
        Vertex2 A = new Vertex2("A", null);
        Vertex2 B = new Vertex2("B", null);
        Vertex2 D = new Vertex2("D", null);
        Vertex2 F = new Vertex2("F", null);
        Vertex2 K = new Vertex2("K", null);
        Vertex2 J = new Vertex2("J", null);
        Vertex2 M = new Vertex2("M", null);
        Vertex2 O = new Vertex2("O", null);
        Vertex2 P = new Vertex2("P", null);
        Vertex2 R = new Vertex2("R", null);
        Vertex2 Z = new Vertex2("Z", null);

        // set the edges and weight
        A.adjacencies = new Edge2[]{ new Edge2(M, 8) };
        B.adjacencies = new Edge2[]{ new Edge2(D, 11) };
        D.adjacencies = new Edge2[]{ new Edge2(B, 11) };
        F.adjacencies = new Edge2[]{ new Edge2(K, 23) };
        K.adjacencies = new Edge2[]{ new Edge2(O, 40) };
        J.adjacencies = new Edge2[]{ new Edge2(K, 25) };
        M.adjacencies = new Edge2[]{ new Edge2(R, 8) };
        O.adjacencies = new Edge2[]{ new Edge2(K, 40) };
        P.adjacencies = new Edge2[]{ new Edge2(Z, 18) };
        R.adjacencies = new Edge2[]{ new Edge2(P, 15) };
        Z.adjacencies = new Edge2[]{ new Edge2(P, 18) };


        computePaths(A); // run Dijkstra
        System.out.println("Distance to " + Z + ": " + Z.minDistance);
        List path = getShortestPathTo(Z);
        System.out.println("Path: " + path);
    }
}