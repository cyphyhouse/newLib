package testSim.main;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import edu.illinois.mitra.cyphyhouse.harness.RealisticSimGpsProvider;
import edu.illinois.mitra.cyphyhouse.harness.IdealSimGpsProvider;
import edu.illinois.mitra.cyphyhouse.harness.SimGpsProvider;
import edu.illinois.mitra.cyphyhouse.harness.SimulationEngine;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.models.Model_iRobot;
import edu.illinois.mitra.cyphyhouse.models.Model_quadcopter;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;
import testSim.draw.DrawFrame;
import testSim.draw.RobotData;

public class Simulation {
	private Collection<SimApp> bots = new HashSet<SimApp>();
	private HashMap<String, String> participants = new HashMap<String, String>();
	private SimGpsProvider gps;
	private SimulationEngine simEngine;

	private ExecutorService executor;

	private final SimSettings settings;


	private final DrawFrame drawFrame;
	private ObstacleList list; 

	public Simulation(Class<? extends LogicThread> app, final SimSettings settings) {
		if(settings.N_IROBOTS + settings.N_QUADCOPTERS <= 0)
			throw new IllegalArgumentException("Must have more than zero robots to simulate!");

		// Create set of robots whose wireless is blocked for passage between
		// the GUI and the simulation communication object
		Set<String> blockedRobots = new HashSet<String>();

		// Create participants and instantiate SimApps
		int robot_number;
		for(robot_number = 0; robot_number < settings.N_IROBOTS; robot_number++) {
			// Mapping between iRobot name and IP address
			participants.put(settings.IROBOT_NAME + robot_number, "192.168.0." + robot_number);
		}
		for(int j = robot_number; j < robot_number + settings.N_QUADCOPTERS; j++) {
			// Mapping between quadcopter name and IP address
			participants.put(settings.QUADCOPTER_NAME + j, "192.168.0." + (j+settings.N_IROBOTS));

		}
		
		// Start the simulation engine
		LinkedList<LogicThread> logicThreads = new LinkedList<LogicThread>();
		if(settings.DRAW){
			// Initialize viewer
			drawFrame = new DrawFrame(participants.keySet(), blockedRobots, settings);
			drawFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			simEngine = new SimulationEngine(settings.SIM_TIMEOUT, settings.MSG_MEAN_DELAY, settings.MSG_STDDEV_DELAY, settings.MSG_LOSSES_PER_HUNDRED, settings.MSG_RANDOM_SEED, settings.TIC_TIME_RATE, blockedRobots, participants, drawFrame.getPanel(), logicThreads);
		}
		else{
			drawFrame = null;
			simEngine = new SimulationEngine(settings.SIM_TIMEOUT, settings.MSG_MEAN_DELAY, settings.MSG_STDDEV_DELAY, settings.MSG_LOSSES_PER_HUNDRED, settings.MSG_RANDOM_SEED, settings.TIC_TIME_RATE, blockedRobots, participants, null, logicThreads);

		}


		// Create the sim gps
		// TODO: need to redefine the noises for models in general
        // According to Yixiao, IdealSimGpsProvider should not be used anymore
        // I've commented out this if statement so it cannot be used

		// Peter's notes:
		// Uncommenting for now to test gps provider without any noise
		// UPDATE: tried using idealsimgps, doesnt work (gives null pointer expcetion when initializing)
		/*if(settings.IDEAL_MOTION) {
			gps = new IdealSimGpsProvider(simEngine, settings.GPS_PERIOD, settings.GPS_ANGLE_NOISE, settings.GPS_POSITION_NOISE);
			//System.out.println("ADDED IDEAL SIM");
		} else {
			gps = new RealisticSimGpsProvider(simEngine, settings.GPS_PERIOD, settings.GPS_ANGLE_NOISE, settings.GPS_POSITION_NOISE);
		}*/
        gps = new RealisticSimGpsProvider(simEngine, settings.GPS_PERIOD, settings.GPS_ANGLE_NOISE, settings.GPS_POSITION_NOISE);

		// Load waypoints
		if(settings.WAYPOINT_FILE != null)
			gps.setWaypoints(WptLoader.loadWaypoints(settings.WAYPOINT_FILE));

		// Load sensepoints
		if(settings.SENSEPOINT_FILE != null)
			gps.setSensepoints(SptLoader.loadSensepoints(settings.SENSEPOINT_FILE));

		// Load Obstacles
		if(settings.OBSPOINT_FILE != null)
		{			
			gps.setObspoints(ObstLoader.loadObspoints(settings.OBSPOINT_FILE));
			list = gps.getObspointPositions();
			list.detect_Precision = settings.Detect_Precision;
			list.de_Radius = settings.De_Radius;
			//should we grid the environment?
			if(settings.Detect_Precision > 1){
				list.Gridfy();
			}
			gps.setViews(list, settings.N_IROBOTS);
		}
		else{
			//if we have no input files, we still have to initialize the obstacle list so that later on, if we detect collision between robots, we can add that obstacle
			gps.setObspoints(new ObstacleList());
			list = gps.getObspointPositions();
			list.detect_Precision = settings.Detect_Precision;
			list.de_Radius = settings.De_Radius;
			gps.setViews(list, settings.N_IROBOTS);
		}


		this.settings = settings;
		simEngine.setGps(gps);
		gps.start();

		// Load initial positions
		PositionList<ItemPosition> t_initialPositions;
		if(settings.INITIAL_POSITIONS_FILE != null){
			t_initialPositions = WptLoader.loadWaypoints(settings.INITIAL_POSITIONS_FILE);
		}
		else
			t_initialPositions = new PositionList<ItemPosition>();		
		Random rand = new Random();
		/*
		PositionList<Model_iRobot> initialPositions = new PositionList<Model_iRobot>();
		for(ItemPosition t_pos : t_initialPositions){
			initialPositions.update(new Model_iRobot(t_pos));
		}
		 */
		// Create each iRobot
		for(robot_number = 0; robot_number < settings.N_IROBOTS; robot_number++) {
			Model_iRobot initialPosition = null;
			String botName = settings.IROBOT_NAME + robot_number;
			ItemPosition initialPos = t_initialPositions.getPosition(botName);
			if(initialPos != null){
				initialPosition = new Model_iRobot(initialPos);
			}
			// If no initial position was supplied, randomly generate one
			if(initialPosition == null) {

				//Set robot 0 to be outside of world boundary area (robot0 is base computer)
				if(robot_number == 0){
					initialPosition = new Model_iRobot(botName, settings.GRID_XSIZE + 500, settings.GRID_YSIZE);
				}
				else {
					//	System.out.println("null position in list");
					int retries = 0;
					boolean valid = false;
					while (retries++ < 10000 && (!acceptableStart(initialPosition) || !valid)) {
						initialPosition = new Model_iRobot(botName, rand.nextInt(settings.GRID_XSIZE), rand.nextInt(settings.GRID_YSIZE), rand.nextInt(360));
						if (list != null) {
							valid = (list.validstarts(initialPosition, initialPosition.radius));
						}
					}
					if (retries > 10000) {
						System.out.println("too many tries for BOT" + botName + "please increase settings.GRID_XSIZE/GRID_YSIZE or remove some obstacles");
					}
				}
			}
			if(robot_number< settings.N_DBOTS){
				initialPosition.type = 1;
			}
			else if((robot_number>=settings.N_DBOTS) && (robot_number<(settings.N_DBOTS + settings.N_RBOTS))){
				initialPosition.type = 2;	
			}
			else{
				initialPosition.type = 0;
				//default robot type is 0
			}

			initialPosition.radius = settings.BOT_RADIUS;
			SimApp sa = new SimApp(botName, participants, simEngine, initialPosition, settings.TRACE_OUT_DIR, app, drawFrame, settings.TRACE_CLOCK_DRIFT_MAX, settings.TRACE_CLOCK_SKEW_MAX);
			bots.add(sa);
			logicThreads.add(sa.logic);
			simEngine.addLogging(sa.gvh.log);

		}
		for(int i = robot_number; i < robot_number + settings.N_QUADCOPTERS; i++) {
			Model_quadcopter initialPosition = null;
			String botName = settings.QUADCOPTER_NAME + i;
			ItemPosition initialPos = t_initialPositions.getPosition(botName);
			if(initialPos != null){
				initialPosition = new Model_quadcopter(initialPos);
			}
			// If no initial position was supplied, randomly generate one
			if(initialPosition == null) {	
				//	System.out.println("null position in list");
				int retries = 0;
				boolean valid = false;
				while(retries++ < 10000 && (!acceptableStart(initialPosition) || !valid))
				{
					initialPosition = new Model_quadcopter(botName, rand.nextInt(settings.GRID_XSIZE), rand.nextInt(settings.GRID_YSIZE), 0, rand.nextInt(360));
					if(list != null){
						valid = (list.validstarts(initialPosition, initialPosition.radius));
					}	
				}
				if(retries > 10000)
				{
					System.out.println("too many tries for BOT"+botName+"please increase settings.GRID_XSIZE/GRID_YSIZE/GRID_ZSIZE or remove some obstacles");
				}
			}
			initialPosition.radius = settings.BOT_RADIUS;

			SimApp sa = new SimApp(botName, participants, simEngine, initialPosition, settings.TRACE_OUT_DIR, app, drawFrame, settings.TRACE_CLOCK_DRIFT_MAX, settings.TRACE_CLOCK_SKEW_MAX);
			
			bots.add(sa);

			logicThreads.add(sa.logic);
			simEngine.addLogging(sa.gvh.log);
			
		}

		if(settings.USE_GLOBAL_LOGGER)
			gps.addObserver(createGlobalLogger(settings));

		if(settings.DRAW){
			// initialize debug drawer class if it was set in the settings
			if(settings.DRAWER != null)
				drawFrame.addPredrawer(settings.DRAWER);
			// GUI observer updates the viewer when new positions are calculated
			Observer guiObserver = new Observer() {
				@Override
				public void update(Observable o, Object arg) {
					Color[] c = new Color[12] ;
					c[0] = Color.BLACK;
					c[1] = Color.BLUE;
					c[2] = Color.GREEN;
					c[3] = Color.MAGENTA;
					c[4] = Color.ORANGE;
					c[5] = Color.CYAN;
					c[6] = Color.GRAY;
					c[7] = Color.PINK;
					c[8] = Color.RED;
					c[9] = Color.LIGHT_GRAY;
					c[10] = Color.YELLOW;
					c[11] = Color.DARK_GRAY;

					Vector<ObstacleList> views = gps.getViews();
					//				ArrayList<Model_iRobot> pos;
					//				ArrayList<Model_quadcopter> pos2;
					ArrayList<RobotData> rd = new ArrayList<RobotData>();
					ArrayList targetList = ((PositionList) arg).getList();
					if(targetList.size() >0){
						for(int i = 0; i < targetList.size(); i++){
							if(targetList.get(i) instanceof Model_iRobot){

								Model_iRobot ip = (Model_iRobot) targetList.get(i);
								if(i<12){
									RobotData nextBot = new RobotData(ip.name, ip.x, ip.y, ip.angle, c[i], views.elementAt(i), ip.leftbump, ip.rightbump);
									nextBot.radius = settings.BOT_RADIUS;
									nextBot.type = ip.type;
									rd.add(nextBot);
								}
								else{
									RobotData nextBot = new RobotData(ip.name, ip.x, ip.y, ip.angle, c[0], views.elementAt(i), ip.leftbump, ip.rightbump);
									nextBot.radius = settings.BOT_RADIUS;
									rd.add(nextBot);
								}
							}
							else if(targetList.get(i) instanceof Model_quadcopter){
								Model_quadcopter ip = (Model_quadcopter) targetList.get(i);
								RobotData nextBot = new RobotData(ip.name, ip.x, ip.y, ip.z, ip.yaw, ip.pitch, ip.roll, ip.receivedTime);
								nextBot.radius = settings.BOT_RADIUS;
								rd.add(nextBot);
							}
						}
					}
					// Add waypoints
					if(settings.DRAW_WAYPOINTS) {
						for(ItemPosition ip : gps.getWaypointPositions().getList()) {
							RobotData waypoint = new RobotData((settings.DRAW_WAYPOINT_NAMES ? ip.name : ""), ip.x, ip.y, ip.index);
							waypoint.radius = 5;
							waypoint.c = new Color(255, 0, 0);
							rd.add(waypoint);
						}
					}
					drawFrame.updateData(rd, simEngine.getTime());
					//add obstacle update later
				}
			};
			gps.addObserver(guiObserver);
			// show viewer
			drawFrame.setVisible(true);
		}
	}

