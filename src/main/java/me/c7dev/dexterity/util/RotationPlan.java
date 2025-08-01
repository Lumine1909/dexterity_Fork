package me.c7dev.dexterity.util;

/**
 * Simple class to store all the possible amounts in degrees for each axis to run in a single rotation
 */
public class RotationPlan {

    public double xDeg = 0, yDeg = 0, zDeg = 0, yawDeg = 0, pitchDeg = 0, rollDeg = 0;
    public boolean setX = false, setY = false, setZ = false, setYaw = false, setPitch = false, setRoll = false, reset = false, async = true;

    public RotationPlan() {

    }

    public RotationPlan clone() {
        RotationPlan r = new RotationPlan();
        r.xDeg = xDeg;
        r.yDeg = yDeg;
        r.zDeg = zDeg;
        r.yawDeg = yawDeg;
        r.pitchDeg = pitchDeg;
        r.rollDeg = rollDeg;
        r.setX = setX;
        r.setY = setY;
        r.setZ = setZ;
        r.setY = setYaw;
        r.setPitch = setPitch;
        r.setRoll = setRoll;
        r.reset = reset;
        r.async = async;
        return r;
    }
}
