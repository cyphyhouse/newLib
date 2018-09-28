package edu.illinois.mitra.cyphyhouse.motion;


import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.RobotEventListener;
import edu.illinois.mitra.cyphyhouse.models.Model_iRobot;
import edu.illinois.mitra.cyphyhouse.objects.Common;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;


import edu.illinois.mitra.cyphyhouse.ros.JavaRosWrapper;

import java.util.Arrays;


/**
 * Motion controller which extends the RobotMotion abstract class. Capable of
 * going to destination waypoints and turning to face waypoints using custom
 * motion parameters. Includes optional collision avoidance which is controlled
 * by the motion parameters setting.
 * <p>
 * behavior: marks the unknown obstacle when collide, redo path planning (get around the obstacle)to reach the goal
 * 1: explore the area robot
 * behavior: explore the shape of the unknown obstacle and sent out the shape to others
 * 2: random moving obstacle robot
 * behavior:acts as simple moving obstacle
 * 3: anti goal robot
 * behavior:acts as AI opponent try to block robots getting to the goal
 * <p>
 * Behavior of different kind of robots are defined here
 *
 * @author Adam Zimmerman, Yixiao Lin
 * @version 1.1
 */
public class MotionAutomaton_iRobot extends RobotMotion {
    protected static final String TAG = "MotionAutomaton";
    protected static final String ERR = "Critical Error";
    public String name;

    private JavaRosWrapper wrapper;


    // MOTION CONTROL CONSTANTS
    //	public static int R_arc = 700;
//	public static int R_slowfwd = 700;
//	public static int A_smallturn = 3;
//	public static int A_straight = 6;
//	public static int A_arc = 25;
//	public static int A_arcexit = 30;
//	public static final int param.SLOWTURN_ANGLE = 25;
//	public static final int ROBOT_RADIUS = 180;

    // DELAY BETWEEN EACH RUN OF THE AUTOMATON
//	private static final int AUTOMATON_PERIOD = 60;
//	public static final int SAMPLING_PERIOD = 300;

    // COLLISION AVOIDANCE CONSTANTS
//	public static final int COLLISION_STRAIGHTTIME = 1250;

    public GlobalVarHolder gvh;

    // Motion tracking
    protected ItemPosition destination;
    private Model_iRobot mypos;
    private ItemPosition blocker;
    private ObstacleList obsList;

    //private ReachAvoid RA;


    protected enum STAGE {
        INIT, ARCING, STRAIGHT, TURN, SMALLTURN, GOAL, UNABLE
    }

    private STAGE next = null;
    protected STAGE stage = STAGE.INIT;
    private STAGE prev = null;
    protected boolean running = false;
    boolean colliding = false;

    private enum OPMODE {
        GO_TO, TURN_TO
    }

    private OPMODE mode = OPMODE.GO_TO;

    private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
    private volatile MotionParameters param = DEFAULT_PARAMETERS;
    //need to pass some more parameteres into this param
//	MotionParameters.Builder settings = new MotionParameters.Builder();


//	private volatile MotionParameters param = settings.build();


    // Collision avoidance
    private enum COLSTAGE {
        TURN, STRAIGHT
    }

    private COLSTAGE colprev = null;
    private COLSTAGE colstage = COLSTAGE.TURN;
    private COLSTAGE colnext = null;

    private enum COLSTAGE0 {
        BACK, STRAIGHT
    }

    private COLSTAGE0 colprev0 = null;
    private COLSTAGE0 colstage0 = COLSTAGE0.STRAIGHT;
    private COLSTAGE0 colnext0 = null;

    private enum COLSTAGE1 {
        BACK, STRAIGHT, TURN, SMALLARC
    }

    private COLSTAGE1 colprev1 = null;
    private COLSTAGE1 colstage1 = COLSTAGE1.STRAIGHT;
    private COLSTAGE1 colnext1 = null;


    private enum COLSTAGE2 {
        BACK, RANDOM, STRAIGHT
    }

    private COLSTAGE2 colprev2 = null;
    private COLSTAGE2 colstage2 = COLSTAGE2.BACK;
    private COLSTAGE2 colnext2 = null;

