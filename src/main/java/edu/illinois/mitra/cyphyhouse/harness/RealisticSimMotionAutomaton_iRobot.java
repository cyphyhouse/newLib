package edu.illinois.mitra.cyphyhouse.harness;

import edu.illinois.mitra.cyphyhouse.Handler.IPCHandler;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomaton_iRobot;
import edu.illinois.mitra.cyphyhouse.motion.MotionHandlerConfig;
import edu.illinois.mitra.cyphyhouse.objects.Common;

@Deprecated
public class RealisticSimMotionAutomaton_iRobot extends MotionAutomaton_iRobot {
	private String name;
	IPCHandler myHandler = null;
	
	public RealisticSimMotionAutomaton_iRobot(GlobalVarHolder gvh, IPCHandler handler) {
		super(gvh);
		name = gvh.id.getName();
		myHandler = handler;
	}

	@Override
	public void motion_stop() {
//		gpsp.setVelocity(name, 0, 0);
        myHandler.obtaintMsg(MotionHandlerConfig.CMD_IROBOT_MOTION_STOP, name).sendToHandler();
		super.running = false;
		super.stage = MotionAutomaton_iRobot.STAGE.INIT;
		super.destination = null;
		super.inMotion = false;
	}

	@Override
	protected void curve(double velocity, double radius) {
		if(running) {
			sendMotionEvent(Common.MOT_ARCING, velocity, radius);
			// TODO: Determine if angular velocity formula works! 
//			gpsp.setVelocity(name, velocity, (int) Math.round((velocity*360.0)/(2*Math.PI*radius)));
			myHandler.obtaintMsg(MotionHandlerConfig.CMD_IROBOT_CURVE, name,
					velocity, Math.round((velocity*360.0)/(2*Math.PI*radius))).sendToHandler();
		}
	}

	@Override
	protected void straight(double velocity) {
		gvh.log.i(TAG, "Straight at velocity " + velocity);
		if(running) {
			if(velocity != 0) {
				sendMotionEvent(Common.MOT_STRAIGHT, velocity);
			} else {
				sendMotionEvent(Common.MOT_STOPPED, 0);
			}
//			gpsp.setVelocity(name, velocity, 0);
            myHandler.obtaintMsg(MotionHandlerConfig.CMD_IROBOT_STRAIGHT, name,
					velocity, 0).sendToHandler();
		}
	}

	@Override
	protected void turn(double velocity, double angle) {
		if(running) {
			sendMotionEvent(Common.MOT_TURNING, velocity, angle);
//			gpsp.setVelocity(name, 0, (int) Math.copySign(velocity, -angle));
            myHandler.obtaintMsg(MotionHandlerConfig.CMD_IROBOT_TURN, name,
					0, Math.copySign(velocity, -angle)).sendToHandler();
		}
	}	
	

	@Override
	public void cancel() {
		super.running = false;
	}
}
