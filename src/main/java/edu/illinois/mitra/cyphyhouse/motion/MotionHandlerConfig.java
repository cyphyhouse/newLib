package edu.illinois.mitra.cyphyhouse.motion;

/**
 * Created by SC on 3/16/17.
 * @brief: Mainly for the parameters of "what" field in the IPCMessage <br>
 * Note that this is a static class
 */
public final class MotionHandlerConfig {
    private MotionHandlerConfig(){}

    public static final int CMD_DRONE_TAKEOFF=0;
    public static final int CMD_DRONE_LAND=1;
    public static final int CMD_DRONE_HOVER=2;
    public static final int CMD_IROBOT_MOTION_STOP=10;
    public static final int CMD_IROBOT_CURVE=11;
    public static final int CMD_IROBOT_STRAIGHT=12;
    public static final int CMD_IROBOT_TURN=13;
}