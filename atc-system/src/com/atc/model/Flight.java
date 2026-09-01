package com.atc.model;

import java.util.List;

 
public class Flight {

    public static final String STATUS_SCHEDULED = "Scheduled";
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_EMERGENCY = "Emergency";
    public static final String STATUS_LANDED = "Landed";

    private final String flightId;
    private final Airport departure;
    private Airport destination;
    private List<Airport> routePath;
    private int currentRouteIndex;
    private String status;
    private final long departureTime;
    private long expectedArrivalTime;
    private double currentX;
    private double currentY;
    private boolean hasWeatherEmergency;
    private int altitude;

   
    private double legProgress;     
    private boolean landingRequest;  
    private String emergencyType;    
    private long lastConflictAtMillis;  
    private double speedMultiplier = 1.0; 
    private boolean cameraNotified;         

    public Flight(String flightId, Airport departure, Airport destination,
                  List<Airport> routePath, long departureTime, long expectedArrivalTime) {
        this.flightId = flightId;
        this.departure = departure;
        this.destination = destination;
        this.routePath = routePath;
        this.currentRouteIndex = 0;
        this.status = STATUS_SCHEDULED;
        this.departureTime = departureTime;
        this.expectedArrivalTime = expectedArrivalTime;
        this.currentX = departure.getX();
        this.currentY = departure.getY();
        this.hasWeatherEmergency = false;
        this.altitude = 30000;  
        this.legProgress = 0.0;
        this.landingRequest = false;
    }

    public String getFlightId() {
        return flightId;
    }

    public Airport getDeparture() {
        return departure;
    }

    public Airport getDestination() {
        return destination;
    }

    public void setDestination(Airport destination) {
        this.destination = destination;
    }

    public List<Airport> getRoutePath() {
        return routePath;
    }

    public void setRoutePath(List<Airport> routePath) {
        this.routePath = routePath;
        this.currentRouteIndex = 0;
        this.legProgress = 0.0;
    }

    
    public void divertRemainingRoute(List<Airport> newRemainingPath) {
        this.routePath = newRemainingPath;
        this.currentRouteIndex = 0;
    }

    
    public void reverseCurrentLeg(List<Airport> reversedTwoPointRoute) {
        this.routePath = reversedTwoPointRoute;
        this.currentRouteIndex = 0;
        this.legProgress = 1.0 - this.legProgress;
    }

    public int getCurrentRouteIndex() {
        return currentRouteIndex;
    }

    public void setCurrentRouteIndex(int currentRouteIndex) {
        this.currentRouteIndex = currentRouteIndex;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDepartureTime() {
        return departureTime;
    }

    public long getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public void setExpectedArrivalTime(long expectedArrivalTime) {
        this.expectedArrivalTime = expectedArrivalTime;
    }

    public double getCurrentX() {
        return currentX;
    }

    public void setCurrentX(double currentX) {
        this.currentX = currentX;
    }

    public double getCurrentY() {
        return currentY;
    }

    public void setCurrentY(double currentY) {
        this.currentY = currentY;
    }

    public boolean isHasWeatherEmergency() {
        return hasWeatherEmergency;
    }

    public void setHasWeatherEmergency(boolean hasWeatherEmergency) {
        this.hasWeatherEmergency = hasWeatherEmergency;
    }

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitude) {
        this.altitude = altitude;
    }

    public double getLegProgress() {
        return legProgress;
    }

    public void setLegProgress(double legProgress) {
        this.legProgress = legProgress;
    }

    public boolean isLandingRequest() {
        return landingRequest;
    }

    public void setLandingRequest(boolean landingRequest) {
        this.landingRequest = landingRequest;
    }

    public String getEmergencyType() {
        return emergencyType;
    }

    public void setEmergencyType(String emergencyType) {
        this.emergencyType = emergencyType;
    }

    public long getLastConflictAtMillis() {
        return lastConflictAtMillis;
    }

    public void setLastConflictAtMillis(long lastConflictAtMillis) {
        this.lastConflictAtMillis = lastConflictAtMillis;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public boolean isCameraNotified() {
        return cameraNotified;
    }

    public void setCameraNotified(boolean cameraNotified) {
        this.cameraNotified = cameraNotified;
    }

 
    public Airport getCurrentAirport() {
        return routePath.get(currentRouteIndex);
    }
 
    public Airport getNextAirport() {
        if (currentRouteIndex + 1 < routePath.size()) {
            return routePath.get(currentRouteIndex + 1);
        }
        return null;
    }

    public boolean isLastLeg() {
        return currentRouteIndex >= routePath.size() - 1;
    }

    @Override
    public String toString() {
        return flightId + " (" + departure.getName() + " -> " + destination.getName() + ") [" + status + "]";
    }
}
