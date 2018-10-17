package edu.illinois.mitra.cyphyhouse.ros;

import java.util.*;
import java.io.*;

import com.fasterxml.jackson.databind.JsonNode;
import ros.Publisher;
import ros.RosBridge;
import ros.RosListenDelegate;
import ros.SubscriptionRequestMsg;
import ros.msgs.sensor_msgs.LaserScan;
import ros.msgs.geometry_msgs.Point;
import ros.msgs.std_msgs.Header;
import ros.msgs.std_msgs.Time;
import ros.msgs.geometry_msgs.PointStamped;
import ros.msgs.std_msgs.PrimitiveMsg;
import ros.tools.MessageUnpacker;
import ros.RosBridge;


import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.models.Model_iRobot;
import edu.illinois.mitra.cyphyhouse.models.Model_Car;
import edu.illinois.mitra.cyphyhouse.models.Model_Quadcopter;
import edu.illinois.mitra.cyphyhouse.comms.UdpGpsReceiver;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_Car;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_Quadcopter;

public class JavaRosWrapper {
	public RosBridge bridge;
	public String robot_name;
	public List<Publisher> publishers;
	public List<String> subscribed_topics;
	public int count;
	public GlobalVarHolder gvh;
	public Model_Car model_car;
	public Model_iRobot model_irobot;
    public Model_Quadcopter model_quadcopter;
	public String platform;

	public JavaRosWrapper(String port, String name, GlobalVarHolder gvh, String platform){
		this.robot_name = name;
		this.publishers = new ArrayList<Publisher>();
		this.subscribed_topics = new ArrayList<String>();
		this.gvh = gvh;

		switch(platform){
			case "Car":
				//this.model_car = (Model_Car)gvh.gps.getMyPosition();
				break;
			case "iRobot":
				//this.model_irobot = (Model_iRobot).gvh.gps.getMyPosition();
				//this.model_irobot = (Model_iRobot)gvh.gps.getMyPosition();
				break;
            case "Quadcopter":
                break;
		}
		
		this.platform = platform;

		this.bridge = new RosBridge();
		bridge.connect(port, true);
	}

	public void createTopic(String topicType){
		int i = 0;
		switch(topicType){
			case "Waypoint": 
				while (i < publishers.size()){
					if (publishers.get(i).getTopic().matches("Waypoint_" + robot_name)){
						return;
					}
					i = i + 1;
				}
				Publisher pub = new Publisher(topicType + "_" + robot_name, "geometry_msgs/PointStamped", bridge);
				publishers.add(pub);
				break;
			case "additional datatypes here.....":
				break;
		}
	}

	public void removeTopic(String topicType){										
		switch(topicType){
			case "Waypoint":
				Iterator<Publisher> it = publishers.iterator();
				while (it.hasNext()) {
					if (it.next().getTopic().matches("Waypoint_" + robot_name)) {
						it.remove();
						this.bridge.unadvertise("Waypoint_" + robot_name);
						break;
					}
				}
				break;
			case "additional datatypes here......":
				break;
		}
	
	}

	public void sendMsg(ItemPosition dest, String type){
		int i = 0;
		while (i < publishers.size()){
			if (publishers.get(i).getTopic().matches("Waypoint_" + robot_name)){
                Point p = new Point((double)dest.x/100., (double)dest.y/100., (double)dest.z/100.);
                Time t = new Time(0,0);
                Header h = new Header(0,t, type);
                
				publishers.get(i).publish(new PointStamped(h, p));
				return;
			}
			i = i + 1;
		}
	}
    
    public void sendMsg(ItemPosition dest){
		int i = 0;
		while (i < publishers.size()){
			if (publishers.get(i).getTopic().matches("Waypoint_" + robot_name)){
                Point p = new Point((double)dest.x/100., (double)dest.y/100., (double)dest.z/100.);
                Time t = new Time(0,0);
                Header h = new Header(0,t, "3");
                
				publishers.get(i).publish(new PointStamped(h, p));
				return;
			}
			i = i + 1;
		}
	}



