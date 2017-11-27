package edu.illinois.mitra.cyphyhouse.models;

import java.util.Random;

import edu.illinois.mitra.cyphyhouse.exceptions.ItemFormattingException;
import edu.illinois.mitra.cyphyhouse.interfaces.TrackedRobot;
import edu.illinois.mitra.cyphyhouse.objects.Common;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.Point3d;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;

/**
 * This class represents a simple model of the Quadcopter
 * 
 */
public class Model_quadcopter extends ItemPosition implements TrackedRobot{
	// for default values, see initial_helper()

	public double radius;
	
	public Random rand;

	public boolean reached;
	
	/**
	 * Construct an Model_iRobot from a received GPS broadcast message
	 * 
	 * @param received GPS broadcast received 
	 * @throws ItemFormattingException
	 */

	public Model_Quadcopter(String received) throws ItemFormattingException{

		// To be modified later....
		initial_helper();
		/*String[] parts = received.replace(",", "").split("\\|");
		if(parts.length == 7) {
			this.name = parts[1];
			this.x = Integer.parseInt(parts[2]);
			this.y = Integer.parseInt(parts[3]);
			this.z = Integer.parseInt(parts[4]);
			this.angle = Integer.parseInt(parts[5]);
		} else {
			throw new ItemFormattingException("Should be length 7, is length " + parts.length);
		}*/

	}
	
	public Model_Quadcopter(String name, int x, int y) {
		super(name, x, y);
		initial_helper();
	}
	
	
	public Model_Quadcopter(String name, int x, int y, double radius) {
		super(name, x, y);
		initial_helper();
		this.radius = radius;
	}
	
	
	@Override 
	public String toString() {
		return name + ": " + x + ", " + y + ", " + z;
	}

	
	
	
	private void initial_helper(){
		/* read from config file, maybe stored on platform */
		radius = 500;
        
	}

	@Override
	public Point3d predict(double[] noises, double timeSinceUpdate) {
		return new Point3d(0,0);
	}

	@Override
	public void collision(Point3d collision_point) {
		// Anything that needs to be done here for collision? on Java side?
	}

	@Override
	public void updatePos(boolean followPredict) {
		
	}

	@Override
	public boolean inMotion() {
		return true;
	}

	@Override
	public void updateSensor(ObstacleList obspoint_positions, PositionList<ItemPosition> sensepoint_positions) {
		
	}

	@Override
	public void initialize() {
		rand = new Random(); //initialize random variable for TrackedRobot
	}
}
