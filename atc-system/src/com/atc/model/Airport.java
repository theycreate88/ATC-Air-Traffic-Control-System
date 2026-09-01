package com.atc.model;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;


public class Airport {

    private final String name;
    private final int x;
    private final int y;

    private final Queue<Flight> departureQueue = new LinkedList<>();
    private final PriorityQueue<Flight> runwayQueue = new PriorityQueue<>(new RunwayComparator());

    public Airport(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Queue<Flight> getDepartureQueue() {
        return departureQueue;
    }

    public PriorityQueue<Flight> getRunwayQueue() {
        return runwayQueue;
    }

 
    public void requestDeparture(Flight f) {
        f.setLandingRequest(false);
        departureQueue.offer(f);
        runwayQueue.offer(f);
    }

    
    public void requestLanding(Flight f) {
        f.setLandingRequest(true);
        runwayQueue.offer(f);
    }
 
    public Flight pollRunway() {
        return runwayQueue.poll();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Airport)) return false;
        return name.equals(((Airport) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }


    private static class RunwayComparator implements Comparator<Flight> {
        @Override
        public int compare(Flight a, Flight b) {
            int pa = priorityOf(a);
            int pb = priorityOf(b);
            if (pa != pb) {
                return Integer.compare(pa, pb);
            }
 
            long ta = a.isLandingRequest() ? a.getExpectedArrivalTime() : a.getDepartureTime();
            long tb = b.isLandingRequest() ? b.getExpectedArrivalTime() : b.getDepartureTime();
            return Long.compare(ta, tb);
        }

        private int priorityOf(Flight f) {
            boolean emergency = "Emergency".equals(f.getStatus());
            if (f.isLandingRequest()) {
                return emergency ? 0 : 1;
            }
            return 2;
        }
    }
}
