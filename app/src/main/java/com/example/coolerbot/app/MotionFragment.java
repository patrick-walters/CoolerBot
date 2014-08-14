package com.example.coolerbot.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.Timer;
import java.util.TimerTask;

public class MotionFragment extends Fragment implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, EditText.OnEditorActionListener,
        Estimator.EstimatorEventListener, Guidance.GuidanceEventListener,
        Controller.ControllerEventListener {

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

    //Speed used by automatic control
    private byte speed = 90;

    //Handler to update display on motion fragment and timer for update rate
    private Handler handler = new Handler();
    private Timer displayUpdate = new Timer();

    private SeekBar speedSeekBar;
    private SeekBar turnSeekBar;
    private Button stopSelectButton;
    private Button modeSelectButton;

    //States for level of control
    private boolean isManual = true;
    private boolean isStop = true;


    //Create new instance for fragment.
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

        //Instantiate motor driver, estimator, controller, and guidance classes when fragment is
        //attached
        sabertoothDriver = new SabertoothDriver(activity);
        estimator = new Estimator(activity, this);
        controller = new Controller(this);
        guidance = new Guidance(activity, this, (Guidance.MotionUpdateListener) activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Inflate information layout
        View rootView = inflater.inflate(R.layout.information_fragment, container, false);

        //Resister all button, seek bar, and text edit callbacks from information layout
        stopSelectButton = (Button) rootView.findViewById(R.id.stopSelectButton);
        stopSelectButton.setOnClickListener(this);
        modeSelectButton = (Button) rootView.findViewById(R.id.modeSelectButton);
        modeSelectButton.setOnClickListener(this);
        speedSeekBar = (SeekBar) rootView.findViewById(R.id.speedSeekBar);
        speedSeekBar.setOnSeekBarChangeListener(this);
        turnSeekBar = (SeekBar) rootView.findViewById(R.id.turnSeekBar);
        turnSeekBar.setOnSeekBarChangeListener(this);
        EditText setSpeed = (EditText) rootView.findViewById(R.id.setSpeed);
        setSpeed.setOnEditorActionListener(this);
        EditText setGain = (EditText) rootView.findViewById(R.id.setGain);
        setGain.setOnEditorActionListener(this);

        //Set update rate for display
        displayUpdate.scheduleAtFixedRate(new UpdateDisplayTask(), 1000, 50);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Register listeners for sensors (e.g. accel, mag, gyro)
        estimator.registerListeners();
        sabertoothDriver.resumeDriver();
        guidance.guidanceResume();
    }

    @Override
    public void onPause() {
        //Unregister listeners for sensors to save battery during pause
        estimator.unregisterListeners();
        guidance.guidancePause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        //Must destroy motor driver driver to release usb port
        sabertoothDriver.destroyDriver();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.modeSelectButton:
                //Toggle manual/automatic mode
                if(isManual) {
                    //In automatic mode
                    modeSelectButton.setText("Go to manual");

                    //Disable seek bars during automatic mode
                    speedSeekBar.setEnabled(false);
                    turnSeekBar.setEnabled(false);

                    //Start guidance during automatic mode
                    guidance.guidanceResume();

                    //Set speed (e.g. currently a class variable)
                    sabertoothDriver.setForwardMixed(speed);
                    isManual = false;
                }
                //Just for you, Anup
                else {
                    //In manual mode
                    modeSelectButton.setText("Go to automatic");

                    //Enable seek bars
                    speedSeekBar.setEnabled(true);
                    turnSeekBar.setEnabled(true);

                    //Stop guidance during manual mode
                    guidance.guidancePause();

                    //Reset progress on seek bar, so last value is not latched
                    speedSeekBar.setProgress(0);
                    turnSeekBar.setProgress(65);

                    //Set speed to zero and turn to center when entering manual
                    sabertoothDriver.setForwardMixed((byte)0);
                    sabertoothDriver.setTurnMixed((byte)65);
                    isManual = true;
                }
                break;
            case R.id.stopSelectButton:
                if(isStop) {
                    //In start
                    //Just set flag. The flag is used else where.
                    stopSelectButton.setText("Stop");
                    stopSelectButton.setBackgroundColor(0xffcc0000);
                    isStop = false;
                }
                //Just for you, Anup
                else {
                    //In stop
                    stopSelectButton.setText("Start");
                    stopSelectButton.setBackgroundColor(0xff669900);

                    //Coming into stop state, reset progress and motor commands.
                    speedSeekBar.setProgress(0);
                    turnSeekBar.setProgress(65);
                    sabertoothDriver.setForwardMixed((byte) 0);
                    sabertoothDriver.setTurnMixed((byte)65);
                    isStop = true;
                }
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        //Update speed and turn on motor drive when seek bar is changed
        switch (seekBar.getId()) {
            case R.id.speedSeekBar:
                sabertoothDriver.setForwardMixed((byte) i);
                break;
            case R.id.turnSeekBar:
                sabertoothDriver.setTurnMixed((byte) i);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}


    @Override
    public void onControllerUpdate(double effort) {
        //Callback from controller class. Called when controller updates. Sets turn value to motors.
        //Left and right turn range from 0-127, but clamped to 0-65, so no one gets knee caped by
        //bot.
        if(isManual) { return; }
        this.effort = effort;
        if (effort <= 0) {
            effort = Math.abs(effort);
            if (effort >= 65) {
                effort = 65;
            }
            sabertoothDriver.setLeftMixed((byte) Math.round(effort));
        }
        else {
            if (effort >= 65) {
                effort = 65;
            }
            sabertoothDriver.setRightMixed((byte) Math.round(effort));
        }
    }

    @Override
    public void onEstimatorUpdate(float[] fusedData) {
        //Callback from estimator class. Gets called when filter updates. "orientation" is used to
        //update display
        orientation = fusedData;

        //Pass current yaw to controller
        controller.setActual(fusedData[0]);
    }

    @Override
    public void onGuidanceUpdate(double desiredBearing, double gpsAccuracy) {
        //Callback from guidance class. Called when guidance updates (on GPS update). Values used
        //for display.
        this.desiredBearing = desiredBearing;
        this.gpsAccuracy = gpsAccuracy;
        waypointDistance = guidance.getWaypointDistance();

        //Pass desired bearing to controller
        controller.setDesired(desiredBearing);

    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        //Handle text inputs from GUI for speed and control gain.
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            switch (textView.getId()) {
                case R.id.setGain:
                    controller.setGains(Integer.parseInt(textView.getText().toString()), 0, 0);
                    break;
                case R.id.setSpeed:
                    speed = (byte) Integer.parseInt(textView.getText().toString());
                    break;
            }
            handled = true;
        }
        return handled;
    }

    //Called by parent class to add waypoint from map fragment to guidance
    public void addWaypoint(LatLng waypoint) {
        guidance.addWaypoint(waypoint);
    }

    //Display update task
    private class UpdateDisplayTask extends TimerTask {
        @Override
        public void run() {
            handler.post(updateDisplay);
        }
    }

    //Display update function
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

