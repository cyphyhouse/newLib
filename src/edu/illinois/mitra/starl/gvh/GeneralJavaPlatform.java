package edu.illinois.mitra.starl.gvh;

import edu.illinois.mitra.starl.interfaces.TrackedRobot;
import edu.illinois.mitra.starl.motion.ReachAvoid;
import edu.illinois.mitra.starl.motion.RobotMotion;

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
