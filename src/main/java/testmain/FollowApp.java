package testmain;

/**
 * Created by VerivitalLab on 2/26/2016.
 * This app was created to test the drones. The bots will each go to an assigned waypoint.
 * Once both bots have arrived at their respective waypoints, they will then go to the next waypoints.
 */
import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.nio.file.*;
import java.util.stream.Stream;

import edu.illinois.mitra.cyphyhouse.functions.DSMMultipleAttr;
import edu.illinois.mitra.cyphyhouse.comms.RobotMessage;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.motion.MotionParameters;
import edu.illinois.mitra.cyphyhouse.motion.RRTNode;
import edu.illinois.mitra.cyphyhouse.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;
import edu.illinois.mitra.cyphyhouse.interfaces.DSM;
import edu.illinois.mitra.cyphyhouse.functions.GroupSetMutex;
import edu.illinois.mitra.cyphyhouse.interfaces.MutualExclusion;


public class FollowApp extends LogicThread {
    private static final String TAG = "Follow App";
    private static final int DEST_MSG = 23;
    private static final int ERASE_MSG = 24;
    private int destIndex;
    int lineno = 0;
    int eraseline = -1;
    private int numBots;
    private int numWaypoints;
    private boolean arrived = false;
    private boolean goForever = false;
    private int robotIndex;
    private DSM dsm;
    private boolean wait0 = false;
    private MutualExclusion mutex0; 
    boolean dgt = false;
    boolean takeoff = false;
    private HashSet<RobotMessage> receivedMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> erasedMsgs = new HashSet<RobotMessage>();
  

    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
    final Map<String, Integer> erasemap = new HashMap<String, Integer>();
    // execute a function that takes a string and returns a string
    ItemPosition currentDestination;
    private enum Stage {
        PICK, GO, DONE, WAIT
    }; 
    private int index;
    private Stage stage = Stage.PICK;
    boolean connected = false;
    public FollowApp(GlobalVarHolder gvh) {
        super(gvh);
        MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
        settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);
        gvh.comms.addMsgListener(this, DEST_MSG);
        gvh.comms.addMsgListener(this, ERASE_MSG);