	public void subscribe_to_ROS(String topic, String topicType){
		int i = 0;
			while (i < subscribed_topics.size()){
				if (subscribed_topics.get(i).matches(topic)){
					return;
				}
				i = i + 1;
			}
		switch (topicType){
			case "Waypoint":
				bridge.subscribe(SubscriptionRequestMsg.generate(topic)
					.setType("geometry_msgs/Point")
					.setThrottleRate(1000)
					.setQueueLength(0),
				new RosListenDelegate() {

					public void receive(JsonNode data, String stringRep) {
						MessageUnpacker<Point> unpacker = new MessageUnpacker<Point>(Point.class);
						Point msg = unpacker.unpackRosMessage(data);
						/*model.TESTX = msg.x;
						if(robot_name.matches("iRobot0")){
							System.out.println("Updated " + robot_name + "'s value to: " + model.TESTX);
						}

									//System.out.println(msg.x + " " + msg.y + " " +msg.z + " " + robot_name);*/
							
					}
				}
				);
				break;


			case "Position":
				bridge.subscribe(SubscriptionRequestMsg.generate(topic)
					.setType("geometry_msgs/PointStamped")
					.setThrottleRate(1)
					.setQueueLength(0),
				new RosListenDelegate() {

					public void receive(JsonNode data, String stringRep) {
						MessageUnpacker<Point> unpacker = new MessageUnpacker<Point>(Point.class);
						Point msg = unpacker.unpackRosMessage(data);

						//System.out.println(msg.z);

						ItemPosition p = new ItemPosition(gvh.id.getName(), (int)msg.x, (int)msg.y, (int)msg.z);
						gvh.gps.setPosition(p);
						System.out.println("ADDING POSITION " + msg.x + " " + msg.y);
						//gvh.plat.model = new Model_Quadcopter("copter", (int)msg.x, (int)msg.y, (int)msg.z);
					}
				}
				);
				break;
				

			case "DecaWave":
				bridge.subscribe(SubscriptionRequestMsg.generate(topic)
					.setType("geometry_msgs/Point")
					.setThrottleRate(1)
					.setQueueLength(0),
				new RosListenDelegate() {

					public void receive(JsonNode data, String stringRep) {
						MessageUnpacker<Point> unpacker = new MessageUnpacker<Point>(Point.class);
						Point msg = unpacker.unpackRosMessage(data);
						System.out.println(msg.x + " " + msg.y + " " +msg.z);
						try{
							Thread.sleep(500);
						}
						catch(InterruptedException e){
		
						}
						//UdpGpsReceiver receive = (UdpGpsReceiver)gvh.gps.mGpsReceiver;
						//receive.receive_position_msg(msg);
						/*model.TESTX = msg.x;
						if(robot_name.matches("iRobot0")){
							System.out.println("Updated " + robot_name + "'s value to: " + model.TESTX);
						}

									//System.out.println(msg.x + " " + msg.y + " " +msg.z + " " + robot_name);*/
							
					}
				}
				);
				break;
				

			case "Reached Message":
				bridge.subscribe(SubscriptionRequestMsg.generate("/Reached")
					.setType("std_msgs/String")
					.setThrottleRate(1)
					.setQueueLength(0),
				new RosListenDelegate() {

					public void receive(JsonNode data, String stringRep) {
						MessageUnpacker<PrimitiveMsg<String>> unpacker = new MessageUnpacker<PrimitiveMsg<String>>(PrimitiveMsg.class);
						PrimitiveMsg<String> msg = unpacker.unpackRosMessage(data);
						//ADD STUFF HERE
						switch(msg.data){
							case "TRUE":
								switch(platform){
                                    case "Quadcopter":
                                        MotionAutomaton_Quadcopter motion;
										motion = (MotionAutomaton_Quadcopter) gvh.plat.moat;
										motion.reached = true;
										break;
								}
								break;
							case "FALSE":
								switch(platform){
                                    case "Quadcopter":
                                        MotionAutomaton_Quadcopter motion;
										motion = (MotionAutomaton_Quadcopter) gvh.plat.moat;
										motion.reached = false;
										break;
								}
								break;
						}
					}
				}
		);

		}


	}
	


}