    private int col_straightime = 0;
    private int col_backtime = 0;
    private int col_turntime = 0;
    private int RanAngle = 0;
    private double linspeed;
    private double turnspeed;


    public MotionAutomaton_iRobot(GlobalVarHolder gvh) {
        super(gvh.id.getName());
        //name = gvh.id.getName();
        name = "IROBOT!!!!";
        this.gvh = gvh;
        this.linspeed = (param.LINSPEED_MAX - param.LINSPEED_MIN) / (double) (param.SLOWFWD_RADIUS - param.GOAL_RADIUS);
        this.turnspeed = (param.TURNSPEED_MAX - param.TURNSPEED_MIN) / (param.SLOWTURN_ANGLE - param.SMALLTURN_ANGLE);
        wrapper = new JavaRosWrapper("ws://localhost:9090", name, this.gvh, "iRobot");
        //wrapper.subscribe_to_ROS("point_msgs", "Waypoint");

        wrapper.subscribe_to_ROS("decaPos", "DecaWave");
    }


    public void goTo(ItemPosition dest, ObstacleList obsList) {
        if ((inMotion && !this.destination.equals(dest)) || !inMotion) {
            this.destination = new ItemPosition(dest.name, dest.x, dest.y, 0);
//            Log.d(TAG, "Going to X: " + Integer.toString(dest.x) + " Y: " + Integer.toString(dest.y));
            //this.destination = dest;
            this.mode = OPMODE.GO_TO;
            this.obsList = obsList;
            startMotion();
        }


        //RA.doReachAvoid(dest, dest, obsList);
    }

    public void goTo(ItemPosition dest) {
        //System.out.println("DOING GOTO\n");
        //Scanner in = new Scanner(((Model_iRobot)gvh.gps.getMyPosition()).name).useDelimiter("[^0-9]+");
        //int index = in.nextInt();
        //System.out.println("DOING GOTO1\n");
        //Vector<ObstacleList> temp = gvh.gps.getViews();
        ObstacleList obsList = new ObstacleList();
        System.out.println("DOING GOTO2\n");
        wrapper.createTopic("Waypoint");
        wrapper.sendMsg(dest);
        Model_iRobot model = (Model_iRobot) gvh.gps.getMyPosition();
        if (name.matches("iRobot0")) {
            System.out.println("Retrieved value " + model.TESTX + " from gvh: " + name);
        }

        goTo(dest, obsList);


		



        /*if(!temp.isEmpty()) {
            obsList = temp.elementAt(index);
        }
        else {
            obsList = new ObstacleList();
        }
        //obsList = new ObstacleList();
		// work in progress here
		goTo(dest, obsList);*/
    }

    public void turnTo(ItemPosition dest) {
        if ((inMotion && !this.destination.equals(dest)) || !inMotion) {
            this.destination = dest;
            this.mode = OPMODE.TURN_TO;
            startMotion();
        }
    }

    @Override
    public synchronized void start() {
        super.start();
        gvh.log.d(TAG, "STARTED!");
    }

