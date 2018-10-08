package edu.illinois.mitra.cyphyhouse.motion;

import java.util.*;

import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.RobotEventListener.Event;
import edu.illinois.mitra.cyphyhouse.models.Model_quadcopter;
import edu.illinois.mitra.cyphyhouse.objects.*;

/**
 * This motion controller is for quadcopter models only
 * 
 * Motion controller which extends the RobotMotion abstract class. Capable of
 * going to destination or passing through a destination without stopping.
 * Includes optional collision avoidance which is controlled
 * by the motion parameters setting.
 *
 * 2017-2-9 Move to general Java env
 *  
 * @author Yixiao Lin, Shuchen
 * @version 1.1
 */
public class MotionAutomation_quadcopter_Base extends RobotMotion {
	protected static final String TAG = "MotionAutomaton";
	protected static final String ERR = "Critical Error";
	final int safeHeight = 500;

	protected GlobalVarHolder gvh;

	// Motion tracking
	protected ItemPosition destination;
	private Model_quadcopter mypos;


	protected enum STAGE {
		INIT, MOVE, HOVER, TAKEOFF, LAND, GOAL, STOP
	}

	private STAGE next = null;
	protected STAGE stage = STAGE.INIT;
	private STAGE prev = null;
	protected boolean running = false;
	boolean colliding = false;

	private enum OPMODE {
		GO_TO
	}

	private OPMODE mode = OPMODE.GO_TO;

	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	private volatile MotionParameters param = DEFAULT_PARAMETERS;
	//need to pass some more parameteres into this param
	//	MotionParameters.Builder settings = new MotionParameters.Builder();


	//	private volatile MotionParameters param = settings.build();

	public MotionAutomation_quadcopter_Base(GlobalVarHolder gvh) {
		super(gvh.id.getName());
		this.gvh = gvh;

	}

	public void goTo(ItemPosition dest, ObstacleList obsList) {
		goTo(dest);
	}

