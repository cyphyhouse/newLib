package testSim.follow;

import testSim.main.SimSettings;
import testSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
        settings.N_IROBOTS(1);
        settings.N_QUADCOPTERS(5);
		settings.TIC_TIME_RATE(50);
        //settings.WAYPOINT_FILE("square.wpt");
		//settings.WAYPOINT_FILE(System.getProperty("user.dir")+"\\trunk\\android\\RaceApp\\waypoints\\four1.wpt");
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.IDEAL_MOTION(true);
		settings.DRAWER(new FollowDrawer());
		settings.DRAW_WORLD_BOUNDARY(true);
		settings.DRAW_BASE_ROBOT(true);
		settings.GRID_XSIZE(10000);
		settings.GRID_YSIZE(10000);
		settings.INITIAL_POSITIONS_FILE("initial_positions.wpt");
		
		Simulation sim = new Simulation(FollowApp.class, settings.build());
		sim.start();
	}

}
