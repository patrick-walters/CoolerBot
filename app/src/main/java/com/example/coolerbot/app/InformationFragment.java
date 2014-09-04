package com.example.coolerbot.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class InformationFragment extends Fragment implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, EditText.OnEditorActionListener{

    private static final String ARG_SECTION_NUMBER = "section_number";

    //Handler to update display on motion fragment and timer for update rate
    private Handler handler = new Handler();
    private Timer displayUpdate = new Timer();

    private SeekBar speedSeekBar;
    private SeekBar turnSeekBar;

    private RemoteControlHandler remoteControlHandler;

    private boolean previousEnableState;
    private boolean previousMissionState;

    //Create new instance for fragment.
    public static InformationFragment newInstance(int sectionNumber) {
        InformationFragment fragment = new InformationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public InformationFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //Save local reference to motion control class
        remoteControlHandler = ((MainActivity) activity).remoteControlHandler;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Inflate information layout
        View rootView = inflater.inflate(R.layout.information_fragment, container, false);

        previousEnableState = remoteControlHandler.isEnabled();
        previousMissionState = remoteControlHandler.isMissionRunning();

        //Register all button, seek bar, and text edit callbacks from information layout
        Button enableButton = (Button) rootView.findViewById(R.id.enableButton);
        enableButton.setOnClickListener(this);
        Button manualButton = (Button) rootView.findViewById(R.id.manualButton);
        manualButton.setOnClickListener(this);
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.manualButton:
                //Toggle manual/automatic mode
                if(!remoteControlHandler.isMissionRunning()) {
                    //Startup guidance and controller
                    remoteControlHandler.startMission();
                }
                //Just for you, Anup
                else {
                    //Stop guidance and controller
                    remoteControlHandler.stopMission();
                }
                break;
            case R.id.enableButton:
                if(!remoteControlHandler.isEnabled()) {
                    //Arm robot in motion control
                    remoteControlHandler.enable();
                }
                //Just for you, Anup
                else {
                    //Disarm robot in motion control
                    remoteControlHandler.disable();
                }
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        //Update speed and turn on motor drive when either seek bar is changed. Currently only goes
        //forward in this view. The minus 127 on turn is because there is no negative value.
        remoteControlHandler.setManualControl(speedSeekBar.getProgress(), turnSeekBar.getProgress() - 127);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        //Handle text inputs from GUI for speed and control gain.
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            switch (textView.getId()) {
                case R.id.setGain:
                    remoteControlHandler.setControllerGains
                            (Integer.parseInt(textView.getText().toString()), 0, 0);
                    break;
                case R.id.setSpeed:
                    remoteControlHandler.setDesiredSpeedSetpoint
                            ((byte) Integer.parseInt(textView.getText().toString()));
                    break;
            }
            handled = true;
        }
        return handled;
    }

    private void resetProgress()  {
        speedSeekBar.setProgress(0);
        turnSeekBar.setProgress(127);
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
                if(remoteControlHandler.isMissionRunning() && !previousMissionState) {
                    ((Button) getView().findViewById(R.id.manualButton))
                            .setText("Running Automatically");

                    //Disable seek bars during automatic mode
                    speedSeekBar.setEnabled(false);
                    turnSeekBar.setEnabled(false);

                    previousMissionState = remoteControlHandler.isMissionRunning();
                }
                else if (!remoteControlHandler.isMissionRunning() && previousMissionState){
                    ((Button) getView().findViewById(R.id.manualButton))
                            .setText("Running Manually");

                    //Reset progress on seek bar, so last value is not latched
                    resetProgress();

                    //Enable seek bars
                    speedSeekBar.setEnabled(true);
                    turnSeekBar.setEnabled(true);

                    previousMissionState = remoteControlHandler.isMissionRunning();
                }
                if(remoteControlHandler.isEnabled() && !previousEnableState) {
                    ((Button) getView().findViewById(R.id.enableButton)).setText("Stop");
                    getView().findViewById(R.id.enableButton).setBackgroundColor(0xffcc0000);

                    previousEnableState = remoteControlHandler.isEnabled();
                }
                else if (!remoteControlHandler.isEnabled() && previousEnableState){
                    ((Button) getView().findViewById(R.id.enableButton)).setText("Start");
                    getView().findViewById(R.id.enableButton).setBackgroundColor(0xff669900);

                    //Coming into stopped state, reset progress and motor commands.
                    resetProgress();

                    previousEnableState = remoteControlHandler.isEnabled();
                }


                TextView desiredBearing = (TextView) getView().findViewById(R.id.desiredBearing);
                desiredBearing.setText("Waypoint Bearing: "
                        + (float) (Math.toDegrees(remoteControlHandler.getDesiredBearing())));

                TextView actualBearing = (TextView) getView().findViewById(R.id.currentBearing);
                actualBearing.setText("Current Bearing: "
                        + (float) (Math.toDegrees(remoteControlHandler.getActualBearing())));

                TextView control = (TextView) getView().findViewById(R.id.effort);
                control.setText("Effort: " + (float) remoteControlHandler.getControlEffort());

                TextView waypoint = (TextView) getView().findViewById(R.id.waypointDistance);
                waypoint.setText("Distance: " + (float) remoteControlHandler.getDistanceToNext());

            }
        }
    };
}

