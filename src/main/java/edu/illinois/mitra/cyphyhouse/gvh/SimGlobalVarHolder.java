package edu.illinois.mitra.cyphyhouse.gvh;

import java.util.HashMap;

import edu.illinois.mitra.cyphyhouse.Handler.IPCHandler;
import edu.illinois.mitra.cyphyhouse.Handler.IPCMessage;
import edu.illinois.mitra.cyphyhouse.Handler.LooperThread;
import edu.illinois.mitra.cyphyhouse.harness.*;
import edu.illinois.mitra.cyphyhouse.interfaces.TrackedRobot;
import edu.illinois.mitra.cyphyhouse.models.*;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_Quadcopter;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_iRobot;
import edu.illinois.mitra.cyphyhouse.motion.ReachAvoid;
import edu.illinois.mitra.cyphyhouse.motion.MotionHandlerConfig;

/**
 * Extension of the GlobalVarHolder class for use in simulations of StarL applications 
 * @author Adam Zimmerman, Yixiao Lin
 * @version 2.0
 *
 */
public class SimGlobalVarHolder extends GlobalVarHolder {
	
	private SimulationEngine engine;
	private final String TAG = "SimGlobalVarHolder";
	private static LooperThread looperTh = new LooperThread();

	private void controlInCheck(double yaw_v, double pitch, double roll, double gaz) {
		if (yaw_v > 1 || yaw_v < -1) {
			throw new IllegalArgumentException("yaw speed must be between -1 to 1");
		}
		if (pitch > 1 || pitch < -1) {
			throw new IllegalArgumentException("pitch must be between -1 to 1");
		}
		if (roll > 1 || roll < -1) {
			throw new IllegalArgumentException("roll speed must be between -1 to 1");
		}
		if (gaz > 1 || gaz < -1) {
			throw new IllegalArgumentException("gaz, vertical speed must be between -1 to 1");
		}
	}
	/**
	 * @param name the name of this agent
	 * @param participants contains (name,IP) pairs for each participating agent 
	 * @param engine the main SimulationEngine
	 * @param initpos this agent's initial position
	 * @param traceDir the directory to write trace files to
	 */
	public SimGlobalVarHolder(String name, HashMap<String,String> participants, SimulationEngine engine, TrackedRobot initpos, String traceDir, int trace_driftMax, double trace_skewBound) {
		super(name, participants);
		this.engine = engine;
		super.comms = new Comms(this, new SimSmartComThread(this, engine.getComChannel()));
		super.gps = new Gps(this, new SimGpsReceiver(this, engine.getGps(), initpos));
		super.log = new SimLogging(name,this);
		super.trace = new Trace(name, traceDir, this);
		if(traceDir != null)
			trace.traceStart();
		super.plat = new GeneralJavaPlatform();
		plat.model = initpos;
		plat.reachAvoid = new ReachAvoid(this);
		looperTh.initThread();
        // Yixiao says IdealSimGpsProvider shouldn't be used. Should probably remove the if else here.
		if(initpos instanceof Model_iRobot){
			if(engine.getGps() instanceof IdealSimGpsProvider) {
				plat.moat = new IdealSimMotionAutomaton(this, (IdealSimGpsProvider)engine.getGps());
			} else {
				IPCHandler simHandlerIRobot = new IPCHandler(looperTh.getLooperRef()) {
					@Override
					public void handleMessage(IPCMessage msg) {
						switch (msg.what) {
							case MotionHandlerConfig.CMD_IROBOT_MOTION_STOP:
								engine.getGps().setVelocity((String) msg.obj, 0, 0);
								break;
							case MotionHandlerConfig.CMD_IROBOT_CURVE:
							case MotionHandlerConfig.CMD_IROBOT_STRAIGHT:
							case MotionHandlerConfig.CMD_IROBOT_TURN:
								engine.getGps().setVelocity((String) msg.obj, msg.arg1.intValue(),
										msg.arg2.intValue());
								break;
						}
					}
				};
				plat.moat = new MotionAutomaton_iRobot(this, simHandlerIRobot);
				plat.moat.start();
			}
		}
		else if(initpos instanceof Model_quadcopter){
			IPCHandler simHandlerQuad = new IPCHandler(looperTh.getLooperRef()) {
				@Override
				public void handleMessage(IPCMessage msg) {
					switch (msg.what) {
						case MotionHandlerConfig.CMD_DRONE_TAKEOFF:
							log.i(TAG, "Drone taking off");
							controlInCheck(0, 0, 0, 1);
							engine.getGps().setControlInput((String) msg.obj,
									0, 0, 0,
									((Model_quadcopter) plat.model).max_gaz);
							break;
						case MotionHandlerConfig.CMD_DRONE_LAND:
							log.i(TAG, "Drone landing");
							engine.getGps().setControlInput((String) msg.obj,
									0, 0, 0, 0);
							break;
						case MotionHandlerConfig.CMD_DRONE_HOVER:
							log.i(TAG, "Drone hovering");
							controlInCheck(0, 0, 0, 0);
							break;
					}
				}
			};
			plat.moat = new MotionAutomation_Quadcopter(this, simHandlerQuad);
			plat.moat.start();
		}
		else {
			throw new RuntimeException("After adding a model, please add the motion controler for that model in SimGlobalVarHolder.java");
		}
	}

	@Override
	public void sleep(long time) {
		if(time <= 0) throw new RuntimeException("What are you doing?? You can't sleep for <= 0!");
		try {
			engine.threadSleep(time, Thread.currentThread());
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public long time() {
		return engine.getTime();
	}
	
	@Override
	public void threadCreated(Thread thread) {
		engine.registerThread(thread);
	}

	@Override
	public void threadDestroyed(Thread thread) {
		engine.removeThread(thread);
	}	
}