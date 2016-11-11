package edu.illinois.mitra.cyphyhouse.harness;

import java.util.Vector;

import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.GpsReceiver;
import edu.illinois.mitra.cyphyhouse.interfaces.RobotEventListener.Event;
import edu.illinois.mitra.cyphyhouse.interfaces.TrackedRobot;
import edu.illinois.mitra.cyphyhouse.objects.Common;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;

public class SimGpsReceiver implements GpsReceiver {
	private static final String TAG = "GPSReceiver";
	
	private GlobalVarHolder gvh;
	
	public boolean inMotion = false;
	
	private SimGpsProvider provider;
	
	public SimGpsReceiver(GlobalVarHolder gvh, SimGpsProvider provider, TrackedRobot initpos) {
		this.gvh = gvh;
		this.provider = provider;
		provider.registerReceiver(gvh.id.getName(), this);
		provider.addRobot(initpos);
	}	
	
	@Override
	public void start() {
		// TODO Auto-generated method stub
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
	}

	public void receivePosition(boolean inMotion) {
		gvh.trace.traceEvent(TAG, "Received Position", gvh.gps.getMyPosition(), gvh.time());
		gvh.sendRobotEvent(Event.GPS_SELF);
		if(inMotion) {
			gvh.sendRobotEvent(Event.MOTION, Common.MOT_STRAIGHT);
		} else {
			gvh.sendRobotEvent(Event.MOTION, Common.MOT_STOPPED);
		}
		this.inMotion = inMotion;
	}

	@Override
	public PositionList<ItemPosition> get_robots() {
		return provider.getAllPositions();
	}

	@Override
	public PositionList<ItemPosition> getWaypoints() {
		return provider.getWaypointPositions();
	}
	@Override
	public ObstacleList getObspoints() {
		return provider.getObspointPositions();
	}

	@Override
	public Vector<ObstacleList> getViews() {
		return provider.getViews() ;
	}

	@Override
	public PositionList<ItemPosition> getSensepoints() {
		return provider.getSensePositions();
	}
}
