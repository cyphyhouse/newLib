package testSim.draw;

import java.awt.Graphics2D;

import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;

public abstract class Drawer
{
	/**
	 * Draw the (logic-thread specific) data onto the simulator image
	 * @param lt the instance we need to draw
	 * @param g the graphics to draw with
	 */
	public abstract void draw(LogicThread lt, Graphics2D g);
}
