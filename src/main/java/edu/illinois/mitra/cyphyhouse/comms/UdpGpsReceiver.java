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
import edu.illinois.mitra.cyphyhouse.models.Model_quadcopter;
import edu.illinois.mitra.cyphyhouse.objects.Common;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;

/**
 * Hardware specific. Opens a UDP socket for receiving GPS broadcasts. 
 * @author Adam Zimmerman
 * @version 1.0
 */
public class UdpGpsReceiver extends Thread implements GpsReceiver {
	private static final String TAG = "UdpGPSReceiver";
	private static final String ERR = "Critical Error";

	public PositionList robotPositions;
	public PositionList<ItemPosition> waypointPositions;
	public ObstacleList obs;
	public Vector<ObstacleList> viewsOfWorld;


	private GlobalVarHolder gvh;

	private DatagramSocket mSocket;
	private InetAddress myLocalIP;
	private boolean running = true;
	private String name = null;
	private boolean received = false;

	public UdpGpsReceiver(GlobalVarHolder gvh,String hostname, int port, PositionList robotPositions,
						  PositionList<ItemPosition> waypointPositions, ObstacleList obs,
						  Vector<ObstacleList> viewsOfWorld) {
		super();
		this.gvh = gvh;

		name = gvh.id.getName();
		this.robotPositions = robotPositions;
		this.waypointPositions = waypointPositions;
		this.obs = obs;
		this.viewsOfWorld = viewsOfWorld;

		try {
			myLocalIP = getLocalAddress();
			mSocket = new DatagramSocket(port);
		} catch (IOException e) {
			gvh.log.e(TAG, "Unable to create socket!");
			e.printStackTrace();
		}
		WaypointHelper();

		gvh.log.i(TAG, "Listening to GPS host on port " + port);
		gvh.trace.traceEvent(TAG, "Created", gvh.time());
	}

	private final int[] mypos = {0,0,0};//the pos of the robot
	//	private final int[][] goalpos = {{-1300,-260,0},{116,-260,0},{1000,-1400,0},{1000,1400,0}};
//	private final int[][] goalpos = {{-1152,-1082,0},{-1152,1200,0},{1800,1200,0},{1800,-1000,0}};
	private final int[][] goalpos = {{-1679,-513,0},{1600,-513,0},{-1679,-513,0},{1600,-513,0}};

	private void WaypointHelper(){
		gvh.log.i("TAG", "using preset waypoints");
		for(int i=0; i<goalpos.length; i++){
			ItemPosition temp = new ItemPosition("Goal"+Integer.toString(i),
					goalpos[i][0], goalpos[i][1], goalpos[i][2]);
			this.waypointPositions.update(temp);
		}
	}

	@Override
	public synchronized void start() {
		gvh.log.i("GPSReceiver", "Starting GPS receiver");
		running = true;
		super.start();
	}

	@Override
	public void run() {
		byte[] buf = new byte[2048];

		while(running) {
			try {
				// Receive a message
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				mSocket.receive(packet);
				InetAddress remoteIP = packet.getAddress();
				if(remoteIP.equals(myLocalIP))
					continue;

				String line = new String(packet.getData(), 0, packet.getLength());

				// Parse the received string
				String [] parts = line.split("\n");
				if(received == false) {
					gvh.log.i(TAG, "RECEIVED FIRST PACKET!");
//					gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_LOCATION, HandlerMessage.GPS_RECEIVING);
					received = true;
				}
				for(int i = 0; i < parts.length; i++) {
					if(parts[i].length() >= 2) {
						switch(parts[i].charAt(0)) {
							case '@':
								try {
									ItemPosition newpos = new ItemPosition(parts[i]);
									waypointPositions.update(newpos, gvh.time());
									gvh.trace.traceEvent(TAG, "Received Waypoint", newpos, gvh.time());
								} catch(ItemFormattingException e){
									gvh.log.e(TAG, "Invalid item formatting: " + e.getError());
								}
								break;

							case '#':
								try {
									Model_iRobot newpos = new Model_iRobot(parts[i]);
									robotPositions.update(newpos, gvh.time());
									gvh.sendRobotEvent(Event.GPS);
									if(newpos.name.equals(name)) {
										gvh.trace.traceEvent(TAG, "Received Position", newpos, gvh.time());
										gvh.sendRobotEvent(Event.GPS_SELF);
									}
								} catch(ItemFormattingException e){
									gvh.log.e(TAG, "Invalid item formatting: " + e.getError());
								}
								break;
							case '$':
								try {
									Model_quadcopter newpos = new Model_quadcopter(parts[i]);
									robotPositions.update(newpos, gvh.time());
									gvh.sendRobotEvent(Event.GPS);
									if(newpos.name.equals(name)) {
										gvh.trace.traceEvent(TAG, "Received Position", newpos, gvh.time());
										gvh.sendRobotEvent(Event.GPS_SELF);
									}
								} catch(ItemFormattingException e){
									gvh.log.e(TAG, "Invalid item formatting: " + e.getError());
								}
								break;
							case '%':
								try {
									ModelARDrone2 newpos;
									newpos = new ModelARDrone2(parts[i]);
									robotPositions.update(newpos, gvh.time());
//									gvh.log.i(TAG, "ARDrone at ("+newpos.x+","+newpos.y+","+newpos.z+")");
									gvh.sendRobotEvent(Event.GPS);
									if(newpos.name.equals(name)) {
										gvh.trace.traceEvent(TAG, "Received Position", newpos, gvh.time());
										gvh.sendRobotEvent(Event.GPS_SELF);
									}
								} catch(ItemFormattingException e){
									gvh.log.e(TAG, "Invalid item formatting: " + e.getError());
								}
								break;
							case 'G':
								gvh.trace.traceEvent(TAG, "Received launch command", gvh.time());
								int[] args = Common.partsToInts(parts[i].substring(3).split(" "));
//								gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_LAUNCH, args[0], args[1]);
								break;
							case 'A':
								gvh.trace.traceEvent(TAG, "Received abort command", gvh.time());
//								gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_ABORT, null);
								break;
							default:
								gvh.log.e(ERR, "Unknown GPS message received: " + line);
								break;
						}
					}
				}
			} catch (IOException e) {
//				gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_LOCATION, HandlerMessage.GPS_OFFLINE);
			}
		}
	}

	private InetAddress getLocalAddress() throws IOException {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress;
					}
				}
			}
		} catch (SocketException ex) {
			gvh.log.e(TAG, ex.toString());
		}
		return null;
	}

	/* (non-Javadoc)
     * @see edu.illinois.mitra.starl.comms.GpsReceiver#cancel()
     */
	@Override
	public void cancel() {
		running = false;
//		gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_LOCATION, HandlerMessage.GPS_OFFLINE);
		try {
			mSocket.disconnect();
			mSocket.close();
		} catch (Exception e) {
			gvh.log.e(ERR, "close of connect socket failed" + e);
			e.printStackTrace();
		}
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
		return null;
	}

	@Override
	public PositionList<ItemPosition> get_robots() {
		return robotPositions;
	}



}