package com.example.coolerbot.app;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class Guidance{

    private List<LatLng> waypoints = new ArrayList<LatLng>(2);
    private LatLng home;
    private int index = 1;
    private int state = 1;

    private double x_current;
    private double y_current;
    private List<Double> x_waypoints = new ArrayList<Double>(2);
    private List<Double> y_waypoints = new ArrayList<Double>(2);

    private double x_los;
    private double y_los;

    private double horizon;

    private double desiredBearing;
    private double psi_last;

    private boolean isEnabled = false;

    private GuidanceEventListener guidanceEventListener;

    public Guidance(GuidanceEventListener guidanceEventListener) {
        this.guidanceEventListener = guidanceEventListener;

        horizon = 5;
    }

    public void onStart() {
        onStart(1);
    }

    public void onStart(int waypointNumber) {
        index = waypointNumber;
        state = 1;
        isEnabled = true;
    }

    public void onResume() {
        isEnabled = true;
    }

    public void onStop() {
        isEnabled = false;
    }

    public void addWaypoint(LatLng waypoint) {
        waypoints.add(waypoint);

        double[] coordinate = LatLngHelper.ll2ltp(home,waypoint);
        x_waypoints.add(coordinate[1]);
        y_waypoints.add(coordinate[0]);
    }

    public void setHomeLocation(LatLng home) {
        this.home = home;
    }

    public void setCurrentLocation(LatLng location) {
        if(waypoints.size() < 2 || !isEnabled || home == null) {return;}

        double[] current_coordinate = LatLngHelper.ll2ltp(home,location);
        x_current = current_coordinate[1];
        y_current = current_coordinate[0];

        calcDesiredBearing();
    }

    public double getDistanceToNext() {
        if ((waypoints.size() - 1) < index) { return 0; }
        return Math.sqrt(Math.pow(x_waypoints.get(index)-x_current,2)
                + Math.pow(y_waypoints.get(index)-y_current,2));
    }

    public double getDesiredBearing() {
        return desiredBearing;
    }

    //Projects current position onto the line connecting the current waypoint and previous waypoint,
    //then advances down the line "horizon" meters.
    private void calcLOSPosition () {
        //Calculates the vector from the previous waypoint to the current waypoint
        double delta_x = x_waypoints.get(index) - x_waypoints.get(index-1);
        double delta_y = y_waypoints.get(index) - y_waypoints.get(index-1);

        //Calculate the norm
        double norm = Math.sqrt(Math.pow(delta_x,2) + Math.pow(delta_y,2));

        //Normalized vector
        double x_unit = delta_x / norm;
        double y_unit = delta_y / norm;

        //If the vector is vertical, just add horizon to y component of current position while
        //keeping x component of current waypoint
        if (delta_x == 0) {
            x_los = x_waypoints.get(index-1);
            if (delta_y > 0) y_los = y_current + horizon;
            else y_los = y_current - horizon;
        }
        //Else complete projection
        else {
            //Find line equation for vector in the form a*x + b*y + c = 0 where b = 0.
            double a = delta_y/delta_x;
            double c = y_waypoints.get(index-1) - a * x_waypoints.get(index-1);

            //Find point nearest to current position on the line.
            double x_nearest = (-(-x_current - a * y_current) - a*c) / (Math.pow(a,2) + 1);
            double y_nearest = (a * (x_current + a * y_current) + c) / (Math.pow(a,2) + 1);

            //Add the horizon components along line to nearest point.
            x_los = x_nearest + horizon*x_unit;
            y_los = y_nearest + horizon*y_unit;
        }

        //Update motion control so LOS waypoint can be added to map for debugging.
        guidanceEventListener.onLOSWaypointUpdate
                (LatLngHelper.ltp2lla(home,new double[]{y_los,x_los,0}));

    }

    private void waypointSwitch() {
        //Bisection algorithm to determine waypoint switches
        double a_x = x_waypoints.get(index-1);
        double a_y = y_waypoints.get(index-1);
        double b_x = x_waypoints.get(index);
        double b_y = y_waypoints.get(index);

        //Find normal vector from current waypoint to previous waypoint.
        double v1_x = a_x - b_x;
        double v1_y = a_y - b_y;
        double v1_norm = Math.sqrt(Math.pow(v1_x,2) + Math.pow(v1_y,2));
        v1_x = v1_x/v1_norm;
        v1_y = v1_y/v1_norm;

        //Find normal vector from current waypoint to next waypoint. If next waypoint does not exist
        //then find normal vector perpendicular to v1.
        double v2_x;
        double v2_y;

        if ((index + 1) >= waypoints.size()) {
            v2_x = v1_y;
            v2_y = -v1_x;
        }
        else {
            double c_x = x_waypoints.get(index+1);
            double c_y = y_waypoints.get(index+1);

            v2_x = c_x - b_x;
            v2_y = c_y - b_y;
            double v2_norm = Math.sqrt(Math.pow(v2_x,2) + Math.pow(v2_y,2));
            v2_x = v2_x/v2_norm;
            v2_y = v2_y/v2_norm;
        }

        //v3 bisects v1 and v2;
        double v3_x = v1_x + v2_x;
        double v3_y = v1_y + v2_y;

        //calculate the end point of v3
        double d_x = v3_x + b_x;
        double d_y = v3_y + b_y;

        //Check if current position is on the left or right side of the bisection vector, and check
        //if previous waypoint is on the left or right side of the bisection vector.
        double sideA =  Math.signum((d_x-b_x)*(y_current-b_y) - (d_y-b_y)*(x_current-b_x));
        double sideB =  Math.signum((d_x-b_x)*(a_y-b_y) - (d_y-b_y)*(a_x-b_x));

        //If the previous waypoint and current position are on opposite sides switch to next
        //waypoint
        if (sideA != sideB) {
            //If no more waypoints stop
            if ((index + 1) >= waypoints.size()) {
                guidanceEventListener.onGuidanceCompletion();
            }
            else {
                index ++;
            }
        }
    }

    //Calculation absolute bearing (e.g. 720 deg if circled twice). Provides continuous input signal
    //for controller.
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

        guidanceEventListener.onGuidanceUpdate(desiredBearing);
    }

    public interface GuidanceEventListener {
        public void onGuidanceUpdate(double bearing);
        public void onGuidanceCompletion();
        public void onLOSWaypointUpdate(LatLng los);
    }
}
