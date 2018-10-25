package edu.illinois.mitra.cyphyhouse.gvh;

import java.util.Map;
import java.util.Vector;

import edu.illinois.mitra.cyphyhouse.comms.SmartUdpComThread;
import edu.illinois.mitra.cyphyhouse.comms.UdpGpsReceiver;
import edu.illinois.mitra.cyphyhouse.comms.RosDecaWaveReceiver;
import edu.illinois.mitra.cyphyhouse.interfaces.TrackedRobot;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomation_ARDrone2;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_iRobot;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_Car;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_Quadcopter;
import edu.illinois.mitra.cyphyhouse.motion.ReachAvoid;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;

import ros.RosBridge;

/**
 * Extension of the GlobalVarHolder class for use in physical implementations of StarL applications
 * @author Adam Zimmerman
 * @version 1.0
 */
public class RealGlobalVarHolder extends GlobalVarHolder {

	/**
	 * @param name the name of this agent
	 * @param participants contains (name,IP) pairs for each participating agent
	 * @param robotMac the MAC address of this agent's iRobot Create chassis
	 */

	public String position_data_topic;

	public RealGlobalVarHolder(String name, Map<String,String> participants, TrackedRobot initpos, String robotMac) {
		super(name, participants);

		super.log = new JavaLogging();
		super.trace = new Trace(name, "/sdcard/trace/", this);
		super.plat = new GeneralJavaPlatform();
		super.comms = new Comms(this, new SmartUdpComThread(this));


		super.gps = new Gps(this, new RosDecaWaveReceiver(this, position_data_topic, new PositionList(),new PositionList(), new ObstacleList(), new Vector<ObstacleList>(3,2) ));
		plat.model = initpos;
		plat.reachAvoid = new ReachAvoid(this);
		plat.moat = new MotionAutomaton_Quadcopter(this);
		plat.moat.start();
	}

	@Override
	public void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public long time() {
		return System.currentTimeMillis();
	}

	@Override
	public void threadCreated(Thread thread) {
		// Nothing happens here
	}

	@Override
	public void threadDestroyed(Thread thread) {
		// Nothing happens here		
	}
}
