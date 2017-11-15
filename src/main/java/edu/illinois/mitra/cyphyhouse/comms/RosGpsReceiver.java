package edu.illinois.mitra.cyphyhouse.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;

import edu.illinois.mitra.cyphyhouse.exceptions.ItemFormattingException;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.GpsReceiver;
import edu.illinois.mitra.cyphyhouse.interfaces.RobotEventListener.Event;
import edu.illinois.mitra.cyphyhouse.models.ModelARDrone2;
import edu.illinois.mitra.cyphyhouse.models.Model_iRobot;
import edu.illinois.mitra.cyphyhouse.models.Model_Car;
import edu.illinois.mitra.cyphyhouse.models.Model_quadcopter;
import edu.illinois.mitra.cyphyhouse.objects.Common;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;
import edu.illinois.mitra.cyphyhouse.ros.JavaRosWrapper;
import ros.msgs.geometry_msgs.Point;

/**
 * Hardware specific. Opens a UDP socket for receiving GPS broadcasts. 
 * @author Adam Zimmerman
 * @version 1.0
 */
public class RosGpsReceiver extends Thread implements GpsReceiver {
	private static final String TAG = "RosGPSReceiver";
	private static final String ERR = "Critical Error";

	public PositionList robotPositions;
	public PositionList<ItemPosition> waypointPositions;
	public ObstacleList obs;
	public Vector<ObstacleList> viewsOfWorld;
	public JavaRosWrapper wrapper;


	private GlobalVarHolder gvh;

	private boolean running = true;
	private String name = null;
	private boolean received = false;
	

	public RosGpsReceiver(GlobalVarHolder gvh, String TopicName, PositionList robotPositions,
						  PositionList<ItemPosition> waypointPositions, ObstacleList obs,
						  Vector<ObstacleList> viewsOfWorld) {
		super();
		this.gvh = gvh;

		name = gvh.id.getName();
		this.robotPositions = robotPositions;
		this.waypointPositions = waypointPositions;
		this.obs = obs;
		this.viewsOfWorld = viewsOfWorld;
	
		wrapper = new JavaRosWrapper("ws://localhost:9090", name, this.gvh, "Car");
		wrapper.subscribe_to_ROS("position", "Waypoint");


		gvh.log.i(TAG, "Subscribing to ROS TOPIC " + TopicName);
		gvh.trace.traceEvent(TAG, "Created", gvh.time());
	}


	@Override
	public synchronized void start() {
		gvh.log.i("GPSReceiver", "Starting GPS receiver");
		running = true;
		super.start();
	}

	public void receive_position_msg(Point msg){
		String parts = name + " " + msg.x + " " + msg.y + " " + msg.z;
		try{
			Model_Car newpos = new Model_Car(parts);
			robotPositions.update(newpos, gvh.time());
			gvh.sendRobotEvent(Event.GPS);
			if(newpos.name.equals(name)) {
				gvh.trace.traceEvent(TAG, "Received Position", newpos, gvh.time());
				gvh.sendRobotEvent(Event.GPS_SELF);
			}
		}
		catch(ItemFormattingException e){
			gvh.log.e(TAG, "Invalid item formatting: " + e.getError());

		}
		// ADD DELAY
		////////////////////////////////////////////////
		////////////////////////////////////////////////

	}

	@Override
	public void run() {
		return;
	}

	

	/* (non-Javadoc)
     * @see edu.illinois.mitra.starl.comms.GpsReceiver#cancel()
     */
	@Override
	public void cancel() {
		running = false;

		gvh.log.i(TAG, "Closed UDP GPS socket");
		gvh.trace.traceEvent(TAG, "cancelled", gvh.time());
	}

	@Override
	public PositionList<ItemPosition> getWaypoints() {
		return waypointPositions;
	}

	@Override
	public ObstacleList getObspoints() {
		return obs;
	}

	@Override
	public Vector<ObstacleList> getViews() {
		return viewsOfWorld;
	}

	@Override
	public PositionList<ItemPosition> getSensepoints() {
		// TODO work in progress, sensepoints data should really come in as the robot's sensor data, this should be more general to accommodate any sensor data
		/////////////////////////////////////
		// ROS Sensor subscriptions here



		return null;
	}

	@Override
	public PositionList<ItemPosition> get_robots() {
		return robotPositions;
	}



}
