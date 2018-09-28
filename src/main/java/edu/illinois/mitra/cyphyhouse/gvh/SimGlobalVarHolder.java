package edu.illinois.mitra.cyphyhouse.gvh;

import java.util.HashMap;

import edu.illinois.mitra.cyphyhouse.harness.*;
import edu.illinois.mitra.cyphyhouse.interfaces.TrackedRobot;
import edu.illinois.mitra.cyphyhouse.models.*;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_Quadcopter;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_iRobot;


/**
 * Extension of the GlobalVarHolder class for use in simulations of StarL applications
 * @author Adam Zimmerman, Yixiao Lin
 * @version 2.0
 *
 */
public class SimGlobalVarHolder extends GlobalVarHolder {

	private SimulationEngine engine;

	/** Updated 9/28/2018
	 * Broke simulator here, need to use REALISTICSIMMOTIONAUTOMATON_QUADCOPTER/IROBOT
	 *
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

		// Yixiao says IdealSimGpsProvider shouldn't be used. Should probably remove the if else here.
		if(initpos instanceof Model_iRobot){
			if(engine.getGps() instanceof IdealSimGpsProvider) {
				plat.moat = new IdealSimMotionAutomaton(this, (IdealSimGpsProvider)engine.getGps());
			} else {
				plat.moat = new MotionAutomaton_iRobot(this);
				plat.moat.start();
			}
		}
		else if(initpos instanceof Model_Quadcopter){
			plat.moat = new MotionAutomaton_Quadcopter(this);
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