	public void goTo(ItemPosition dest) {
		if((inMotion && !this.destination.equals(dest)) || !inMotion) {
			done = false;
			this.destination = new ItemPosition(dest.name,dest.x,dest.y,dest.z);
			//this.destination = dest;
			this.mode = OPMODE.GO_TO;
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
		// some control parameters
		double kpx,kpy,kpz, kdx,kdy,kdz;
		kpx = kpy = kpz = 0.00030; //used to be 0.00033
		kdx = kdy = kdz = 0.00075;  //used to be 0.0006
		while(true) {
			//			gvh.gps.getObspointPositions().updateObs();
			if(running) {
				mypos = (Model_quadcopter)gvh.plat.getModel();
//				System.out.println(mypos.toString());
				int distance = (int) Math.sqrt(Math.pow((mypos.x - destination.x),2) + Math.pow((mypos.y - destination.y), 2)); 
				//int distance = mypos.distanceTo(destination);		
				if(mypos.gaz < -50){
			//		System.out.println("going down");
				}
				colliding = (stage != STAGE.LAND && mypos.gaz < -50);

				if(!colliding && stage != null) {

					switch(stage) {
					case INIT:
						if(mode == OPMODE.GO_TO) {
							if(mypos.z < safeHeight){
								// just a safe distance from ground
								takeOff();
								next = STAGE.TAKEOFF;
							}
							else{
								if(distance <= param.GOAL_RADIUS) {
									next = STAGE.GOAL;
								}
								else{
									next = STAGE.MOVE;
								}
							}
						}	
						break;
					case MOVE:
						if(mypos.z < safeHeight){
							// just a safe distance from ground
							takeOff();
							next = STAGE.TAKEOFF;
							break;
						}
						if(distance <= param.GOAL_RADIUS) {
							next = STAGE.GOAL;
						}
						else{
							double Ax_d, Ay_d = 0.0;
							double Ryaw, Rroll, Rpitch, Rvs, Ryawsp = 0.0;
							//		System.out.println(destination.x - mypos.x + " , " + mypos.v_x);
							Ax_d = (kpx * (destination.x - mypos.x) - kdx * mypos.v_x) ;
							Ay_d = (kpy * (destination.y - mypos.y) - kdy * mypos.v_y) ;
							Ryaw = Math.atan2(destination.y - mypos.y, destination.x - mypos.x);
							//Ryaw = Math.atan2((destination.y - mypos.x), (destination.x - mypos.y));
							Ryawsp = kpz * ((Ryaw - Math.toRadians(mypos.yaw)));
							Rroll = Math.asin((Ay_d * Math.cos(Math.toRadians(mypos.yaw)) - Ax_d * Math.sin(Math.toRadians(mypos.yaw))) %1);
							Rpitch = Math.asin( (-Ay_d * Math.sin(Math.toRadians(mypos.yaw)) - Ax_d * Math.cos(Math.toRadians(mypos.yaw))) / (Math.cos(Rroll)) %1);
							Rvs = (kpz * (destination.z - mypos.z) - kdz * mypos.v_z);
						//	System.out.println(Ryaw + " , " + Ryawsp + " , " +  Rroll  + " , " +  Rpitch + " , " + Rvs);

							setControlInputRescale(Math.toDegrees(Ryawsp),Math.toDegrees(Rpitch)%360,Math.toDegrees(Rroll)%360,Rvs);
							//setControlInput(Ryawsp/param.max_yaw_speed, Rpitch%param.max_pitch_roll, Rroll%param.max_pitch_roll, Rvs/param.max_gaz);
							//next = STAGE.INIT;
						}
						break;
					case HOVER:
						setControlInput(0,0,0, 0);
						break;
					case TAKEOFF:
						switch(mypos.z/(safeHeight/2)){
						case 0:// 0 - 1/2 safeHeight
							setControlInput(0,0,0,1);
							break;
						case 1: // 1/2- 1 safeHeight
							setControlInput(0,0,0, 0.5);
							break;
						default: // above safeHeight:
							hover();
							if(prev != null){
								next = prev;
							}
							else{
								next = STAGE.HOVER;
							}
							break;
						}
						break;
					case LAND:
						switch(mypos.z/(safeHeight/2)){
						case 0:// 0 - 1/2 safeHeight
							setControlInput(0,0,0,0);
							next = STAGE.STOP;
							break;
						case 1: // 1/2- 1 safeHeight
							setControlInput(0,0,0, -0.05);
							break;
						default:   // above safeHeight
							setControlInput(0,0,0,-0.5);
							break;
						}
						break;
					case GOAL:
						done = true;
						gvh.log.i(TAG, "At goal!");
						gvh.log.i("DoneFlag", "write");

						/*if(param.STOP_AT_DESTINATION){
							hover();
							next = STAGE.HOVER;
						}*/

						/* this keeps quadcopter hovering over the point once its reached its destination instead of
						   moving off the screen at a constant speed
						 */

						//System.out.println("dest is: " + destination.x + " " + destination.y);
						//System.out.println("vel: " + mypos.v_x + " " + mypos.v_y);
						double Ax_d, Ay_d = 0.0;
						double Ryaw, Rroll, Rpitch, Rvs, Ryawsp = 0.0;
						//		System.out.println(destination.x - mypos.x + " , " + mypos.v_x);
						Ax_d = (kpx * (destination.x - mypos.x) - kdx * mypos.v_x) ;
						Ay_d = (kpy * (destination.y - mypos.y) - kdy * mypos.v_y) ;
						Ryaw = Math.atan2(destination.y - mypos.y, destination.x - mypos.x);
						//Ryaw = Math.atan2((destination.y - mypos.x), (destination.x - mypos.y));
						Ryawsp = kpz * ((Ryaw - Math.toRadians(mypos.yaw)));
						Rroll = Math.asin((Ay_d * Math.cos(Math.toRadians(mypos.yaw)) - Ax_d * Math.sin(Math.toRadians(mypos.yaw))) %1);
						Rpitch = Math.asin( (-Ay_d * Math.sin(Math.toRadians(mypos.yaw)) - Ax_d * Math.cos(Math.toRadians(mypos.yaw))) / (Math.cos(Rroll)) %1);
						Rvs = (kpz * (destination.z - mypos.z) - kdz * mypos.v_z);
						//	System.out.println(Ryaw + " , " + Ryawsp + " , " +  Rroll  + " , " +  Rpitch + " , " + Rvs);

						setControlInputRescale(Math.toDegrees(Ryawsp),Math.toDegrees(Rpitch)%360,Math.toDegrees(Rroll)%360,Rvs);

						running = true;  /* need to keep running to be true for quadcopter to hover in GOAL state */

						inMotion = false;
						break;
					case STOP:
						gvh.log.i("FailFlag", "write");
						System.out.println("STOP");
						motion_stop();
						//do nothing
					}
					if(next != null) {
						prev = stage;
						stage = next;
//						System.out.println("Stage transition to " + stage.toString() + ", the previous stage is "+ prev);

						gvh.log.i(TAG, "Stage transition to " + stage.toString());
						gvh.trace.traceEvent(TAG, "Stage transition", stage.toString(), gvh.time());
					}
					next = null;
				} 

				if((colliding || stage == null) ) {
					gvh.log.i("FailFlag", "write");
					done = false;
					motion_stop();
				//	land();
				//	stage = STAGE.LAND;
				}
			}
			gvh.sleep(param.AUTOMATON_PERIOD);
		}
	}

	public void cancel() {
		running = false;
	}

	@Override
	public void motion_stop() {
		//land();
		//stage = STAGE.LAND;
		this.destination = null;
		running = false;
		inMotion = false;
	}

	@Override
	public void motion_resume() {
		running = true;
	}

	private void startMotion() {
		running = true;
		stage = STAGE.INIT;
		inMotion = true;
	}

	protected void sendMotionEvent(int motiontype, int... argument) {
		// TODO: This might not be necessary
		gvh.trace.traceEvent(TAG, "Motion", Arrays.toString(argument), gvh.time());
		gvh.sendRobotEvent(Event.MOTION, motiontype);
	}

	private void setControlInputRescale(double yaw_v, double pitch, double roll, double gaz){
		setControlInput(rescale(yaw_v, mypos.max_yaw_speed), rescale(pitch, mypos.max_pitch_roll), rescale(roll, mypos.max_pitch_roll), rescale(gaz, mypos.max_gaz));
	}

	private double rescale(double value, double max_value){
		if(Math.abs(value) > max_value){
			return (Math.signum(value));
		}
		else{
			return value/max_value;
		}
	}
//TODO: change this func to integrate the hardware control.
	protected void setControlInput(double yaw_v, double pitch, double roll, double gaz){
		if(yaw_v > 1 || yaw_v < -1){
			throw new IllegalArgumentException("yaw speed must be between -1 to 1");
		}
		if(pitch > 1 || pitch < -1){
			throw new IllegalArgumentException("pitch must be between -1 to 1");
		}
		if(roll > 1 || roll < -1){
			throw new IllegalArgumentException("roll speed must be between -1 to 1");
		}
		if(gaz > 1 || gaz < -1){
			throw new IllegalArgumentException("gaz, vertical speed must be between -1 to 1");
		}
		//Bluetooth command to control the drone
	//	gvh.log.i(TAG, "control input as, yaw, pitch, roll, thrust " + yaw_v + ", " + pitch + ", " +roll + ", " +gaz);
		/*
		if(running) {
			if(velocity != 0) {
				sendMotionEvent(Common.MOT_STRAIGHT, velocity);
			} else {
				sendMotionEvent(Common.MOT_STOPPED, 0);
			}
			bti.send(BluetoothCommands.straight(velocity));
		}
		 */
	}

	/**
	 *  	take off from ground
	 */
	protected void takeOff(){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "Drone taking off");
	}

