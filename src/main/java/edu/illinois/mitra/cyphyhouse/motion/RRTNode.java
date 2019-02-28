package edu.illinois.mitra.cyphyhouse.motion;

import java.util.Stack;

import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeySizeException;
import edu.illinois.mitra.cyphyhouse.objects.Point;


/**
 * This implements RRT path finding algorithm using kd tree
 *
 * @author Yixiao Lin
 * @version 1.0
 */

public class RRTNode {
	public Point position = new Point(0,0);
	public double heading;
	public RRTNode parent;
	public RRTNode stopNode;
	public KDTree<RRTNode> kd;
	//	public LinkedList<ItemPosition> pathList = new LinkedList<ItemPosition>();

	public double [] getValue(){
		double [] toReturn = {position.x,position.y};
		return toReturn;
	}

	public RRTNode(){
		position.x = 0;
		position.y = 0;
		parent = null;
		heading = 0;
	}

	public RRTNode(double x, double y){
		position.x = x;
		position.y = y;
		parent = null;
		heading = 0;
	}

	public RRTNode(RRTNode copy){
		position.x = copy.position.x;
		position.y = copy.position.y;
		parent = copy.parent;
		heading = copy.heading;
	}

	public void setHeading(double heading){
		this.heading = heading;
	}

	/**
	 * methods to find the route
	 if find a path, return a midway point stack
	 if can not find a path, return null
	 remember to handle the null stack when writing apps using RRT path planning
	 the obstacle list will be modified to remove any obstacle that is inside a robot
	 * @param destination
	 * @param K
	 * @param obsList
	 * @param xLower
	 * @param xUpper
	 * @param yLower
	 * @param yUpper
	 * @param RobotPos
	 * @param radius
	 * @return
	 */

	public Stack<ItemPosition> findRoute(double InitHeading, ItemPosition destination, int K, ObstacleList obsList, double xLower, double xUpper, double yLower, double yUpper, ItemPosition RobotPos, double radius) {
		//TODO: add Steer here
		//initialize a kd tree;
		//obsList.remove(RobotPos, 0.9*Radius);
		if(xLower > xUpper || yLower> yUpper){
			System.err.println("Lower bound must be smaller or equal to than upper bound");
			return null;
		}
		kd = new KDTree<RRTNode>(2);
		//double [] root = {position.x,position.y};
		//final RRTNode rootNode = new RRTNode(position.x,position.y);
		double [] root = {RobotPos.x,RobotPos.y};
		final RRTNode rootNode = new RRTNode(RobotPos.x,RobotPos.y);
		rootNode.setHeading(InitHeading);
		final RRTNode destNode = new RRTNode(destination.x, destination.y);

		try{
			kd.insert(root, rootNode);
		}
		catch(Exception e){
			System.err.println(e);
		}

		RRTNode currentNode = new RRTNode(rootNode);
		RRTNode addedNode = new RRTNode(rootNode);

		//for(i< k)  keep finding
		for(int i = 0; i<K; i++){
			//if can go from current to destination, meaning path found, add destinationNode to final, stop looping.
			//System.out.println("i is: " + i );

			//System.out.println("curnode: " + addedNode.position.x+" "+ addedNode.position.y);
			//System.out.println("destnode: " + destNode.position.x+" "+ destNode.position.y);
			//System.out.println("RADIUS: " + radius);

			boolean validpath = false;
			try{
				validpath = obsList.validPath(destNode, addedNode, radius);
			}
			catch(Exception e){
				//System.out.println("VALID PATH FUNCTION FAILED");

			}

			if(validpath){
				destNode.parent = addedNode;
				stopNode = destNode;
				try{
					kd.insert(destNode.getValue(), destNode);
				}
				catch (Exception e) {
					System.err.println(e);
				}
				//System.out.println("Path found!");
				break;
			}

			//Path not found yet, keep exploring

			//Generate a random sample from the current space
			//No obstacles at the moment so just generate the destination as our "randomly" sampled point
			boolean validRandom = false;
			ItemPosition sampledPos = random_sampler(destination.x, destination.y, destination.z);

			//Next, find the node in the tree that is closest to the sampledPos
			RRTNode sampledNode = new RRTNode(sampledPos.x, sampledPos.y);
			try{
				currentNode = kd.nearest(sampledNode.getValue());
			}
			catch (Exception e) {

				System.err.println(e);
			}


			//From the current node, generate a new node to add to the tree by using the car_sampler
			//We sample 5 times and take the point that is closest to the destination
			double closest_dist = Double.MAX_VALUE;

			double [] best_sampled_point = {};
			for(int j=0; j<50; j++) {

				double [] car_sampler_input = {currentNode.position.x, currentNode.position.y, currentNode.heading};
				double [] car_sampler_point = car_sample(car_sampler_input);
				/*if(car_sampler_point[0] > xUpper || car_sampler_point[0] < xLower || car_sampler_point[1] > yUpper || car_sampler_point[1] < yLower){
					j--;
					continue;
				}*/
				double dist_to_dest = Math.sqrt(Math.pow(car_sampler_point[0]-destination.x, 2) + Math.pow(car_sampler_point[1]-destination.y, 2));
				if(dist_to_dest < closest_dist) {
					best_sampled_point = car_sampler_point;
					closest_dist = dist_to_dest;
				}
			}

			//Finally, we add this newly generated node to the RRT
			RRTNode newNode = new RRTNode(best_sampled_point[0], best_sampled_point[1]);

			newNode.setHeading(best_sampled_point[2]);

			newNode.parent = currentNode;
			try{
				kd.insert(newNode.getValue(), newNode);
			}
			catch (Exception e) {
				System.err.println(e);
			}

			addedNode = newNode;

		}

		stopNode = addedNode;

		//after searching, we update the path to a stack

		RRTNode curNode = destNode;
		Stack<ItemPosition> pathStack= new Stack<ItemPosition>();
		while(curNode != null){
			ItemPosition ToGo= new ItemPosition("midpoint", curNode.position.x, curNode.position.y);
			pathStack.push(ToGo);
			curNode = curNode.parent;
		}

		if(destNode.parent == null){
			//System.out.println("Path Not found! Tree size: " + kd.size());
			return(null);
		}
		else{
			stopNode = destNode;
			return pathStack;
		}
	}

