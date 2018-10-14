package testSim.follow;

import testSim.main.SimSettings;
import testSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
        settings.N_IROBOTS(3);
        settings.N_QUADCOPTERS(1);
		settings.TIC_TIME_RATE(20);
        //settings.WAYPOINT_FILE("square.wpt");
		//settings.WAYPOINT_FILE(System.getProperty("user.dir")+"\\trunk\\android\\RaceApp\\waypoints\\four1.wpt");
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.IDEAL_MOTION(true);
		settings.DRAWER(new FollowDrawer());
		settings.DRAW_WORLD_BOUNDARY(true);
		settings.DRAW_BASE_ROBOT(false);
		
		Simulation sim = new Simulation(FollowApp.class, settings.build());
		sim.start();
	}

}