    @Override
    public void run() {
        System.out.println("starting run\n");
        super.run();
        gvh.threadCreated(this);

        while (true) {
            //System.out.println("running\n");
//			gvh.gps.getObspointPositions().updateObs();
            if (running) {
                //Notice: interesting here....
                // why is getModel being used? Think it should be get position.
                //mypos = (Model_iRobot)gvh.plat.getModel();


                mypos = (Model_iRobot) gvh.gps.getMyPosition();
                int distance = mypos.distanceTo(destination);
                int angle = mypos.angleTo(destination);
                int absangle = Math.abs(angle);
                switch (param.COLAVOID_MODE) {
                    case BUMPERCARS:
                        colliding = false;
                        break;
                    case USE_COLAVOID:
                        colliding = collision_mem_less();
                        break;
                    case USE_COLBACK:
                        colliding = collision();
                        break;
                    case STOP_ON_COLLISION:
                        colliding = collision();
                        break;
                    default:
                        colliding = false;
                        break;
                }

                if ((mypos.type == 3) || (mypos.type == 2)) {
                    stage = null;
                    next = null;
                    colliding = true;
                }

                if (!colliding && stage != null) {
                    if (stage != prev)
                        gvh.log.e(TAG, "Stage is: " + stage.toString());
                    if (distance <= param.GOAL_RADIUS) {
                        next = STAGE.GOAL;
                    }
                    switch (stage) {
                        case INIT:

                            //System.out.println("NOT REACH AVOID");
                            //System.out.println("\n");

                            done = false;
                            if (mode == OPMODE.GO_TO) {
                                if (param.ENABLE_ARCING && distance <= param.ARC_RADIUS && absangle <= param.ARCANGLE_MAX) {
                                    next = STAGE.ARCING;
                                } else {
                                    next = STAGE.TURN;
                                }
                            } else {
                                next = STAGE.TURN;
                            }
                            break;
                        case ARCING:
                            // If this is the first run of ARCING, begin the arc
                            if (stage != prev) {
                                int radius = curveRadius();
                                curve(param.ARCSPEED_MAX, radius);
                            } else {
                                // Otherwise, check exit conditions
                                if (absangle > param.ARC_EXIT_ANGLE)
                                    next = STAGE.TURN;
                                if (absangle < param.STRAIGHT_ANGLE)
                                    next = STAGE.STRAIGHT;
                            }
                            break;
                        case STRAIGHT:
                            if (stage != prev) {
                                straight(LinSpeed(distance));
                            } else {
                                if (Common.inRange(distance, param.GOAL_RADIUS, param.SLOWFWD_RADIUS))
                                    straight(LinSpeed(distance));
                                if (Common.inRange(absangle, param.SMALLTURN_ANGLE, param.ARCANGLE_MAX))
                                    next = STAGE.SMALLTURN;
                                if (absangle > param.ARCANGLE_MAX)
                                    next = STAGE.TURN;
                            }
                            break;
                        case TURN:
                            if (stage != prev) {
                                turn(TurnSpeed(absangle), angle);
                            } else {
                                if (absangle <= param.SMALLTURN_ANGLE) {
                                    gvh.log.i(TAG, "Turn stage: within angle bounds!");
                                    next = (mode == OPMODE.GO_TO) ? STAGE.STRAIGHT : STAGE.GOAL;
                                } else if (absangle <= param.SLOWTURN_ANGLE) {
                                    // Resend a reduced-speed turn command if we're
                                    // within the slow-turn window
                                    turn(TurnSpeed(absangle), angle);
                                }
                            }
                            break;
                        case SMALLTURN:
                            if (stage != prev) {
                                int radius = curveRadius() / 2;
                                curve(LinSpeed(distance), radius);
                            } else {
                                if (absangle <= param.SMALLTURN_ANGLE)
                                    next = STAGE.STRAIGHT;
                                if (absangle > param.ARCANGLE_MAX)
                                    next = STAGE.TURN;
                            }
                            break;
                        case GOAL:
                            done = true;
                            gvh.log.i(TAG, "At goal, done flag set!");
                            if (param.STOP_AT_DESTINATION)
                                straight(0);
                            running = false;
                            inMotion = false;
                            break;
                        case UNABLE:
                            System.out.println("motionautomation inactive");
                            gvh.log.i(TAG, "motion could not reach dest");
                            straight(0);
                            done = false;
                            running = false;
                            inMotion = false;
                            break;
                    }

                    prev = stage;
                    if (next != null) {
                        stage = next;
                        gvh.log.i(TAG, "Stage transition to " + stage.toString());
                        gvh.trace.traceEvent(TAG, "Stage transition", stage.toString(), gvh.time());
                    }
                    next = null;
                }

                if ((colliding || stage == null)) {
                    switch (param.COLAVOID_MODE) {
                        case USE_COLAVOID:
                            use_colavoid();
                            break;
                        case USE_COLBACK:
                            use_colback();
                            break;
                        case STOP_ON_COLLISION:
                            if (stage != null) {
                                gvh.log.d(TAG, "Imminent collision detected!");
                                straight(0);
                                stage = STAGE.UNABLE;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            gvh.sleep(param.AUTOMATON_PERIOD);
        }
    }


    private void use_colavoid() {
        if (stage != null) {
            gvh.log.d(TAG, "Imminent collision detected!");
            stage = null;
            straight(0);
            colnext = null;
            colprev = null;
            colstage = COLSTAGE.TURN;
        }
        switch (colstage) {
            case TURN:
                if (colstage != colprev) {
                    gvh.log.d(TAG, "Colliding: sending turn command");
                    turn(param.TURNSPEED_MAX, -1 * mypos.angleTo(blocker));

                }

                if (!colliding) {

                    colnext = COLSTAGE.STRAIGHT;

                } else {
                    gvh.log.d(TAG, "colliding with " + blocker.name + " - " + mypos.isFacing(blocker) + " - " + mypos.distanceTo(blocker));
                }
                break;
            case STRAIGHT:
                if (colstage != colprev) {
                    gvh.log.d(TAG, "Colliding: sending straight command");
                    straight(param.LINSPEED_MAX);
                    col_straightime = 0;
                } else {
                    col_straightime += param.AUTOMATON_PERIOD;
                    // If a collision is imminent (again), return to the
                    // turn stage
                    if (colliding) {
                        gvh.log.d(TAG, "Collision imminent! Cancelling straight stage");
                        straight(0);
                        colnext = COLSTAGE.TURN;
                    }
                    // If we're collision free and have been for enough
                    // time, restart normal motion
                    if (!colliding && col_straightime >= param.COLLISION_AVOID_STRAIGHTTIME) {
                        gvh.log.d(TAG, "Free! Returning to normal execution");
                        colprev = null;
                        colnext = null;
                        colstage = null;
                        stage = STAGE.INIT;
                    }
                }
                break;
        }
        colprev = colstage;
        if (colnext != null) {
            colstage = colnext;
            gvh.log.i(TAG, "Advancing stage to " + colnext);
        }
        colnext = null;
        return;
    }

    private void use_colback() {
        switch (mypos.type) {
            case 0:
                goalbot();
                break;
            case 1:
                discoverbot();
                break;
            case 2:
                badbot();
                break;
            case 3:
                break;
        }
    }


    private void goalbot() {
        if (stage != null) {
            gvh.log.d(TAG, "Imminent collision detected!");
            stage = null;
            straight(0);
            colnext0 = null;
            colprev0 = null;
            colstage0 = COLSTAGE0.BACK;
        }

        switch (colstage0) {
            case BACK:
                col_backtime += param.AUTOMATON_PERIOD;
                straight(-param.LINSPEED_MAX / 2);
                if (col_backtime > param.COLLISION_AVOID_BACKTIME) {
                    col_backtime = 0;
                    straight(0);
                    colprev0 = null;
                    colnext0 = null;
                    colstage0 = null;
                    stage = STAGE.UNABLE;
                }
                break;
            case STRAIGHT:
                straight(param.LINSPEED_MAX);
                if (colliding) {
                    gvh.log.d(TAG, "Collision imminent! Cancelling straight stage");
                    straight(0);
                    colnext0 = COLSTAGE0.BACK;
                }
                break;
        }
        colprev0 = colstage0;
        if (colnext0 != null) {
            colstage0 = colnext0;
            gvh.log.i(TAG, "Advancing stage to " + colnext);
        }
        colnext0 = null;

    }

    private void discoverbot() {
        if (stage != null) {
            gvh.log.d(TAG, "Imminent collision detected!");
            stage = null;
            straight(0);
            colnext1 = null;
            colprev1 = null;
            colstage1 = COLSTAGE1.BACK;
        }

        switch (colstage1) {
            case BACK:
                col_backtime += param.AUTOMATON_PERIOD;
                if (col_backtime > param.COLLISION_AVOID_BACKTIME) {
                    col_backtime = 0;
                    straight(0);
                    colnext1 = COLSTAGE1.TURN;
                } else
                    straight(-param.LINSPEED_MAX / 2);
                break;
            case STRAIGHT:
                col_straightime += param.AUTOMATON_PERIOD;
                if (!colliding) {
                    if (col_straightime < param.COLLISION_AVOID_STRAIGHTTIME) {
                        straight(param.LINSPEED_MAX);
                    } else {
                        col_straightime = 0;
                        straight(0);
                        colprev1 = null;
                        colnext1 = null;
                        colstage1 = null;
                        stage = STAGE.UNABLE;
                    }
                } else {
                    gvh.log.d(TAG, "Collision imminent! Cancelling straight stage");
                    straight(0);
                    col_straightime = 0;
                    colnext1 = COLSTAGE1.BACK;
                }
                break;
            case TURN:
                col_turntime += param.AUTOMATON_PERIOD;
                if (col_turntime < param.COLLISION_AVOID_TURNTIME)
                    turn(param.TURNSPEED_MAX, 45);
                    //trun left when hit
                else {
                    col_turntime = 0;
                    colnext1 = COLSTAGE1.SMALLARC;
                }
                break;
            case SMALLARC:
                col_straightime += param.AUTOMATON_PERIOD;
                if (!colliding) {
                    if (col_straightime < param.COLLISION_AVOID_STRAIGHTTIME)
                        curve(param.LINSPEED_MAX, 320);
                    else {

                        colnext1 = COLSTAGE1.STRAIGHT;
                    }
                } else {
                    col_straightime = 0;
                    straight(0);
                    colnext1 = COLSTAGE1.BACK;
                }
                break;
        }
        colprev1 = colstage1;
        if (colnext1 != null) {
            colstage1 = colnext1;
            gvh.log.i(TAG, "Advancing stage to " + colnext);
        }
        colnext1 = null;
    }

    private void badbot() {
        if (stage != null) {
            gvh.log.d(TAG, "Imminent collision detected!");
            stage = null;
            straight(0);
            colnext2 = null;
            colprev2 = null;
            colstage2 = COLSTAGE2.BACK;
        }
        switch (colstage2) {
            case BACK:
                col_backtime += param.AUTOMATON_PERIOD;
                if (col_backtime > param.COLLISION_AVOID_BACKTIME) {
                    col_backtime = 0;
                    colstage2 = COLSTAGE2.RANDOM;
                } else {
                    straight(-param.LINSPEED_MAX / 2);
                }
                break;
            case RANDOM:
                if (col_turntime == 0) {
                    RanAngle = (int) (-90 + (Math.random() * 180));
                }
                col_turntime += param.AUTOMATON_PERIOD;
                if (col_turntime < param.COLLISION_AVOID_TURNTIME)
                    turn(param.TURNSPEED_MAX, RanAngle);
                    //trun left when hit
                else {
                    col_turntime = 0;
                    colnext2 = COLSTAGE2.STRAIGHT;
                }

                break;
            case STRAIGHT:
                if (colliding) {
                    straight(0);
                    colnext2 = COLSTAGE2.BACK;
                } else {
                    straight(param.LINSPEED_MAX);
                }
                break;

        }
        colprev2 = colstage2;
        if (colnext2 != null) {
            colstage2 = colnext2;
            gvh.log.i(TAG, "Advancing stage to " + colnext);
        }
        colnext2 = null;
    }


    public void cancel() {
    }

    @Override
    public void motion_resume() {
        running = true;
    }

    @Override
    public void motion_stop() {

        running = false;
        stage = MotionAutomaton_iRobot.STAGE.INIT;
        destination = null;
        inMotion = false;

    }

    // Calculates the radius of curvature to meet a target
    private int curveRadius() {
        int x0 = mypos.x;
        int y0 = mypos.y;
        int x1 = destination.x;
        int y1 = destination.y;
        int theta = (int) mypos.angle;
        double alpha = -180 + Math.toDegrees(Math.atan2((y1 - y0), (x1 - x0)));
        double rad = -(Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2)) / (2 * Math.sin(Math.toRadians(alpha - theta))));
        return (int) rad;
    }

    private void startMotion() {
        running = true;
        stage = STAGE.INIT;
        inMotion = true;
    }

    protected void sendMotionEvent(int motiontype, int... argument) {
        // TODO: This might not be necessary
        gvh.trace.traceEvent(TAG, "Motion", Arrays.toString(argument), gvh.time());
        gvh.sendRobotEvent(RobotEventListener.Event.MOTION, motiontype);
    }

    protected void curve(int velocity, int radius) {
        if (running) {
            sendMotionEvent(Common.MOT_ARCING, velocity, radius);
            // TODO: Determine if angular velocity formula works!
//			gpsp.setVelocity(name, velocity, (int) Math.round((velocity*360.0)/(2*Math.PI*radius)));

        }
    }

    protected void straight(int velocity) {
        gvh.log.i(TAG, "Straight at velocity " + velocity);
        if (running) {
            if (velocity != 0) {
                sendMotionEvent(Common.MOT_STRAIGHT, velocity);
            } else {
                sendMotionEvent(Common.MOT_STOPPED, 0);
            }
//			gpsp.setVelocity(name, velocity, 0);

        }
    }

    protected void turn(int velocity, int angle) {
        if (running) {
            sendMotionEvent(Common.MOT_TURNING, velocity, angle);
//			gpsp.setVelocity(name, 0, (int) Math.copySign(velocity, -angle));

        }
    }

    // Ramp linearly from min at param.SMALLTURN_ANGLE to max at param.SLOWTURN_ANGLE
    public int TurnSpeed(int angle) {
        if (angle > param.SLOWTURN_ANGLE) {
            return param.TURNSPEED_MAX;
        } else if (angle > param.SMALLTURN_ANGLE && angle <= param.SLOWTURN_ANGLE) {
            return param.TURNSPEED_MIN + (int) ((angle - param.SMALLTURN_ANGLE) * turnspeed);
        } else {
            return param.TURNSPEED_MIN;
        }
    }

    /**
     * Slow down linearly upon coming within R_slowfwd of the goal
     *
     * @param distance
     * @return
     */
    private int LinSpeed(int distance) {
        if (distance > param.SLOWFWD_RADIUS)
            return param.LINSPEED_MAX;
        if (distance > param.GOAL_RADIUS && distance <= param.SLOWFWD_RADIUS) {
            return param.LINSPEED_MIN + (int) ((distance - param.GOAL_RADIUS) * linspeed);
        }
        return param.LINSPEED_MIN;
    }

    private boolean collision_mem_less() {
        if (mypos.leftbump || mypos.rightbump) {
            double ColPoint_x, ColPoint_y;
            if (mypos.leftbump && mypos.rightbump) {
                ColPoint_x = mypos.radius * (Math.cos(Math.toRadians(mypos.angle))) + mypos.x;
                ColPoint_y = mypos.radius * (Math.sin(Math.toRadians(mypos.angle))) + mypos.y;
                blocker = new ItemPosition("detected", (int) ColPoint_x, (int) ColPoint_y, 0);
            } else if (mypos.leftbump) {
                ColPoint_x = mypos.radius * (Math.cos(Math.toRadians(mypos.angle + 45))) + mypos.x;
                ColPoint_y = mypos.radius * (Math.sin(Math.toRadians(mypos.angle + 45))) + mypos.y;
                blocker = new ItemPosition("detected", (int) ColPoint_x, (int) ColPoint_y, 0);


            } else {
                ColPoint_x = mypos.radius * (Math.cos(Math.toRadians(mypos.angle - 45))) + mypos.x;
                ColPoint_y = mypos.radius * (Math.sin(Math.toRadians(mypos.angle - 45))) + mypos.y;
                blocker = new ItemPosition("detected", (int) ColPoint_x, (int) ColPoint_y, 0);
            }

            return true;
        } else
            return false;
    }

    private boolean collision() {
        boolean toreturn = collision_mem_less();
        if (toreturn)
            obsList.detected(blocker);
        return toreturn;
    }

    // Detects an imminent collision with another robot or with any obstacles

    @Override
    public void setParameters(MotionParameters param) {
        this.param = param;
        this.linspeed = (double) (param.LINSPEED_MAX - param.LINSPEED_MIN) / Math.abs((param.SLOWFWD_RADIUS - param.GOAL_RADIUS));
        this.turnspeed = (param.TURNSPEED_MAX - param.TURNSPEED_MIN) / (param.SLOWTURN_ANGLE - param.SMALLTURN_ANGLE);
    }
}
