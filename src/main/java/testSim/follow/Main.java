package testSim.follow;

import testSim.main.SimSettings;
import testSim.main.Simulation;

import com.fasterxml.jackson.databind.JsonNode;
import ros.Publisher;
import ros.RosBridge;
import ros.RosListenDelegate;
import ros.SubscriptionRequestMsg;
import ros.msgs.std_msgs.PrimitiveMsg;
import ros.msgs.sensor_msgs.LaserScan;
import ros.tools.MessageUnpacker;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
        settings.N_IROBOTS(4);
		settings.N_QUADCOPTERS(0);
		settings.TIC_TIME_RATE(2);
        settings.WAYPOINT_FILE("square.wpt");
		//settings.WAYPOINT_FILE(System.getProperty("user.dir")+"\\trunk\\android\\RaceApp\\waypoints\\four1.wpt");
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.DRAWER(new FollowDrawer());

		/* set up ros java bridge */
		/*RosBridge bridge = new RosBridge();
		bridge.connect("ws://localhost:9090", true);*/
		
		Simulation sim = new Simulation(FollowApp.class, settings.build());
		sim.start();
	}

}
