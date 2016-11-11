package edu.illinois.mitra.cyphyhouse.gvh;

import edu.illinois.mitra.cyphyhouse.interfaces.TrackedRobot;
import edu.illinois.mitra.cyphyhouse.motion.ReachAvoid;
import edu.illinois.mitra.cyphyhouse.motion.RobotMotion;

/**
 * Created by SC on 11/2/16.
 */
public class GeneralJavaPlatform {
    public ReachAvoid reachAvoid;

    public RobotMotion moat;

    public TrackedRobot model;

    public TrackedRobot getModel() {
        return model;
    }
}
