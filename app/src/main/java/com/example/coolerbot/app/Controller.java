package com.example.coolerbot.app;

import android.content.Context;

import java.util.Timer;
import java.util.TimerTask;

public class Controller implements EstimatorEventListener{

    private double control;
    private double kp;
    private double ki;
    private double kd;

    private double integral;

    private double desiredSetpoint;
    private double desired;
    private double deltaMax;
    private double actual;

    private ControllerEventListener controllerEventListener;

    private static final int TIME_CONSTANT = 30;

    public Controller(Context context, ControllerEventListener controllerEventListener) {
        this.controllerEventListener = controllerEventListener;

        Estimator estimator = new Estimator(context, this);
        Timer controllerTimer = new Timer();

        kp = 1;
        ki = 0;
        kd = 0;

        deltaMax = 0.0025;
        actual = 500;

        controllerTimer.scheduleAtFixedRate(new ControlTask(), 1000, TIME_CONSTANT);
    }

    public void setGains(double kp, double ki, double kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    public void setDesired(double desired) {
        desiredSetpoint = desired;
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
        if (actual < -Math.PI || actual > Math.PI) { return; }

        double deltaSign = Math.signum(desiredSetpoint - desired);
        double deltaMag = Math.abs(desiredSetpoint - desired);

        if (deltaMag >= deltaMax) {
            desired += deltaSign*deltaMax;
        }

        double error = remapAngle(desired) - actual;

        if (error > Math.PI) { error -= 2*Math.PI; }
        else if (error < -Math.PI) { error += 2*Math.PI; }

        integral += ki*error*(1/TIME_CONSTANT);

        controllerEventListener.onControllerUpdate(Math.toDegrees(kp*error + integral));
    }

    @Override
    public void onEstimatorUpdate(float[] fusedData) {
        actual = fusedData[0];
    }

    private class ControlTask extends TimerTask {
        @Override
        public void run() {
            updateControl();
        }
    }
}