	/**
	 * toggle function deals with constrains by the environment as well as robot systems.
	 * It changes sampledNode to some point alone the line of sampledNode and currentNode so that no obstacles are in the middle
	 * In other words, it changes sampledNode to somewhere alone the line where robot can reach
	 *
	 * TODO: we can add robot system constraints later
	 *
	 * @param currentNode
	 * @param sampledNode
	 * @param obsList
	 * @param radius
	 * @return
	 */
	private RRTNode toggle(RRTNode currentNode, RRTNode sampledNode, ObstacleList obsList, double radius) {
		RRTNode toggleNode = new RRTNode(sampledNode);
		int tries = 0;
		// try 20 times, which will shorten it to 0.00317 times the original path length
		// smaller tries might make integer casting loop forever
		while((!obsList.validPath(toggleNode, currentNode, radius)) && (tries < 20)){
			//move 1/4 toward current
			toggleNode.position.x =  ((toggleNode.position.x + currentNode.position.x)/(1.5));
			toggleNode.position.y =  ((toggleNode.position.y + currentNode.position.y)/(1.5));
			tries ++;
		}
		//return currentNode if toggle failed
		// TODO: remove magic number
		if(tries >= 19)
			return null;
		else
			return toggleNode;
	}

	private ItemPosition random_sampler(double x, double y, double z){
		// No obstacles for now so just use the destination as our "randomly" sampled point to get straighter paths
		ItemPosition random_point = new ItemPosition("random_node", x, y, z);
		return random_point;
	}

	private double[] car_sample(double[] root)  {
		double vUpper = 3.0;
		double vLower = 0.1;
		double thetaUpper = 0.30; //radians, about 20 degrees
		double thetaLower =-0.30; //radians

		double vRandom = (Math.random() * (vUpper - vLower)) + vLower;
		//System.out.println(vRandom);
		double thetaRandom = (Math.random() * (thetaUpper - thetaLower)) + thetaLower;

		double d = 0.3; // length of the car
		double dt = 0.1;
		double[] x = {root[0], 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		double[] y = {root[1], 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		double[] phi = {root[2], 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

		for(int i = 0; i<10; i++){
			phi[i+1] = phi[i] + dt*vRandom*Math.tan(thetaRandom)/d;
			x[i+1] = x[i] + dt*vRandom*Math.cos((phi[i+1]+phi[i])/2.0);
			y[i+1] = y[i] + dt*vRandom*Math.sin((phi[i+1]+phi[i])/2.0);
		}

		if (phi[10] > 2*Math.PI) phi[10] -= 2*Math.PI;
		if (phi[10] < -2*Math.PI) phi[10] += 2*Math.PI;

		//System.out.println("Init x: "+x[0]+", end x: "+x[10]+", Init y: "+y[0]+", end y: "+y[10]+", Init Phi: "+phi[0]+", end phi: "+phi[10]+", Vel: "+vRandom+", theta: "+thetaRandom);
		//System.out.println("Init Phi: "+phi[0]+", end phi: "+phi[10]+", Vel: "+vRandom+", theta: "+thetaRandom);
		//System.out.println("Init Phi: "+phi[0]+", end phi: "+phi[10]+", Vel: "+vRandom+", theta: "+thetaRandom);

		double[] out = {x[10], y[10], phi[10]};
		return out;
	}



}