	private static final double BOT_SPACING_FACTOR = 2.8;
	private Map<String, ItemPosition> startingPositions = new HashMap<String, ItemPosition>();

	private boolean acceptableStart(ItemPosition pos) {
		if(pos == null)
			return false;
		startingPositions.put(pos.getName(), pos);
		for(Entry<String, ItemPosition> entry : startingPositions.entrySet()) {
			if(!entry.getKey().equals(pos.getName())) {
				if(entry.getValue().distanceTo(pos) < (BOT_SPACING_FACTOR * settings.BOT_RADIUS))
					return false;
			}
		}
		return true;
	}

	/**
	 * Add an Observer to the list of GPS observers. This Observer's update
	 * method will be passed a PositionList object as the argument. This must be
	 * called before the simulation is started!
	 * 
	 * @param o
	 */
	public void addPositionObserver(Observer o) {
		if(executor == null)
			gps.addObserver(o);
	}

	private Observer createGlobalLogger(final SimSettings settings) {
		final GlobalLogger gl = new GlobalLogger(settings.TRACE_OUT_DIR, "global.txt");

		// global logger observer updates the log file when new positions are calculated
		Observer globalLogger = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				ArrayList<RobotData> rd = new ArrayList<RobotData>();
                ArrayList<ItemPosition> pos = ((PositionList) arg).getList();
                for(ItemPosition ip : pos) {
                    if(ip instanceof Model_iRobot) {
                        Model_iRobot m = (Model_iRobot) ip;
                        RobotData nextBot = new RobotData(m.name, m.x, m.y, m.angle, ip.receivedTime);
                        nextBot.radius = settings.BOT_RADIUS;
                        rd.add(nextBot);
                    }
                    else if(ip instanceof Model_quadcopter) {
                        Model_quadcopter m = (Model_quadcopter) ip;
                        RobotData nextBot = new RobotData(ip.name, m.x, m.y, m.z, m.yaw, m.pitch, m.roll, m.receivedTime);
                        nextBot.radius = settings.BOT_RADIUS;
                        rd.add(nextBot);
                    }
                }

				gl.updateData(rd, simEngine.getTime());
			}
		};
		return globalLogger;
	}


	private List<List<Object>> resultsList = new ArrayList<List<Object>>();
	/**
	 * Begins executing a simulation. This call will block until the simulation completes.
	 */
	public void start() {
		executor = Executors.newFixedThreadPool(participants.size());
		// Save settings to JSON file
		if(settings.TRACE_OUT_DIR != null)
			SettingsWriter.writeSettings(settings);

		// Invoke all simulated robots
		List<Future<List<Object>>> results = null;
		try {
			if(settings.TIMEOUT > 0)
				results = executor.invokeAll(bots, settings.TIMEOUT, TimeUnit.SECONDS);
			else
				results = executor.invokeAll(bots);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		// Wait until all result values are available
		for(Future<List<Object>> f : results) {
			try {
				List<Object> res = f.get();
				if(res != null && !res.isEmpty())
					resultsList.add(res);
			} catch(CancellationException e) {
				// If the executor timed out, the result is cancelled
				System.err.println("Simulation timed out! Execution reached " + settings.TIMEOUT + " sec duration. Aborting.");
				break;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		shutdown();
	}

	public void shutdown() {
		simEngine.simulationDone();
		executor.shutdownNow();
	}

	public void closeWindow() {
		drawFrame.dispose();
	}

	public List<List<Object>> getResults() {
		return resultsList;
	}

	public long getSimulationDuration() {
		return simEngine.getDuration();
	}

	public String getMessageStatistics() {
		return 	simEngine.getComChannel().getStatistics();
	}
}
