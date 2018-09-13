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
    int lineno = 0;
    int eraseline = -1;
    private int numBots;
    private int numWaypoints;
    private boolean arrived = false;
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
    private int testindex;
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
        robotIndex = Integer.parseInt(intValue);
        numBots = gvh.id.getParticipants().size();
        dsm = new DSMMultipleAttr(gvh); 
        mutex0 = new GroupSetMutex(gvh,0);

    }

    @Override
    public List<Object> callStarL() {
         

        dsm.createMW("testindex",0);
        while(true) {
            testindex = Integer.parseInt(dsm.get("testindex","*"));

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
                        System.out.println("HERE1");
                        break;
                    } else {
                        int numwaypoints = destinations.size();
                        if (index >= numwaypoints)
                           stage = Stage.WAIT;
                        if(!wait0){	
						mutex0.requestEntry(0);
						wait0 = true;
              
					}
					if(mutex0.clearToEnter(0)){
						testindex = testindex +1;
                                                System.out.println("incrementing testindex "+robotIndex);
						dsm.put("testindex", "*", testindex + 1);
						mutex0.exit(0);
					}
                        System.out.println("testindex:"+ testindex);
                        currentDestination = getDestination(destinations, testindex);
                        index++;
                        destinations.remove(currentDestination.getName());
                        gvh.plat.moat.goTo(currentDestination);
                        dgt = true;
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
                       wait0 = false;
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
}
