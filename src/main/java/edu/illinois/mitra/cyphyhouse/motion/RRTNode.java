package edu.illinois.mitra.cyphyhouse.motion;

import java.awt.Point;
import java.util.Stack;

import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * This implements RRT path finding algorithm using kd tree
 * 
 * @author Yixiao Lin
 * @version 1.0
 */

public class RRTNode {
	public Point position = new Point();
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

	public RRTNode(int x, int y){
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

	public Stack<ItemPosition> findRoute(double InitHeading, ItemPosition destination, int K, ObstacleList obsList, int xLower, int xUpper, int yLower, int yUpper, ItemPosition RobotPos, int radius) {
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
			System.out.println("i is: " + i + " root: " + rootNode.getValue()[0] + " dest: " + destNode.getValue()[0]);

			if(obsList.validPath(destNode, addedNode, radius)){
				destNode.parent = addedNode;
				stopNode = destNode;
				try{	
					kd.insert(destNode.getValue(), destNode);
				}
				catch (Exception e) {

					System.err.println(e);
				}
				System.out.println("Path found!");
				break;
			}
			//not find yet, keep exploring
			//random a sample point in the valid set of space
			boolean validRandom = false;
			int xRandom = 0;
			int yRandom = 0;
			ItemPosition sampledPos = new ItemPosition("rand",xRandom, yRandom, 0);

			double [] cur_node_data = {addedNode.getValue()[0], addedNode.getValue()[1], addedNode.heading};
			double [] sampled_point = {};

			int counter = 0;
			while(!validRandom){
				if(counter > 10000){

					addedNode = addedNode.parent.parent;

					cur_node_data[0] = addedNode.getValue()[0];

					cur_node_data[1] = addedNode.getValue()[1];

					cur_node_data[2] = addedNode.heading;

					counter = 0;

				}

				/*xRandom = (int) Math.round((Math.random() * ((xUpper - xLower))));
				yRandom = (int) Math.round((Math.random() * ((yUpper - yLower))));
				sampledPos.x = xRandom + xLower;
				sampledPos.y = yRandom + yLower;
				//System.out.println(sampledPos.x + " " + sampledPos.y);
				validRandom = ((sampledPos.x >= xLower && sampledPos.x <= xUpper) && (sampledPos.y >= yLower && sampledPos.y <= yUpper));*/

				// Sample from car model to get a new point
				sampled_point = car_sample(cur_node_data);

				sampledPos.x = (int)sampled_point[0];
				sampledPos.y = (int)sampled_point[1];

				validRandom = ((sampledPos.x >= xLower && sampledPos.x <= xUpper) && (sampledPos.y >= yLower && sampledPos.y <= yUpper));
				//validRandom = ((sampled_point[0] >= xLower && sampled_point[0] <= xUpper) && (sampled_point[1] >= yLower && sampled_point[1] <= yUpper));

				validRandom = validRandom && obsList.validstarts(sampledPos, radius);
				if(validRandom){

					// added a check to see if sampledPos is already in tree
					boolean notInTree = true;
					RRTNode possibleNode = new RRTNode(sampledPos.x, sampledPos.y);
					//System.out.println(sampledPos.x + " " + sampledPos.y);
					try {
						if(kd.search(possibleNode.getValue()) != null) {
							notInTree = false;
						}
					} catch (KeySizeException e) {

						e.printStackTrace();
					}
					validRandom = (validRandom && notInTree);
				}
				//System.out.println(validRandom);
				counter++;
			}

			RRTNode sampledNode = new RRTNode(sampledPos.x, sampledPos.y);

			sampledNode.setHeading(sampled_point[2]);

			// with a valid random sampled point, we find it's nearest neighbor in the tree, set it as current Node
			try{
				currentNode = kd.nearest(sampledNode.getValue());
			}
			catch (Exception e) {

				System.err.println(e);
			}


			sampledNode = toggle(currentNode, sampledNode, obsList, radius);

			//check if toggle failed
			//if not failed, insert the new node to the tree
			if(sampledNode != null){
				sampledNode.parent = currentNode;
				try{
					kd.insert(sampledNode.getValue(), sampledNode);
				}
				catch (Exception e) {

					System.err.println(e);
				}
				//set currentNode as newest node added, so we can check if we can reach the destination
				addedNode = sampledNode;

				//
			}
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
			System.out.println("Path Not found! Tree size: " + kd.size());
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
	private RRTNode toggle(RRTNode currentNode, RRTNode sampledNode, ObstacleList obsList, int radius) {
		RRTNode toggleNode = new RRTNode(sampledNode);
		int tries = 0;
		// try 20 times, which will shorten it to 0.00317 times the original path length
		// smaller tries might make integer casting loop forever
		while((!obsList.validPath(toggleNode, currentNode, radius)) && (tries < 20)){
			//move 1/4 toward current
			toggleNode.position.x = (int) ((toggleNode.position.x + currentNode.position.x)/(1.5));
			toggleNode.position.y = (int) ((toggleNode.position.y + currentNode.position.y)/(1.5));
			tries ++;
		}
		//return currentNode if toggle failed
		// TODO: remove magic number
		if(tries >= 19)
			return null;
		else
			return toggleNode;
	}

	private double[] car_sample(double[] root)  {
		double vUpper = 3.0;
		double vLower = 0.1;
		double thetaUpper = 0.3; //radians, about 20 degrees
		double thetaLower =-0.3; //radians

		double vRandom = (Math.random() * ((vUpper - vLower)));
		double thetaRandom = (Math.random() * ((thetaUpper - thetaLower)));

		double d = 0.3; // length of the car
		double dt = 0.1;
		double[] x = {root[0], 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		double[] y = {root[1], 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		double[] phi = {root[2], 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

		for(int i = 0; i<10; i++){
			phi[i+1] = phi[i] = dt*vRandom*Math.tan(thetaRandom)/d;
			x[i+1] = x[i] + dt*vRandom*Math.cos((phi[i+1]+phi[i])/2.0);
			y[i+1] = y[i] + dt*vRandom*Math.sin((phi[i+1]+phi[i])/2.0);
		}

		double[] out = {x[10], y[10], phi[10]};
		return out;
	}



}