	/**
	 * land on the ground
	 */
	protected void land(){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "Drone landing");
	}

	/**
	 * hover at current position
	 */
	protected void hover(){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "Drone hovering");
	}

	@Override
	public void turnTo(ItemPosition dest) {
		throw new IllegalArgumentException("quadcopter does not have a corresponding turn to");
	}

	@Override
	public void setParameters(MotionParameters param) {
		// TODO Auto-generated method stub		
	}


	/**
	 * Slow down linearly upon coming within R_slowfwd of the goal
	 * 
	 * @param distance
	 * @return
	 */
	/*
	private int LinSpeed(int distance) {
		if(distance > param.SLOWFWD_RADIUS)
			return param.LINSPEED_MAX;
		if(distance > param.GOAL_RADIUS && distance <= param.SLOWFWD_RADIUS) {
			return param.LINSPEED_MIN + (int) ((distance - param.GOAL_RADIUS) * linspeed);
		}
		return param.LINSPEED_MIN;
	}

	// Detects an imminent collision with another robot or with any obstacles

	@Override
	public void setParameters(MotionParameters param) {
		this.param = param;/		this.linspeed = (double) (param.LINSPEED_MAX - param.LINSPEED_MIN) / Math.abs((param.SLOWFWD_RADIUS - param.GOAL_RADIUS));
		this.turnspeed = (param.TURNSPEED_MAX - param.TURNSPEED_MIN) / (param.SLOWTURN_ANGLE - param.SMALLTURN_ANGLE);
	}
	 */
}
