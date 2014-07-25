package com.example.coolerbot.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MotionFragment extends Fragment implements View.OnClickListener,
        EstimatorEventListener, GuidanceEventListener, ControllerEventListener{

    private static final String ARG_SECTION_NUMBER = "section_number";
    private SabertoothDriver sabertoothDriver;
    private Estimator estimator;
    private Controller controller;
    private Guidance guidance;

    private float[] orientation = new float[3];
    private double desiredBearing;
    private double gpsAccuracy;
    private double effort;
    private double waypointDistance;

    private Handler handler = new Handler();
    private Timer displayUpdate = new Timer();

    public static MotionFragment newInstance(int sectionNumber) {
        MotionFragment fragment = new MotionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public MotionFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        sabertoothDriver = new SabertoothDriver(activity);
        estimator = new Estimator(activity, this);
        controller = new Controller(activity, this);
        guidance = new Guidance(activity, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.information_fragment, container, false);

        Button forwardButton = (Button) rootView.findViewById(R.id.forwardButton);
        forwardButton.setOnClickListener(this);
        Button leftButton = (Button) rootView.findViewById(R.id.leftButton);
        leftButton.setOnClickListener(this);
        Button rightButton = (Button) rootView.findViewById(R.id.rightButton);
        rightButton.setOnClickListener(this);
        Button backwardButton = (Button) rootView.findViewById(R.id.backwardButton);
        backwardButton.setOnClickListener(this);
        Button stopButton = (Button) rootView.findViewById(R.id.stopButton);
        stopButton.setOnClickListener(this);

        displayUpdate.scheduleAtFixedRate(new UpdateDisplayTask(), 1000, 50);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        sabertoothDriver.resumeDriver();
        estimator.registerListeners();
        guidance.guidanceResume();
    }

    @Override
    public void onPause() {
        estimator.unregisterListeners();
        guidance.guidancePause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        sabertoothDriver.destroyDriver();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.forwardButton:
                sabertoothDriver.setForwardMixed((byte)50);
                break;
            case R.id.leftButton:
                sabertoothDriver.setLeftMixed((byte)30);
                break;
            case R.id.rightButton:
                sabertoothDriver.setRightMixed((byte)30);
                break;
            case R.id.backwardButton:
                sabertoothDriver.setBackwardMixed((byte)50);
                break;
            case R.id.stopButton:
                sabertoothDriver.setForwardMixed((byte)0);
                break;
        }
    }


    @Override
    public void onControllerUpdate(double effort) {
        this.effort = effort;
    }

    @Override
    public void onEstimatorUpdate(float[] fusedData) {
        orientation = fusedData;
    }

    @Override
    public void onGuidanceUpdate(double desiredBearing, double gpsAccuracy) {
        this.desiredBearing = desiredBearing;
        this.gpsAccuracy = gpsAccuracy;
        controller.setDesired(desiredBearing);
        waypointDistance = guidance.getWaypointDistance();
    }

    private class UpdateDisplayTask extends TimerTask {
        @Override
        public void run() {
            handler.post(updateDisplay);
        }
    }

    private Runnable updateDisplay = new Runnable() {
        public void run() {
            if ( getView() != null ) {
                TextView fusedAzimuth = (TextView) getView().findViewById(R.id.bearing);
                fusedAzimuth.setText("Current Bearing: " + (float) (Math.toDegrees(orientation[0])));
                TextView azimuth = (TextView) getView().findViewById(R.id.wpBearing);
                azimuth.setText("Waypoint Bearing: " + (float) (Math.toDegrees(desiredBearing)));
                TextView accuracy = (TextView) getView().findViewById(R.id.accruacy);
                accuracy.setText("GPS Accuracy: " + (float) gpsAccuracy);
                TextView control = (TextView) getView().findViewById(R.id.effort);
                control.setText("Effort: " + (float) effort);
                TextView waypoint = (TextView) getView().findViewById(R.id.waypointDistance);
                waypoint.setText("Distance: " + (float) waypointDistance);
            }

        }
    };
}

