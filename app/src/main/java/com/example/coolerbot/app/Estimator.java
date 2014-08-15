package com.example.coolerbot.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.Timer;
import java.util.TimerTask;

public class Estimator implements SensorEventListener {

    private EstimatorEventListener estimatorEvent;
    private SensorManager sensorManager;
    private Handler sensorHandler;

    private float[] gyro = new float[3];
    private float[] gyroMatrix = new float[9];
    private float[] gyroOrientation = new float[3];
    private float[] mag = new float[3];
    private float[] gravity = new float[3];
    private float[] accelMagOrientation = new float[3];
    private float[] fusedOrientation = new float[3];
    private float[] rotationMatrix = new float[9];

    private float timeStampGyro;
    private boolean initialized = false;
    private boolean accelMagAvaiable = false;

    private static final int TIME_CONSTANT = 30;
    private static final float ORIENTATION_COEFF = 0.98f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float EPSILON = 0.000000001f;

    private Timer filterTimer = new Timer();

    public Estimator(Context context, EstimatorEventListener estimatorEvent) {

        this.estimatorEvent = estimatorEvent;

        HandlerThread handlerThread = new HandlerThread("sensorThread");
        handlerThread.start();

        sensorHandler = new Handler(handlerThread.getLooper());

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        registerListeners();

        filterTimer.scheduleAtFixedRate(new FuseTask(), 1000, TIME_CONSTANT);
    }

    public float getActualBearing() {
        return fusedOrientation[0];
    }

    public void registerListeners() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST, sensorHandler);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_FASTEST, sensorHandler);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST, sensorHandler);
    }

    public void unregisterListeners() {
        sensorManager.unregisterListener(this);
    }

    private void calcGyroOrientation(SensorEvent sensorEvent) {
        if(accelMagAvaiable == false) return;

        if(!initialized){
            gyroOrientation[0] = accelMagOrientation[0];
            gyroOrientation[1] = accelMagOrientation[1];
            gyroOrientation[2] = accelMagOrientation[2];

            gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
            gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
            gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

            gyroMatrix = matrixMultiplication(gyroMatrix, rotationMatrix);
            initialized = true;
        }

        float[] deltaVector = new float[4];
        if(timeStampGyro != 0) {
            float dt = (sensorEvent.timestamp - timeStampGyro) * NS2S;
            getGyroRotationVector(gyro, deltaVector, dt/2.0f);
        }

        timeStampGyro = sensorEvent.timestamp;

        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private void calcAccelMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, gravity, mag)) {
            SensorManager.getOrientation(rotationMatrix, accelMagOrientation);
            accelMagAvaiable = true;
        }
    }

    private void getGyroRotationVector(float[] gyroValues, float[] deltaRotationVector,
                                       float timeFactor)
    {
        float[] normValues = new float[3];

        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    private void updateOrientationFilter() {
        if(!initialized) return;

        float filterCoeffComp = 1.0f - ORIENTATION_COEFF;

        if (gyroOrientation[0] < -0.5 * Math.PI && accelMagOrientation[0] > 0.0) {
            fusedOrientation[0] = (float) (ORIENTATION_COEFF * (gyroOrientation[0] + 2.0 * Math.PI) + filterCoeffComp * accelMagOrientation[0]);
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (accelMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
            fusedOrientation[0] = (float) (ORIENTATION_COEFF * gyroOrientation[0] + filterCoeffComp * (accelMagOrientation[0] + 2.0 * Math.PI));
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else {
            fusedOrientation[0] = ORIENTATION_COEFF * gyroOrientation[0] + filterCoeffComp * accelMagOrientation[0];
        }

        if (gyroOrientation[1] < -0.5 * Math.PI && accelMagOrientation[1] > 0.0) {
            fusedOrientation[1] = (float) (ORIENTATION_COEFF * (gyroOrientation[1] + 2.0 * Math.PI) + filterCoeffComp * accelMagOrientation[1]);
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (accelMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
            fusedOrientation[1] = (float) (ORIENTATION_COEFF * gyroOrientation[1] + filterCoeffComp * (accelMagOrientation[1] + 2.0 * Math.PI));
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else {
            fusedOrientation[1] = ORIENTATION_COEFF * gyroOrientation[1] + filterCoeffComp * accelMagOrientation[1];
        }

        if (gyroOrientation[2] < -0.5 * Math.PI && accelMagOrientation[2] > 0.0) {
            fusedOrientation[2] = (float) (ORIENTATION_COEFF * (gyroOrientation[2] + 2.0 * Math.PI) + filterCoeffComp * accelMagOrientation[2]);
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (accelMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
            fusedOrientation[2] = (float) (ORIENTATION_COEFF * gyroOrientation[2] + filterCoeffComp * (accelMagOrientation[2] + 2.0 * Math.PI));
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else {
            fusedOrientation[2] = ORIENTATION_COEFF * gyroOrientation[2] + filterCoeffComp * accelMagOrientation[2];
        }

        gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
        System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);

        estimatorEvent.onEstimatorUpdate(fusedOrientation);
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_GRAVITY:
                System.arraycopy(sensorEvent.values, 0, gravity, 0, 3);
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(sensorEvent.values, 0, gyro, 0, 3);
                calcGyroOrientation(sensorEvent);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(sensorEvent.values, 0, mag, 0, 3);
                calcAccelMagOrientation();
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    private class FuseTask extends TimerTask {
        @Override
        public void run() {
            updateOrientationFilter();
        }
    }

    public interface EstimatorEventListener {
        public void onEstimatorUpdate(float[] fusedData);
    }
}