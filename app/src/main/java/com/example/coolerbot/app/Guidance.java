package com.example.coolerbot.app;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

public class Guidance implements LocationListener{

    private Context context;

    private Location[] waypoints;
    private Location home;
    private int index;

    private static final int TIME_CONSTANT = 250;

    private double x_current;
    private double y_current;
    private double[] x_waypoints;
    private double[] y_waypoints;

    private double x_los;
    private double y_los;
    private double desiredBearing;

    private double horizon;
    private double waypointDistance;

    private double switchDistance;
    private int maxIndex;
    private int state;

    private double psi_last;

    private LocationManager locationManager;
    private GuidanceEventListener guidanceEventListener;

    public Guidance(Context context, GuidanceEventListener guidanceEventListener) {
        this.context = context;
        this.guidanceEventListener = guidanceEventListener;

        home = new Location("Code");
        home.setLatitude(29.646104);
        home.setLongitude(-82.349658);
        Location waypoint1 = new Location("Code");
        waypoint1.setLatitude(29.646104);
        waypoint1.setLongitude(-82.349658);
        Location waypoint2 = new Location("Code");
        waypoint2.setLatitude(29.646098);
        waypoint2.setLongitude(-82.349877);
        Location waypoint3 = new Location("Code");
        waypoint3.setLatitude(29.646297);
        waypoint3.setLongitude(-82.349884);
        waypoints = new Location[3];
        waypoints[0] = waypoint1;
        waypoints[1] = waypoint2;
        waypoints[2] = waypoint3;
        switchDistance = 2;

        horizon = 2;

        maxIndex = waypoints.length;
        x_waypoints = new double[maxIndex];
        y_waypoints = new double[maxIndex];

        int i = 0;

        for (Location waypoint : waypoints) {
            float bearing = home.bearingTo(waypoint);
            float distance = home.distanceTo(waypoint);
            x_waypoints[i] = - distance*Math.sin(bearing * Math.PI / 180);
            y_waypoints[i] = distance*Math.cos(bearing * Math.PI / 180);
            i++;
        }

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        guidanceStart();
    }

    public void guidanceStart() {
        index = 1;
        state = 1;

        guidanceResume();
    }

    public void guidanceResume() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    public void guidancePause() {
        locationManager.removeUpdates(this);
    }

    private void calcLOSPosition () {

        double delta_x = x_waypoints[index] - x_waypoints[index-1];
        double delta_y = y_waypoints[index] - y_waypoints[index-1];

        double norm = Math.sqrt(delta_x * delta_x + delta_y * delta_y);

        double x_unit = delta_x / norm;
        double y_unit = delta_y / norm;

        if (delta_x == 0) {
            x_los = x_waypoints[index-1];
            if (delta_y > 0) y_los = y_current + horizon;
            else y_los = y_current - horizon;
        }
        else {
            double a = delta_y/delta_x;
            double c = y_waypoints[index-1] - a * x_waypoints[index-1];

            double x_nearest = ((x_current - a * y_current) - a*c) / (a*a + 1);
            double y_nearest = (a * (-x_current + a * y_current) - c) / (a*a + 1);

            x_los = x_nearest + x_unit;
            y_los = y_nearest + y_unit;
        }
    }

    private void waypointSwitch() {
        double distance  = Math.sqrt(Math.pow(x_waypoints[index] - x_current, 2)
                + Math.pow(y_waypoints[index] - y_current, 2));

        waypointDistance = distance;

        if (distance <= switchDistance) {
            index ++;
            if (index >= maxIndex) { index = -1; }
        }
    }

    private void calcDesiredBearing() {
        waypointSwitch();
        calcLOSPosition();

        double delta_x = x_los - x_current;
        double delta_y = y_los - y_current;

        double accumulate;

        double psi_now = Math.atan2(delta_y, delta_x);

        if (Math.signum(delta_y) == 1 && Math.signum(delta_x) == 1) {
            //Now in the first quadrant
            if(state == 3) {
                if ((psi_now + Math.abs( psi_last)) <= Math.PI) {
                    accumulate = psi_now - psi_last;
                }
                else { accumulate = psi_last - psi_now + 2*Math.PI; }
            }
            else { accumulate = psi_now - psi_last; }
            state = 1;
        }
        else if (Math.signum(delta_y) == -1 && Math.signum(delta_x) == 1) {
            //Now in the second quadrant
            if (state == 4) {
                if ((Math.abs(psi_now) + psi_last) <= Math.PI) {
                    accumulate = psi_now - psi_last;
                }
                else { accumulate = psi_now - psi_last + 2*Math.PI; }
            }
            else { accumulate = psi_now - psi_last; }
            state = 2;
        }
        else if (Math.signum(delta_y) == -1 && Math.signum(delta_x) == -1) {
            //Now in the third quadrant
            if (state == 1) {
                if ((Math.abs(psi_now) + psi_last) <= Math.PI) {
                    accumulate = psi_now - psi_last;
                }
                else { accumulate = psi_now - psi_last + 2*Math.PI; }
            }
            else if (state == 4) { accumulate = psi_now - psi_last +2*Math.PI; }
            else { accumulate = psi_now - psi_last; }
            state = 3;
        }
        else {
            //Now in the fourth quadrant
            if (state == 2) {
                if ((psi_now + Math.abs(psi_last)) <= Math.PI) {
                    accumulate = psi_now - psi_last;
                }
                else { accumulate = psi_last - psi_now + 2*Math.PI; }
            }
            else if (state == 3) { accumulate = psi_now - psi_last - 2*Math.PI; }
            else { accumulate = psi_now - psi_last; }
            state = 4;
        }

        desiredBearing += accumulate;
        psi_last = psi_now;
    }

    @Override
    public void onLocationChanged(Location location) {
        float bearing = home.bearingTo(location);
        float distance = home.distanceTo(location);
        x_current = distance * Math.cos(bearing * Math.PI / 180);
        y_current = distance * Math.sin(bearing * Math.PI / 180);

        calcDesiredBearing();

        guidanceEventListener.onGuidanceUpdate(desiredBearing, location.getAccuracy());
    }

    public double getWaypointDistance() { return waypointDistance; }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(context, "Enabled provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(context, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
    }
}
