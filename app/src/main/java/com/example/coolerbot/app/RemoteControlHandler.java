package com.example.coolerbot.app;

import android.app.Activity;

import com.google.android.gms.maps.model.LatLng;

public class RemoteControlHandler {

    private MotionControl motionControl;
    private boolean isRemote;

    RemoteControlHandler(MotionControl motionControl) {
        this.motionControl = motionControl;
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
            motionControl.startMission();
        }
        return true;
    }

    public boolean resumeMission() {
        if (!isRemote) {
            motionControl.resumeMission();
        }
        return true;
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
    }
    public boolean isMissionRunning() {
        if (!isRemote) {
            return isMissionRunning();
        }
    }

    public double getDesiredBearing() {
        if (!isRemote) {
            return motionControl.getDesiredBearing();
        }
    }

    public double getDistanceToNext() {
        if (!isRemote) {
            return motionControl.getDistanceToNext();
        }
    }

    public double getActualBearing() {
        if (!isRemote) {
            return motionControl.getActualBearing();
        }
    }

    public double getControlEffort() {
        if (!isRemote) {
            return motionControl.getControlEffort();
        }
    }
}
