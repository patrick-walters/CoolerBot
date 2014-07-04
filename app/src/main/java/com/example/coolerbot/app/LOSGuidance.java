package com.example.coolerbot.app;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Timer;

public class LOSGuidance implements LocationListener{

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

    private double horizon;

    private double switchDistance;
    private int maxIndex;
    private int state;

    private double psi_now;
    private double psi_last;

    private LocationManager locationManager;
    private Timer guidanceTimer = new Timer();


    public LOSGuidance(Context context, Location home, Location[] waypoints, double switchDistance) {
        this.context = context;
        this.home = home;
        this.waypoints = waypoints;
        this.switchDistance = switchDistance;

        maxIndex = waypoints.length;
        x_waypoints = new double[maxIndex-1];
        y_waypoints = new double[maxIndex-1];

        int i = 0;

        for (Location waypoint : waypoints) {
            float bearing = home.bearingTo(waypoint);
            float distance = home.distanceTo(waypoint);
            x_waypoints[i] = distance*Math.cos(bearing*Math.PI/180);
            y_waypoints[i] = distance*Math.sin(bearing*Math.PI/180);
            i++;
        }

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        guidanceStart();
    }

    public void guidanceStart() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        index = 1;
        state = 1;
    }

    public void guidancePause() {
        locationManager.removeUpdates(this);
    }

    public double getAngle() {
        return angle;
    }

    private void calcLOSPosition () {

        double delta_x = x_waypoints[index] - x_waypoints[index-1];
        double delta_y = y_waypoints[index] - y_waypoints[index-1];

        if (delta_x == 0) {
            x_los = x_waypoints[index-1];

            if (delta_y > 0) y_los = y_current + horizon;
            else y_los = y_current - horizon;
        }
        else {
            double d = delta_y/delta_x;
            double e = x_waypoints[index-1];
            double f = y_waypoints[index-1];
            double g = -d*e + f;

            double a = 1 + d*d;
            double b = 2*(d*g - d* y_current - x_current);
            double c = x_current * x_current + y_current * y_current + g*g - (horizon)*(horizon)
                    - 2*g* y_current;

            if (delta_x > 0) x_los = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
            else x_los = (-b - Math.sqrt(b * b - 4 * a * c)) / (2 * a);
            y_los = d*(x_los - e) + f;
        }
    }

    private void waypointSwitch() {
        double distance  = Math.sqrt(Math.pow(x_waypoints[index] - x_current, 2)
                + Math.pow(y_waypoints[index] - y_current, 2));

        if (distance <= switchDistance) {
            index ++;
            if (index >= maxIndex) { index = -1; }
        }
    }

    private void angleMapping (double delta_x, double delta_y, double angle) {
        if (Math.signum(delta_y) == 1 && Math.signum(delta_x) == 1) {
            //Now in the first quadrant
            if(state == 3) {
                if ((psi_now + Math.abs( psi_last)) <= Math.PI) {
                    double accumulate = psi_now = psi_last;
                }
                else { double accumulate = psi_last - psi_now + 2*Math.PI; }
            }
            else { double accumulate = psi_now - psi_last; }
            state = 1;
        }
        else if (Math.signum(delta_y) == -1 && Math.signum(delta_x) == 1) {
            //Now in the second quadrant
            if (state == 4) {
                if ((Math.abs(psi_now) + psi_last) <= Math.PI) {
                    double accumulate = psi_now - psi_last;
                }
                else { double accumulate = psi_now - psi_last + 2*Math.PI; }
            }
            else { double accumulate = psi_now - psi_last; }
            state = 2;
        }
        else if (Math.signum(delta_y) == -1 && Math.signum(delta_x) == -1) {
            //Now in the third quadrant
            if (state == 1) {
                if ((Math.abs(psi_now) + psi_last) <= Math.PI) {
                    double accumulate = psi_now - psi_last;
                }
                else { double accumulate = psi_now - psi_last + 2*Math.PI; }
            }
            else if (state == 4) { double accumulate = psi_now - psi_last +2*Math.PI; }
            else { double accumulate = psi_now - psi_last; }
            state = 3;
        }
        else {
            //Now in the fourth quadrant
            if (state == 2) {
                if ((psi_now + Math.abs(psi_last)) <= Math.PI) {
                    double accumulate = psi_now - psi_last;
                }
                else { double accumulate = psi_last - psi_now + 2*Math.PI; }
            }
            else if (state == 3) { double accumulate = psi_now - psi_last - 2*Math.PI; }
            else { double accumulate = psi_now - psi_last; }
            state = 4;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        float bearing = home.bearingTo(location);
        float distance = home.distanceTo(location);
        x_current = distance * Math.cos(bearing * Math.PI / 180);
        y_current = distance * Math.sin(bearing * Math.PI / 180);

        waypointSwitch();
        calcLOSPosition();
    }

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
