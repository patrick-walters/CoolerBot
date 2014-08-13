package com.example.coolerbot.app;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class Guidance implements LocationListener{

    private Context context;

    private MotionUpdateListener motionUpdateListener;

    private List<LatLng> waypoints = new ArrayList<LatLng>(2);
    private LatLng home;
    private int index;

    private double x_current;
    private double y_current;
    private List<Double> x_waypoints = new ArrayList<Double>(2);
    private List<Double> y_waypoints = new ArrayList<Double>(2);

    private double x_los;
    private double y_los;
    private double desiredBearing;

    private double horizon;
    private double waypointDistance;

    private double switchDistance;
    private int state;

    private double psi_last;

    private LocationManager locationManager;
    private GuidanceEventListener guidanceEventListener;

    public Guidance(Context context, GuidanceEventListener guidanceEventListener,
                    MotionUpdateListener motionUpdateListener) {
        this.context = context;
        this.guidanceEventListener = guidanceEventListener;
        this.motionUpdateListener = motionUpdateListener;

        switchDistance = 5;
        horizon = 5;

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        guidanceStart();
    }

    public void addWaypoint(LatLng waypoint) {
        waypoints.add(waypoint);

        double[] coordinate = LatLngHelper.ll2ltp(home,waypoint);
        x_waypoints.add(coordinate[1]);
        y_waypoints.add(coordinate[0]);
    }

    public double getWaypointDistance() { return waypointDistance; }

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
        double delta_x = x_waypoints.get(index) - x_waypoints.get(index-1);
        double delta_y = y_waypoints.get(index) - y_waypoints.get(index-1);

        double norm = Math.sqrt(Math.pow(delta_x,2) + Math.pow(delta_y,2));

        double x_unit = delta_x / norm;
        double y_unit = delta_y / norm;

        if (delta_x == 0) {
            x_los = x_waypoints.get(index-1);
            if (delta_y > 0) y_los = y_current + horizon;
            else y_los = y_current - horizon;
        }
        else {
            double a = delta_y/delta_x;
            double c = y_waypoints.get(index-1) - a * x_waypoints.get(index-1);

            double x_nearest = (-(-x_current - a * y_current) - a*c) / (Math.pow(a,2) + 1);
            double y_nearest = (a * (x_current + a * y_current) + c) / (Math.pow(a,2) + 1);

            x_los = x_nearest + horizon*x_unit;
            y_los = y_nearest + horizon*y_unit;
        }

        motionUpdateListener.onLOSUpdate(LatLngHelper.ltp2lla(home,new double[]{y_los,x_los,0}));

    }

    private void waypointSwitch() {
        double distance  = Math.sqrt(Math.pow(x_waypoints.get(index) - x_current, 2)
                + Math.pow(y_waypoints.get(index) - y_current, 2));

        waypointDistance = distance;

        if (distance <= switchDistance) {
            index ++;
            if (index >= waypoints.size()) { index = -1; }
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
        if(home == null && location != null) {
            home = new LatLng(location.getLatitude(),location.getLongitude());
            motionUpdateListener.onHomeUpdate(home);
        }
        if(waypoints.size() < 2) {return;}
        double[] current_coordinate = LatLngHelper.ll2ltp(home,
                new LatLng(location.getLatitude(),location.getLongitude()));
        x_current = current_coordinate[1];
        y_current = current_coordinate[0];

        calcDesiredBearing();

        guidanceEventListener.onGuidanceUpdate(desiredBearing, location.getAccuracy());
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

    public interface GuidanceEventListener {
        public void onGuidanceUpdate(double desiredBearing, double accuracy);
    }

    public interface MotionUpdateListener {
        public void onHomeUpdate(LatLng home);
        public void onLOSUpdate(LatLng los);
    }
}