        // bot names must be bot0, bot1, ... botn for this to work
        String intValue = name.replaceAll("[^0-9]", "");
        destIndex = 0;
        robotIndex = Integer.parseInt(intValue);
        numBots = gvh.id.getParticipants().size();
        dsm = new DSMMultipleAttr(gvh); 
        mutex0 = new GroupSetMutex(gvh,0);

    }

    @Override
    public List<Object> callStarL() {
         
    
        while(true) {
            System.out.println("ROBOT INDEX= "+ robotIndex + "stage: " + stage); 
            switch(stage) {
                case PICK:
                    arrived = false;
		    String line;
                        //System.out.println(lineno+" "+robotIndex); 
                        try (Stream<String> lines = Files.lines(Paths.get("tasks.txt"))) {
                           line = lines.skip(lineno).findFirst().get();      
                            
                           RobotMessage inform = new RobotMessage("ALL", name, DEST_MSG, line);
                           gvh.comms.addOutgoingMessage(inform);
                           lineno  = lineno +1;
     
                        }
                        catch (IOException e) {}
                        catch (NoSuchElementException e) {stage = Stage.WAIT;lineno = lineno - 1;}
                        catch (IllegalArgumentException e) {stage = Stage.WAIT;lineno = 0;}
                    if(destinations.isEmpty()||robotIndex == 0) {
                        stage = Stage.WAIT;
                        //System.out.println("HERE1");
                    } else {
			System.out.println("HERE1");
                        int numwaypoints = destinations.size();
                        if (index >= numwaypoints) {
			 System.out.println("GOING TO WAIT");
                           stage = Stage.WAIT;
                           break;
                        }
                        try {
                        currentDestination = getDestination(destinations, index);
                        }
                        catch (NullPointerException e) {stage = Stage.DONE;lineno = lineno - 1; break;}
                        int z = currentDestination.getZ(); 
                  
                        if(!takeoff && z>=150 && robotIndex == 1) {
                          System.out.println("taking off");
                              //destinations.remove(currentDestination.getName());
                              gvh.plat.moat.goTo(currentDestination);
                              dgt = true;
                              takeoff = true;
                              stage = Stage.GO;
                              break;
                        }

			if(!takeoff && z<=125 && robotIndex == 2) {
                          System.out.println("taking off");
                              //destinations.remove(currentDestination.getName());
                              gvh.plat.moat.goTo(currentDestination);
                              dgt = true;
                              takeoff = true;
                              stage = Stage.GO;
                              break;
                        }

			index++;
			System.out.println("z value is: " + z);

			if (z == -100 && robotIndex == 1){
			      System.out.println(currentDestination.getZ()+" "+robotIndex);
                              //destinations.remove(currentDestination.getName());
                              gvh.plat.moat.goTo(currentDestination);
                              dgt = true;
			      stage = Stage.DONE;
			} 
			if (z == -200 && robotIndex == 1){
			      System.out.println(currentDestination.getZ()+" "+robotIndex);
                              //destinations.remove(currentDestination.getName());
                              gvh.plat.moat.goTo(currentDestination);
                              dgt = true;
			      stage = Stage.DONE;
			}

                        if (z >= 150 && robotIndex == 1) {
			
                              System.out.println(currentDestination.getZ()+" "+robotIndex);
                              //destinations.remove(currentDestination.getName());
                              gvh.plat.moat.goTo(currentDestination);
                              dgt = true;
                        }
                        else  {
                            if (z < 125 && robotIndex == 2) {
                              System.out.println(currentDestination.getZ()+" "+robotIndex);
                              //destinations.remove(currentDestination.getName());
                              gvh.plat.moat.goTo(currentDestination);
                              dgt = true;
                            } 
                            else {
                            }
                        }
                        //System.out.println(currentDestination.toString());
                        //System.out.println("HERE2");
                        stage = Stage.GO;
                    }
                    break;
                case GO:
                    if(!gvh.plat.moat.inMotion) {
                       if (!arrived && currentDestination != null){
                          stage = Stage.WAIT;}
                       else {
                          if (dgt == true) {
                       eraseline++;
                       RobotMessage erase = new RobotMessage("ALL",name, ERASE_MSG, Integer.toString(eraseline));
                       gvh.comms.addOutgoingMessage(erase);
                       dgt = false;
                       }
                          stage = Stage.PICK;
                          break;
                       }
                       arrived = true;
                    }
                    break;
                case WAIT:
			System.out.println("IN WAIT");
                    if (arrived && robotIndex != 0) { 
                       stage = Stage.PICK;
                    }
                    if (robotIndex == 0)
                       stage = Stage.PICK;
                    stage = Stage.GO;
                    break;
                case DONE:
                    return null;
            }
            sleep(100);
        }
    }

    @Override
    protected void receive(RobotMessage m) {
        boolean alreadyReceived = false;
        for(RobotMessage msg : receivedMsgs) {
            if(msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
                alreadyReceived = true;
                break;
            }
        }
        for(RobotMessage msg : erasedMsgs) {
            if(msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
                alreadyReceived = true;
                break;
            }
        }
       int i = receivedMsgs.size(); 
       int j = erasedMsgs.size(); 
       if (m.getMID() == DEST_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            receivedMsgs.add(m);
            gvh.log.d(TAG, "received destination message from " + m.getFrom());

            String dest = m.getContents().toString();
            dest = dest.replace(" ",",").replace("`","");
            String[] parts = dest.split(",");
            int x = (int) (Float.parseFloat(parts[0])*100);
            int y = (int) (Float.parseFloat(parts[1])*100);
            int z = (int) (Float.parseFloat(parts[2])*100);
            String name = Integer.toString(i) +"-A";
            ItemPosition p = new ItemPosition(name,x,y,z);
            destinations.put(p.getName(),p);
              
        }
       if (m.getMID() == ERASE_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            erasedMsgs.add(m);
            gvh.log.d(TAG, "received erase message from " + m.getFrom());
            String linenum = m.getContents().toString().replace("`","");
            int x = Integer.parseInt(linenum);
            try(FileWriter fw = new FileWriter("erase.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
            {
               out.println(x);
              } catch (IOException e) {
               //exception handling left as an exercise for the reader
               }

        }
       
       
    }

    @SuppressWarnings("unchecked")
    private <X, T> T getDestination(Map<X, T> map, int index) {
        // Keys must be 0-A format for this to work
        String key = Integer.toString(index) + "-A";
        // this is for key that is just an int, no -A
        //String key = Integer.toString(index);
        return map.get(key);
    }
    
    private boolean getXsection(double[] x1, double[] x2, double[] x3, double[] x4) {
        double[] p1 = new double[3];
        double[] p2 = new double[3];
        double[] tmp1 = new double[3];
        double[] tmp2 = new double[3];
        p1 = sub(x2, x1, 3);
        p2 = sub(x4, x3, 3);
        
        if (p1[2] == 0) {//Stay at same height
            tmp1 = cross(p1, x3);
            if (tmp1[2] == 0) { 
                // intersection happens
                return true;
            }
        }
        else if (p2[2] == 0) {
            tmp1 = cross(p2, x1);
            if (tmp1[2] == 0) {
                // intersection
                return true;
            }
        }
        else if (Math.abs(x2[2] - x4[2]) < 0.5) {
            // If the final heights are close enough, check if paths intersect
            tmp1 = cross(p1, x3);
            tmp2 = cross(p1, x4);
            double s1 = Math.signum(tmp1[2]);
            double s2 = Math.signum(tmp2[2]);
            if ((s1 != s2) && ((s1 != 0) || (s2 != 0))) {
                //intersection happens
                return true;
            }
        } 
        
        return false;
    }
    
    private double[] sub(double[] A, double[] B, int cols) {
        double[] C = new double[cols];
        for(int i = 0; i < cols; i++) {
            C[i] = A[i] - B[i];
        }
        return C;
    }
    
    private double[] cross(double[] u, double[] v) {
        double[] w = new double[3];
        w[0] = u[1]*v[2] - u[2]*v[1];
        w[1] = -(u[0]*v[2] - u[2]*v[0]);
        w[2] = u[0]*v[1] - u[1]*v[0];
        return w;
    }
}








/**
 * Created by VerivitalLab on 2/26/2016.
 * This app was created to test the drones. The bots will each go to an assigned waypoint.
 * Once both bots have arrived at their respective waypoints, they will then go to the next waypoints.
 */

/*
import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.nio.file.*;
import java.util.stream.Stream;

import edu.illinois.mitra.cyphyhouse.functions.DSMMultipleAttr;
import edu.illinois.mitra.cyphyhouse.comms.RobotMessage;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.motion.MotionParameters;
import edu.illinois.mitra.cyphyhouse.motion.RRTNode;
import edu.illinois.mitra.cyphyhouse.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;
import edu.illinois.mitra.cyphyhouse.interfaces.DSM;
import edu.illinois.mitra.cyphyhouse.functions.GroupSetMutex;
import edu.illinois.mitra.cyphyhouse.interfaces.MutualExclusion;


public class FollowApp extends LogicThread {
    private static final String TAG = "Follow App";
    private static final int DEST_MSG = 23;
    private static final int ERASE_MSG = 24;
    private int destIndex;
    int lineno = 0;
    int eraseline = -1;
    private int numBots;
    private int numWaypoints;
    private boolean arrived = false;
    private boolean goForever = false;
    private int robotIndex;
    private DSM dsm;
    private boolean wait0 = false;
    private MutualExclusion mutex0; 
    boolean dgt = false;
    private HashSet<RobotMessage> receivedMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> erasedMsgs = new HashSet<RobotMessage>();

    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
    final Map<String, Integer> erasemap = new HashMap<String, Integer>();
    // execute a function that takes a string and returns a string
    ItemPosition currentDestination;
    private enum Stage {
        PICK, GO, DONE, WAIT
    }; 
    private int index;
    private Stage stage = Stage.PICK;
    boolean connected = false;
    public FollowApp(GlobalVarHolder gvh) {
        super(gvh);
        MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
        settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);
        gvh.comms.addMsgListener(this, DEST_MSG);
        gvh.comms.addMsgListener(this, ERASE_MSG);

        // bot names must be bot0, bot1, ... botn for this to work
        String intValue = name.replaceAll("[^0-9]", "");
        destIndex = 0;
        robotIndex = Integer.parseInt(intValue);
        numBots = gvh.id.getParticipants().size();
        dsm = new DSMMultipleAttr(gvh); 
        mutex0 = new GroupSetMutex(gvh,0);

    }

    @Override
    public List<Object> callStarL() {
         

        while(true) {
            System.out.println(stage+ " "+ robotIndex); 
            switch(stage) {
                case PICK:
                    arrived = false;
		    String line;
                        //System.out.println(lineno+" "+robotIndex); 
                        try (Stream<String> lines = Files.lines(Paths.get("tasks.txt"))) {
                           line = lines.skip(lineno).findFirst().get();  
    			   //System.out.println("======================================");
                           //lines.forEach(System.out::println);
			   //System.out.println("======================================");
                           RobotMessage inform = new RobotMessage("ALL", name, DEST_MSG, line);
                           gvh.comms.addOutgoingMessage(inform);
                           lineno  = lineno +1;
     
                        }
                        catch (IOException e) {}
                        catch (NoSuchElementException e) {stage = Stage.WAIT;lineno = lineno - 1;}
                        catch (IllegalArgumentException e) {stage = Stage.WAIT;lineno = 0;}
                        
                    if(destinations.isEmpty()||robotIndex == 0) {
                        stage = Stage.WAIT;
                        System.out.println("HERE1");
                    } else {
                        int numwaypoints = destinations.size();
                        if (index >= numwaypoints)
                           stage = Stage.WAIT;
                        currentDestination = getDestination(destinations, index);
                        index++;
                        System.out.println(currentDestination.toString());
                        destinations.remove(currentDestination.getName());
                        gvh.plat.moat.goTo(currentDestination);
                        dgt = true;
                        System.out.println("HERE2");
                        stage = Stage.GO;
                    }
                    break;
                case GO:
			
			System.out.println("MOTION FLAG "+gvh.plat.moat.inMotion);
                    if(!gvh.plat.moat.inMotion) {
			
			System.out.println("IN GO");
                       if (!arrived && currentDestination != null){
                          stage = Stage.WAIT;}
                       else {
                          if (dgt == true) {
				System.out.println("IN ERASE");
                       eraseline++;
                       RobotMessage erase = new RobotMessage("ALL",name, ERASE_MSG, Integer.toString(eraseline));
		       
                       gvh.comms.addOutgoingMessage(erase);
                       dgt = false;
                       }
                          stage = Stage.PICK;
                          break;
                       }
                       arrived = true;
                    }
                    break;
                case WAIT:
                    if (arrived && robotIndex != 0) { 
                       stage = Stage.PICK;
                    }
                    if (robotIndex == 0)
                       stage = Stage.PICK;
                    stage = Stage.GO;
                    break;
                case DONE:
                    return null;
            }
            sleep(100);
        }
    }

    @Override
    protected void receive(RobotMessage m) {
        boolean alreadyReceived = false;
        for(RobotMessage msg : receivedMsgs) {
            if(msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
                alreadyReceived = true;
                break;
            }
        }
        for(RobotMessage msg : erasedMsgs) {
            if(msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
                alreadyReceived = true;
                break;
            }
        }
       int i = receivedMsgs.size(); 
       int j = erasedMsgs.size(); 
       if (m.getMID() == DEST_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            receivedMsgs.add(m);
            gvh.log.d(TAG, "received destination message from " + m.getFrom());

            String dest = m.getContents().toString();
            dest = dest.replace(" ",",").replace("`","");
            String[] parts = dest.split(",");
            int x = (int) (Float.parseFloat(parts[0])*100);
            int y = (int) (Float.parseFloat(parts[1])*100);
            int z = (int) (Float.parseFloat(parts[2])*100);
            String name = Integer.toString(i) +"-A";
            ItemPosition p = new ItemPosition(name,x,y,z);
            destinations.put(p.getName(),p);
              
        }
       if (m.getMID() == ERASE_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            erasedMsgs.add(m);
            gvh.log.d(TAG, "received erase message from " + m.getFrom());
            String linenum = m.getContents().toString().replace("`","");
            int x = Integer.parseInt(linenum);
            try(FileWriter fw = new FileWriter("erase.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
            {
               out.println(x);
              } catch (IOException e) {
               //exception handling left as an exercise for the reader
               }

        }
       
       
    }

    @SuppressWarnings("unchecked")
    private <X, T> T getDestination(Map<X, T> map, int index) {
        // Keys must be 0-A format for this to work
        String key = Integer.toString(index) + "-A";
        // this is for key that is just an int, no -A
        //String key = Integer.toString(index);
        return map.get(key);
    }
}*/
