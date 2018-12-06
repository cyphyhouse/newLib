package edu.illinois.mitra.cyphyhouse.harness;

import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.RobotEventListener;
import edu.illinois.mitra.cyphyhouse.motion.MotionParameters;
//import edu.illinois.mitra.cyphyhouse.motion.RRTNode;
import edu.illinois.mitra.cyphyhouse.motion.RobotMotion;
import edu.illinois.mitra.cyphyhouse.objects.Common;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;

import java.util.Stack;

public class IdealSimMotionAutomaton extends RobotMotion implements RobotEventListener {
	private static final String TAG = "MotionAutomaton";
	private IdealSimGpsProvider gpspro;
	private String name;
	private MotionParameters defaultParam = MotionParameters.defaultParameters();
	
	private GlobalVarHolder gvh;
	private ObstacleList obsList;
	
	private ItemPosition dest;
	
	public IdealSimMotionAutomaton(GlobalVarHolder gvh, IdealSimGpsProvider gpspro) {
		this.gvh = gvh;
		this.gpspro = gpspro; 
		name = gvh.id.getName();
		gvh.addEventListener(this);
		gvh.trace.traceEvent(TAG, "Created", gvh.time());
	}
	@Override
	public void goTo(ItemPosition dest, ObstacleList obsList) {
		// need to take a look into the collision here
		ItemPosition ip = new ItemPosition(dest);
		this.dest = ip;
		this.obsList = obsList;
		gvh.trace.traceEvent(TAG, "Go To", ip, gvh.time());
		
		gpspro.setDestination(name, ip, defaultParam.LINSPEED_MAX);
		
		inMotion = true;
	}

	@Override
	public Stack<ItemPosition> initMotion(){
		Stack<ItemPosition> path = new Stack<ItemPosition>();
		return path;
	}

	@Override
	public void goTo(ItemPosition dest) {
		ObstacleList obsList = gvh.gps.getObspointPositions();
		goTo(dest,obsList);
	}

	@Override
	public void turnTo(ItemPosition dest) {
		// turnTo isn't implemented for ideal motion		
	}

	@Override
	public void cancel() {
		gvh.trace.traceEvent(TAG, "Cancelled", gvh.time());
		motion_stop();
	}

	@Override
	public void motion_stop() {
		gvh.trace.traceEvent(TAG, "Halt", gvh.time());
		gpspro.halt(name);
	}
	@Override
	public void robotEvent(Event type, int event) {
		if(type == Event.MOTION) {
			inMotion = (event!=Common.MOT_STOPPED);
		}
	}

	@Override
	public void motion_resume() {
		// I don't think anything needs to happen here
	}
	@Override
	public void setParameters(MotionParameters param) 
	{
		defaultParam = param;
	}


}
