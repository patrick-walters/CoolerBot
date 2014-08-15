package com.example.coolerbot.app;

import java.util.Timer;
import java.util.TimerTask;

public class Controller{

    private double kp;
    private double ki;
    private double kd;

    private double integral;

    private double desiredSetpoint;
    private double desired;
    private double deltaMax;
    private double actual;
    private double effort;

    private Timer controllerTimer;

    private ControllerEventListener controllerEventListener;

    private static final int TIME_CONSTANT = 30;

    public Controller(ControllerEventListener controllerEventListener) {
        this.controllerEventListener = controllerEventListener;

        kp = 1;
        ki = 0;
        kd = 0;
        deltaMax = 0.0025;
    }

    public void onStart() {
        controllerTimer = new Timer();
        controllerTimer.scheduleAtFixedRate(new ControlTask(), 0, TIME_CONSTANT);
    }

    public void onStop() {
        if (controllerTimer != null) {
            controllerTimer.cancel();
        }
        effort = 0;
        integral = 0;
    }


    public void setGains(double kp, double ki, double kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    public void setDesired(double desired) {
        desiredSetpoint = desired;
    }

    public void setActual(double actual) {
        this.actual = actual;
    }

    public double getEffort() {
        return effort;
    }

    private double remapAngle(double angle) {
        double n;
        double output;

        if (Math.signum(angle) > 0 ) { n = Math.floor(angle/Math.PI); }
        else if (Math.signum(angle) < 0) { n = Math.ceil(angle/Math.PI); }
        else { n = 0; }

        double reminder = n % 2;

        if (reminder == 0) { output = angle - n*Math.PI; }
        else {
            if (Math.signum(reminder) > 0) { output = angle - (n+1)* Math.PI; }
            else { output = angle - (n-1)*Math.PI; }
        }

        return output;
    }

    private void updateControl() {
        double deltaSign = Math.signum(desiredSetpoint - desired);
        double deltaMag = Math.abs(desiredSetpoint - desired);

        if (deltaMag >= deltaMax) {
            desired += deltaSign*deltaMax;
        }

        double error = Math.toDegrees(remapAngle(desired) - actual);

        if (error > Math.PI) { error -= 2*Math.PI; }
        else if (error < -Math.PI) { error += 2*Math.PI; }

        integral += ki*error*(1/TIME_CONSTANT);

        effort = kp*error + integral;

        controllerEventListener.onControllerUpdate(effort);
    }

    private class ControlTask extends TimerTask {
        @Override
        public void run() {
            updateControl();
        }
    }

    public interface ControllerEventListener {
        public void onControllerUpdate(double effort);
    }
}


