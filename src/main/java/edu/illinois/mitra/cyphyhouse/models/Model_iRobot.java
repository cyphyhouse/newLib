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
public class Model_iRobot extends ItemPosition implements TrackedRobot{
	// for default values, see initial_helper()
	public double angle;
	public double radius;
	public int type;
	public double velocity;
	
	public boolean leftbump;
	public boolean rightbump;
	public boolean circleSensor;
		
	public double vFwd;
	public double vRad;
	public Random rand;
	public double x_p;
	public double y_p;
	public double angle_p;

	public double TESTX;
	
	/**
	 * Construct an Model_iRobot from a received GPS broadcast message
	 * 
	 * @param received GPS broadcast received 
	 * @throws ItemFormattingException
	 */

	public Model_iRobot(String received) throws ItemFormattingException{
		initial_helper();
		String[] parts = received.replace(",", "").split("\\|");
		if(parts.length == 7) {
			this.name = parts[1];
			this.x = Integer.parseInt(parts[2]);
			this.y = Integer.parseInt(parts[3]);
			this.z = Integer.parseInt(parts[4]);
			this.angle = Integer.parseInt(parts[5]);
		} else {
			throw new ItemFormattingException("Should be length 7, is length " + parts.length);
		}

	}
	
	public Model_iRobot(String name, double x, double y) {
		super(name, x, y);
		initial_helper();
	}
	
	public Model_iRobot(String name, double x, double y, double angle) {
		super(name, x, y);
		initial_helper();
		this.angle = angle;
	}
	
	public Model_iRobot(String name, double x, double y, double angle, double radius) {
		super(name, x, y);
		initial_helper();
		this.angle = angle;
		this.radius = radius;
	}
	
	public Model_iRobot(ItemPosition t_pos) {
		super(t_pos.name, t_pos.x, t_pos.y, t_pos.z);
		initial_helper();
		this.angle = t_pos.index;
		// TODO Auto-generated constructor stub
	}
	
	@Override 
	public String toString() {
		return name + ": " + x + ", " + y + ", " + z + ", angle " + angle;
	}

	/** 
	 * 
	 * @return true if one robot is facing another robot/point
	 */
	public boolean isFacing(Point3d other) { 
		if(other == null) {
			return false;
		}
		if(other.x == this.x && other.y == this.y){
			return true;
		}
    	double angleT = Math.toDegrees(Math.atan2((other.y - this.y) , (other.x - this.x)));
    	if(angleT  == 90){
    		if(this.y < other.y)
    			angleT = angleT + 90;
    		double temp = this.angle % 360;
    		if(temp > 0)
    			return true;
    		else
    			return false;
    	}
		if(angleT < 0)
		{
			angleT += 360;
		}
		double angleT1, angleT2, angleself;
		angleT1 = (angleT - 90) % 360;
		if(angleT1 < 0)
		{
			angleT1 += 360;
		}
		angleT2 = (angleT + 90) % 360;
		if(angleT2 < 0)
		{
			angleT2 += 360;
		}
		angleself = this.angle % 360;
		if(angleself < 0)
		{
			angleself += 360;
		}
		if(angleT2 <= 180)
		{
			if((angleself < angleT1) && (angleself > angleT2))
				return false;
			else
				return true;
		}
		else
		{
			if(angleself > angleT2 || angleself < angleT1)
				return false;
			else
				return true;
				
		}
	}

	/** 
	 * @param other The ItemPosition to measure against
	 * @return Number of degrees this position must rotate to face position other
	 */
	public <T extends Point3d> double angleTo(T other) {
		if(other == null) {
			return 0;
		}
		
		double delta_x = other.x - this.x;
		double delta_y = other.y - this.y;
		double angle = this.angle;
		double otherAngle = Math.toDegrees(Math.atan2(delta_y,delta_x));
		if(angle > 180) {
			angle -= 360;
		}
		double retAngle = Common.min_magnitude((otherAngle - angle),(angle - otherAngle));
		retAngle = retAngle%360;
		if(retAngle > 180) {
			retAngle = retAngle-360;
		}
		if(retAngle <= -180) {
			retAngle = retAngle+360;
		}
		if(retAngle > 180 || retAngle< -180){
			System.out.println(retAngle);
		}
		return  retAngle;
	}
	
	public void setPos(double x, double y, double angle) {
		this.x = x;
		this.y = y;
		this.angle = angle;
	}
	
	public void setPos(Model_iRobot other) {
		this.x = other.x;
		this.y = other.y;
		this.angle = other.angle;
	}
	
	
	
	private void initial_helper(){
		/* read from config file, maybe stored on platform */
		angle = 0;
		radius = 325;
		type = 0;
		velocity = 0;
		leftbump = false;
		rightbump = false;
		circleSensor = false;
		vFwd = 0;
		vRad = 0;
	}

	@Override
	public Point3d predict(double[] noises, double timeSinceUpdate) {
		if(noises.length != 3){
			System.out.println("Incorrect number of noises parameters passed in, please pass in x noise, y, noise and angle noise");
			return new Point3d(x,y);
		}
		double xNoise = (rand.nextDouble()*2*noises[0]) - noises[0];
		double yNoise = (rand.nextDouble()*2*noises[1]) - noises[1];
		double aNoise = (rand.nextDouble()*2*noises[2]) - noises[2];
		
		double dX = 0, dY = 0;
		double dA = 0;
		// Arcing motion
		dA = aNoise + (vRad*timeSinceUpdate);
		dX = (xNoise + Math.cos(Math.toRadians(angle))*(vFwd*timeSinceUpdate));
		dY = (yNoise + Math.sin(Math.toRadians(angle))*(vFwd*timeSinceUpdate));
		x_p = x+dX;
		y_p = y+dY;
		angle_p = angle+dA;
		return new Point3d(x_p,y_p);
	}

	@Override
	public void collision(Point3d collision_point) {
		// No collision point, set both sensor to false
		if(collision_point == null){
			rightbump = false;
			leftbump = false;
			return;
		}
		if(isFacing(collision_point)){
			if(angleTo(collision_point)%90>(-20)){
				rightbump = true;
			}
			if(angleTo(collision_point)%90<20){
				leftbump = true;
			}
		}
		else{
			rightbump = false;
			leftbump = false;
		}
		
		//TODO update local map
	}

	@Override
	public void updatePos(boolean followPredict) {
		if(followPredict){
			angle = angle_p;
			x = x_p;
			y = y_p;
		}
	}

	@Override
	public boolean inMotion() {
		return (vFwd != 0 || vRad != 0);
	}

	@Override
	public void updateSensor(ObstacleList obspoint_positions, PositionList<ItemPosition> sensepoint_positions) {
		
		for(ItemPosition other : sensepoint_positions.getList()) {
			if(distanceTo(other)<600){
				if(!obspoint_positions.badPath(this, other)){
					circleSensor = true;
					return;
				}
			}
		}
		return;
	}

	@Override
	public void initialize() {
		rand = new Random(); //initialize random variable for TrackedRobot
	}
}
