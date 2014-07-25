package com.example.coolerbot.app;


import android.content.Context;

public class SabertoothDriver{

    private static final byte ADDRESS = (byte)0x80;
    private static final byte MOTOR_FORWARD_A = 0x00;
    private static final byte MOTOR_BACKWARD_A = 0x01;
    private static final byte MIN_VOLTAGE = 0x02;
    private static final byte MAX_VOLTAGE = 0x03;
    private static final byte MOTOR_FORWARD_B = 0x04;
    private static final byte MOTOR_BACKWARD_B = 0x05;
    private static final byte FORWARD_MIXED = 0x08;
    private static final byte BACKWARD_MIXED = 0x09;
    private static final byte RIGHT_MIXED = 0x0A;
    private static final byte LEFT_MIXED = 0x0B;
    private static final byte SERIAL_TIMEOUT = 0x0E;
    private static final byte BAUD_RATE = 0x0F;
    private static final byte RAMPING = 0x10;
    private static final byte DEADBAND = 0x11;

    private static final int baudRate = 9600;
    private static final byte dataBits = 0x08;
    private static final byte stopBits = 0x01;
    private static final byte parity = 0x00;
    private static final byte flowControl = 0x00;


    private FT311UARTInterface uartInterface;

    public SabertoothDriver(Context context) {
        super();

        uartInterface = new FT311UARTInterface(context, null);
        uartInterface.SetConfig(baudRate, dataBits, stopBits, parity, flowControl);
    }

    public void resumeDriver() {
        if (uartInterface.ResumeAccessory() == 2) {
            uartInterface.SetConfig(baudRate, dataBits, stopBits, parity, flowControl);
        }
    }

    public void destroyDriver() {
        uartInterface.DestroyAccessory(true);
    }

    public void setMotorForwardA(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = MOTOR_FORWARD_A;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)MOTOR_FORWARD_A + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setMotorBackwardA(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = MOTOR_BACKWARD_A;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)MOTOR_BACKWARD_A + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setMinVoltage(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = MIN_VOLTAGE;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)MIN_VOLTAGE + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setMaxVoltage(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = MAX_VOLTAGE;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)MAX_VOLTAGE + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setMotorForwardB(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = MOTOR_FORWARD_B;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)MOTOR_FORWARD_B + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setMotorBackwardB(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = MOTOR_BACKWARD_B;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)MOTOR_BACKWARD_B + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setForwardMixed(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = FORWARD_MIXED;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)FORWARD_MIXED + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setBackwardMixed(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = BACKWARD_MIXED;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)BACKWARD_MIXED + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setRightMixed(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = RIGHT_MIXED;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)RIGHT_MIXED + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setLeftMixed(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = LEFT_MIXED;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)LEFT_MIXED + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setSerialTimeout(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = SERIAL_TIMEOUT;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)SERIAL_TIMEOUT + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setBaudRate(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = BAUD_RATE;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)BAUD_RATE + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setRamping(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = RAMPING;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)RAMPING + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }

    public void setDeadband(byte command) {
        byte[] message = new byte[4];
        message[0] = ADDRESS;
        message[1] = DEADBAND;
        message[2] = command;
        int checkSum = ((int)ADDRESS & 0xFF) + (int)DEADBAND + (int)command;
        message[3] = (byte)(checkSum & 127);

        uartInterface.SendData(4, message);
    }
}
