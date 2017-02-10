package edu.illinois.mitra.cyphyhouse.harness;

import edu.illinois.mitra.cyphyhouse.motion.MotionAutomation_quadcopter_Base;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.models.Model_quadcopter;

public class RealisticSimMotionAutomation_quadcopter extends MotionAutomation_quadcopter_Base {
	private SimGpsProvider gpsp;
	private String name;
	private Model_quadcopter my_model;
	
	public RealisticSimMotionAutomation_quadcopter(GlobalVarHolder gvh, SimGpsProvider gpsp) {
		super(gvh);
		name = gvh.id.getName();
		this.gpsp = gpsp;
		this.my_model = (Model_quadcopter)gvh.plat.model;
	}

	@Override
	public void setControlInput(double yaw_v, double pitch, double roll, double gaz){
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
		gpsp.setControlInput(name, yaw_v*my_model.max_yaw_speed, pitch*my_model.max_pitch_roll, roll*my_model.max_pitch_roll, gaz*my_model.max_gaz);
	}

	/**
	 *  	take off from ground
	 */
	@Override
	protected void takeOff(){
		gvh.log.i(TAG, "Drone taking off");
		setControlInput(0, 0, 0, 1);
	}
	
	/**
	 * land on the ground
	 */
	@Override
	protected void land(){
		gvh.log.i(TAG, "Drone landing");
		//setControlInput(my_model.yaw, 0, 0, 5);
	}
	
	/**
	 * hover at current position
	 */
	@Override
	protected void hover(){
		gvh.log.i(TAG, "Drone hovering");
		setControlInput(0, 0, 0, 0);
	}

	@Override
	public void cancel() {
		super.running = false;
	}
}
