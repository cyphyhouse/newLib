package edu.illinois.mitra.cyphyhouse.motion;


import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.models.Model_Car;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.ros.JavaRosWrapper;


public class MotionAutomaton_Car extends RobotMotion {
    protected static final String TAG = "MotionAutomaton";
    protected static final String ERR = "Critical Error";

    private String name;

    private JavaRosWrapper wrapper;


    public GlobalVarHolder gvh;

    public boolean reached = false;

    // Motion tracking
    protected ItemPosition destination;
    private Model_Car mymodel;
    private ItemPosition blocker;
    private ObstacleList obsList;


    protected enum STAGE {
        MOVING, GOAL, INIT
    }

    private STAGE next = null;
    protected STAGE stage = STAGE.INIT;
    private STAGE prev = null;
    protected boolean running = false;
    boolean colliding = false;
    private boolean reached_goal = false;


    private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
    private volatile MotionParameters param = DEFAULT_PARAMETERS;


    public MotionAutomaton_Car(GlobalVarHolder gvh) {
        super(gvh.id.getName());
        name = gvh.id.getName();
        this.gvh = gvh;
        this.mymodel = (Model_Car) gvh.gps.getMyPosition();
        wrapper = new JavaRosWrapper("ws://localhost:9090", name, this.gvh, "Car");
        wrapper.subscribe_to_ROS("Reached", "Reached Message");
        reached = false;

    }

    public void goTo(ItemPosition dest, ObstacleList obsList) {
        if ((inMotion && !this.destination.equals(dest)) || !inMotion) {
            this.destination = new ItemPosition(dest.name, dest.x, dest.y, 0);
//            Log.d(TAG, "Going to X: " + Integer.toString(dest.x) + " Y: " + Integer.toString(dest.y));
            //this.destination = dest;
            this.obsList = obsList;
            System.out.println("about to start motion\n");
            startMotion();
        }


    }

    public void goTo(ItemPosition dest) {

        System.out.println("running car motionautomaton");
        wrapper.createTopic("Waypoint");
        wrapper.sendMsg(dest);

        startMotion();

        return;
        //goTo(dest, obsList);
        //Model_iRobot model = (Model_iRobot)gvh.gps.getMyPosition();
        //if(name.matches("iRobot0")){
        //	System.out.println("Retrieved value " + model.TESTX + " from gvh: " + name);
        //}


		



       /* if(!temp.isEmpty()) {
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
        super.run();
        gvh.threadCreated(this);

        while (true) {
//			gvh.gps.getObspointPositions().updateObs();
            if (running) {
                //Notice: interesting here....
                // why is getModel being used? Think it should be get position.
                //mypos = (Model_iRobot)gvh.plat.getModel();
                //System.out.println("Doing Motion\n");
                //reached_goal = true;

                if (reached == true) {
                    inMotion = false;
                }
                switch (stage) {
                    case MOVING:
                        break;

                    case GOAL:
                        if (mymodel.reached == true) {
                            running = false;
                            mymodel.reached = false;
                            reached_goal = true;
                        }
                        break;

                }


            }
            gvh.sleep(param.AUTOMATON_PERIOD);
        }
    }


    @Override
    public void cancel() {
    }


    @Override
    public void motion_resume() {
        running = true;
    }

    @Override
    public void motion_stop() {

        running = false;
        stage = MotionAutomaton_Car.STAGE.INIT;
        destination = null;
        inMotion = false;

    }


    private void startMotion() {

        running = true;

        //stage = STAGE.MOVING;
        inMotion = true;

        reached_goal = false;

        reached = false;
        return;
    }

    // Detects an imminent collision with another robot or with any obstacles

    @Override
    public void setParameters(MotionParameters param) {
        this.param = param;

    }


    //ADD IMPLEMENTATION OF ROBOT EVENTS///////////////////
    // EVENTS: Event {MOTION, GPS, GPS_SELF, WAYPOINT_RECEIVED};


}














	
