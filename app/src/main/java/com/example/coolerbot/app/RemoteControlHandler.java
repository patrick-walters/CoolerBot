package com.example.coolerbot.app;

import android.app.Activity;

import com.google.android.gms.maps.model.LatLng;

public class RemoteControlHandler implements MotionControl.MotionControlEventListener {

    private MotionControl motionControl;
    private boolean isRemote = false;
    private RemoteControlEventListener remoteControlEventListener;

    RemoteControlHandler(Activity activity) {
        this.motionControl = new MotionControl(activity, this);
        remoteControlEventListener = (RemoteControlEventListener) activity;
    }

    public void onResume() {
        if (!isRemote) {
            motionControl.onResume();
        }
    }

    public void onPause() {
        if (!isRemote) {
            motionControl.onPause();
        }
    }

    public void onDestroy() {
        if (!isRemote) {
            motionControl.onDestroy();
        }
    }

    public void enable() {
        if (!isRemote) {
            motionControl.enable();
        }
    }

    public void disable() {
        if (!isRemote) {
            motionControl.disable();
        }
    }

    public boolean startMission() {
        if (!isRemote) {
            return motionControl.startMission();
        }
        return false;
    }

    public boolean resumeMission() {
        if (!isRemote) {
            return motionControl.resumeMission();
        }
        return false;
    }

    public void pauseMission() {
        if (!isRemote) {
            motionControl.pauseMission();
        }
    }

    public void stopMission() {
        if (!isRemote) {
            motionControl.stopMission();
        }
    }

    public boolean setManualControl(int speed, int turn) {
        if (!isRemote) {
            motionControl.setManualControl(speed, turn);
        }
        return true;
    }

    public boolean addWaypoint(LatLng waypoint) {
        if (!isRemote) {
            motionControl.addWaypoint(waypoint);
        }
        return true;
    }

    public void setControllerGains(double kp, double ki, double kd) {
        if (!isRemote) {
            motionControl.setControllerGains(kp,ki,kd);
        }
    }

    public void setDesiredSpeedSetpoint(byte speed) {
        if (!isRemote) {
            motionControl.setDesiredSpeedSetpoint(speed);
        }
    }

    public boolean isEnabled() {
        if (!isRemote) {
            return motionControl.isEnabled();
        }
        return false;
    }
    public boolean isMissionRunning() {
        if (!isRemote) {
            return motionControl.isMissionRunning();
        }
        return false;
    }

    public double getDesiredBearing() {
        if (!isRemote) {
            return motionControl.getDesiredBearing();
        }
        return 0;
    }

    public double getDistanceToNext() {
        if (!isRemote) {
            return motionControl.getDistanceToNext();
        }
        return 0;
    }

    public double getActualBearing() {
        if (!isRemote) {
            return motionControl.getActualBearing();
        }
        return 0;
    }

    public double getControlEffort() {
        if (!isRemote) {
            return motionControl.getControlEffort();
        }
        return 0;
    }

    @Override
    public void onHomeWaypointUpdate(LatLng home) {
        remoteControlEventListener.onHomeWaypointUpdate(home);
    }

    @Override
    public void onLOSWaypointUpdate(LatLng los) {
        remoteControlEventListener.onLOSWaypointUpdate(los);
    }

    public interface RemoteControlEventListener {
        public void onHomeWaypointUpdate(LatLng home);
        public void onLOSWaypointUpdate(LatLng los);
    }
}
