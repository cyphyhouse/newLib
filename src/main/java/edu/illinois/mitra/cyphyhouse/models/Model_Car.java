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
 * This class represents a simple model of the iRobot Create, including angle, radius, type, velocity, leftbump, rightbump, circleSensor, vFwd, vRad
 * and some prediction on x and y based on vFwd and vRad
 * 
 * default type:
 *	0: get to goal robot
 *	behavior: marks the unknown obstacle when collide, redo path planning (get around the obstacle)to reach the goal
 *	1: explore the area robot
 *	behavior: explore the shape of the unknown obstacle and sent out the shape to others
 *	2: random moving obstacle robot 
 *	behavior:acts as simple moving obstacle
 *	3: anti goal robot
 *	behavior:acts as AI opponent try to block robots getting to the goal
 * @author Yixiao Lin
 * @version 1.0
 */
public class Model_Car extends ItemPosition implements TrackedRobot{
	// for default values, see initial_helper()

	public double turn_radius;
	public double velocity;
	
	public boolean leftbump;
	public boolean rightbump;
		
	public double vFwd;
	public double vRad;
	public Random rand;

	public boolean reached;
	
	/**
	 * Construct an Model_iRobot from a received GPS broadcast message
	 * 
	 * @param received GPS broadcast received 
	 * @throws ItemFormattingException
	 */

	public Model_Car(String received) throws ItemFormattingException{

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
	
	public Model_Car(String name, int x, int y) {
		super(name, x, y);
		initial_helper();
	}
	
	
	public Model_Car(String name, int x, int y, double turn_radius) {
		super(name, x, y);
		initial_helper();
		this.turn_radius = turn_radius;
	}
	
	
	@Override 
	public String toString() {
		return name + ": " + x + ", " + y + ", " + z;
	}

	
	
	
	private void initial_helper(){
		/* read from config file, maybe stored on platform */
		turn_radius = 325;
		velocity = 0;
		leftbump = false;
		rightbump = false;
		vFwd = 0;
		vRad = 0;
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
		return (vFwd != 0 || vRad != 0);
	}

	@Override
	public void updateSensor(ObstacleList obspoint_positions, PositionList<ItemPosition> sensepoint_positions) {
		
	}

	@Override
	public void initialize() {
		rand = new Random(); //initialize random variable for TrackedRobot
	}
}
