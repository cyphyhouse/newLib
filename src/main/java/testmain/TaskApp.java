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


public class TaskApp extends LogicThread {
    private static final String TAG = "Task App";
    private static final int DEST_MSG = 23;
    private int numBots;
    private int numWaypoints;
    private boolean arrived = false;
    private int robotIndex;
    private DSM dsm;
    private boolean wait0 = false;
    private MutualExclusion mutex0;
    boolean dgt = false;
    boolean entered_mutex = false;
    private int turn = 2;
    private HashSet<RobotMessage> receivedMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> erasedMsgs = new HashSet<RobotMessage>();

    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
    final Map<String, Integer> erasemap = new HashMap<String, Integer>();
    // execute a function that takes a string and returns a string
    ItemPosition currentDestination;

    private enum Stage {
        ADD, PICK, GO, DONE, WAIT
    }

    ;
    private int testindex;
    private int num_robots;
    private Stage stage = Stage.ADD;
    boolean connected = false;

    public TaskApp(GlobalVarHolder gvh) {
        super(gvh);
        MotionParameters.Builder settings = new MotionParameters.Builder();
        settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);
        gvh.comms.addMsgListener(this, DEST_MSG);

        // bot names must be bot0, bot1, ... botn for this to work
        String intValue = name.replaceAll("[^0-9]", "");
        robotIndex = Integer.parseInt(intValue);
        numBots = gvh.id.getParticipants().size();
        dsm = new DSMMultipleAttr(gvh);
        mutex0 = new GroupSetMutex(gvh, 0);

    }

    @Override
    public List<Object> callStarL() {


        dsm.createMW("testindex", 0);
        dsm.createMW("num_robots", numBots - 1);

        while (true) {
            mk_list_from_file("tasks.txt", name, DEST_MSG);
            currentDestination = getDestination(destinations, 1);
            System.out.println("size of destinations is: " + destinations.size());
            System.out.println("destinations empty?: " + destinations.isEmpty());
            destinations.remove(currentDestination.getName());
            System.out.println("size of destinations is: " + destinations.size());
            System.out.println("destinations empty?: " + destinations.isEmpty());
            sleep(1500);
        }


       /* while (true) {

            sleep(300);

            testindex = Integer.parseInt(dsm.get("testindex", "*"));


            System.out.println(dsm.get("tasklist", "*", "1"));

            switch (stage) {

                case ADD:
                    mk_list_from_file("tasks.txt", name, DEST_MSG);
                    if(robotIndex != 0)
                        stage = Stage.PICK;


                case PICK:
                    if(robotIndex == 0)
                        break;

                    arrived = false;

                    if (destinations.isEmpty()) {
                        break;
                    } else {
                        int numwaypoints = destinations.size();

                        if (testindex >= numwaypoints)
                            stage = Stage.WAIT;

                        currentDestination = getDestination(destinations, testindex);


                        num_robots = Integer.parseInt(dsm.get("num_robots", "*"));

                        if (!wait0) {

                            mutex0.requestEntry(0);
                            wait0 = true;
                            break;

                        } else if (mutex0.clearToEnter(0) && testindex % num_robots == robotIndex % num_robots) {

                            destinations.remove(currentDestination.getName());
                            gvh.plat.moat.goTo(currentDestination);
                            entered_mutex = true;

                            //mutex0.exit(0);
                        } else {
                            break;
                        }


                        dgt = true;
                        stage = Stage.GO;
                        if (currentDestination.getZ() == 0) {
                            stage = Stage.DONE;
                            if (entered_mutex == true) {
                                testindex = testindex + 1;
                                num_robots = Integer.parseInt(dsm.get("num_robots", "*"));
                                dsm.put("testindex", "*", testindex);
                                dsm.put("num_robots", "*", num_robots - 1);
                                mutex0.exit(0);
                                entered_mutex = false;
                                break;
                            }

                        }
                    }
                    break;
                case GO:

                    if(robotIndex == 0)
                        break;

                    if (!gvh.plat.moat.inMotion) {
                        if (!arrived && currentDestination != null) {
                            stage = Stage.WAIT;
                        } else {
                            if (dgt == true) {
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

                    if(robotIndex == 0)
                        break;

                    if (arrived && robotIndex != 0) {
                        stage = Stage.PICK;
                        //dsm.put("turn", "*", 1);  //setting turn for other robot
                        if (entered_mutex == true) {
                            testindex = testindex + 1;
                            dsm.put("testindex", "*", testindex);
                            mutex0.exit(0);
                            entered_mutex = false;
                            break;
                        }
                    }
                    if (robotIndex == 0)
                        stage = Stage.PICK;
                    stage = Stage.GO;
                    break;
                case DONE:
                    return null;
            }

        }*/
    }

    @Override
    protected void receive(RobotMessage m) {
        boolean alreadyReceived = false;
        for (RobotMessage msg : receivedMsgs) {
            if (msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
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
            dest = dest.replace(" ", ",").replace("`", "");
            String[] parts = dest.split(",");
            int x = (int) (Float.parseFloat(parts[0]) * 100);
            int y = (int) (Float.parseFloat(parts[1]) * 100);
            int z = (int) (Float.parseFloat(parts[2]) * 100);
            String name = Integer.toString(i) + "-A";
            ItemPosition p = new ItemPosition(name, x, y, z);
            destinations.put(p.getName(), p);

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


    private void mk_list_from_file(String filename, String robotname, int msg_type) {
        String line;
        int lineno = 0;

        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            line = lines.skip(lineno).findFirst().get();

            RobotMessage inform = new RobotMessage("ALL", robotname, msg_type, line);
            gvh.comms.addOutgoingMessage(inform);
            lineno = lineno + 1;

        } catch (IOException e) {
        } catch (NoSuchElementException e) {
            lineno = lineno - 1;
        } catch (IllegalArgumentException e) {
            lineno = 0;
        }
    }

}




