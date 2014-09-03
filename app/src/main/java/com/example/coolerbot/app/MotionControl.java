package com.example.coolerbot.app;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

public class MotionControl implements Estimator.EstimatorEventListener, Guidance.GuidanceEventListener,
        Controller.ControllerEventListener, LocationListener{

    private SabertoothDriver sabertoothDriver;
    private Estimator estimator;
    private Controller controller;
    private Guidance guidance;

    private Context context;
    private LocationManager locationManager;
    private MotionControlEventListener motionControlEventListener;

    private boolean enabled = false;
    private boolean missionRunning = false;

    private byte currentDesiredSpeed;
    private byte desiredSpeedSetpoint = 90;
    private boolean isHomeSet = false;

    private static final double DELTA_MAX = 2;

    MotionControl(Activity activity) {
        context = activity;

        //Instantiate motor driver, estimator, controller, and guidance classes when fragment is
        //attached
        sabertoothDriver = new SabertoothDriver(context);
        estimator = new Estimator(activity, this);
        controller = new Controller(this);
        guidance = new Guidance(this);

        motionControlEventListener = (MotionControlEventListener) context;

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void onResume() {
        //Reconnect accessory device
        sabertoothDriver.resumeDriver();

        //Register listeners for sensors (e.g. accel, mag, gyro)
        estimator.registerListeners();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    public void onPause() {
        //Turn off guidance and controller
        disable();

        //Unregister listeners for sensors to save battery during pause
        estimator.unregisterListeners();
        locationManager.removeUpdates(this);
    }

    public void onDestroy() {
        //Must destroy motor driver driver to release usb port
        sabertoothDriver.destroyDriver();
    }

    public void enable() {
        enabled = true;
        if (missionRunning) {
            resumeMission();
        }
    }

    public void disable() {
        enabled = false;
        pauseMission();
    }

    public boolean startMission() {
        if (!enabled) { return false; }
        guidance.onStart();
        controller.onStart();
        missionRunning = true;
        return true;
    }

    public boolean resumeMission() {
        if (!enabled) { return false; }
        guidance.onResume();
        controller.onStart();
        missionRunning = true;
        return true;
    }

    public void pauseMission() {
        controller.onStop();
        guidance.onStop();
        allStop();
    }

    public void stopMission() {
        pauseMission();
        missionRunning = false;
    }

    public boolean setManualControl(int speed, int turn) {
        if (!enabled || missionRunning) { return false; }

        if (speed <= 0) {
            speed = Math.abs(speed);
            if (speed >= 127) { speed = 127; }
            sabertoothDriver.setBackwardMixed((byte) speed);
        }
        else {
            if (speed >= 127) { speed = 127; }
            sabertoothDriver.setForwardMixed((byte) speed);
        }
        if (turn <= 0) {
            turn = Math.abs(turn);
            if (turn >= 127) { turn = 127; }
            sabertoothDriver.setLeftMixed((byte) turn);
        }
        else {
            if (turn >= 127) { turn = 127; }
            sabertoothDriver.setRightMixed((byte) turn);
        }
        return true;
    }

    public boolean addWaypoint(LatLng waypoint) {
        guidance.addWaypoint(waypoint);

        return true;
    }

    public void setControllerGains(double kp, double ki, double kd) {
        controller.setGains(kp, ki, kd);
    }

    public void setDesiredSpeedSetpoint(byte speed) {
        desiredSpeedSetpoint = speed;
    }

    public boolean isEnabled() { return enabled; }
    public boolean isMissionRunning() { return missionRunning; }

    public double getDesiredBearing() {
        return guidance.getDesiredBearing();
    }

    public double getDistanceToNext() {
        return guidance.getDistanceToNext();
    }

    public double getActualBearing() {
        return estimator.getActualBearing();
    }

    public double getControlEffort() {
        return controller.getEffort();
    }

    private void allStop() {
        currentDesiredSpeed = 0;
        sabertoothDriver.setForwardMixed((byte) 0);
        sabertoothDriver.setLeftMixed((byte) 0);
    }

    @Override
    public void onControllerUpdate(double effort) {
        //Callback from controller class. Called when controller updates. Sets turn value to motors.
        //Left and right turn range from 0-127, but clamped to 0-65, so no one gets knee caped by
        //bot.

        double deltaSign = Math.signum(desiredSpeedSetpoint - currentDesiredSpeed);
        double deltaMag = Math.abs(desiredSpeedSetpoint - currentDesiredSpeed);

        if (deltaMag >= DELTA_MAX) {
            currentDesiredSpeed += deltaSign*DELTA_MAX;
        }
        else {
            currentDesiredSpeed = desiredSpeedSetpoint;
        }

        sabertoothDriver.setForwardMixed(currentDesiredSpeed);

        if (effort <= 0) {
            effort = Math.abs(effort);
            if (effort >= 65) { effort = 65; }
            sabertoothDriver.setLeftMixed((byte) Math.round(effort));
        }
        else {
            if (effort >= 65) { effort = 65; }
            sabertoothDriver.setRightMixed((byte) Math.round(effort));
        }
    }

    @Override
    public void onEstimatorUpdate(float[] fusedData) {
        //Pass current yaw to controller
        controller.setActual(fusedData[0]);
    }

    @Override
    public void onGuidanceUpdate(double bearing) {
        //Pass desired bearing to controller
        controller.setDesired(bearing);
    }

    @Override
    public void onGuidanceCompletion() {
        stopMission();
    }

    @Override
    public void onLOSWaypointUpdate(LatLng los) {
        motionControlEventListener.onLOSWaypointUpdate(los);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
            if (!isHomeSet) {
                motionControlEventListener.onHomeWaypointUpdate(latLng);
                guidance.setHomeLocation(latLng);
                isHomeSet = true;
                Toast.makeText(context, "Location Found", Toast.LENGTH_SHORT).show();
            }
            guidance.setCurrentLocation(latLng);
        }
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

    public interface MotionControlEventListener {
        public void onHomeWaypointUpdate(LatLng home);
        public void onLOSWaypointUpdate(LatLng los);
    }
}