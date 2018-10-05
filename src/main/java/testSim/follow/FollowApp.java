package testSim.follow;


/**
 * Created by VerivitalLab on 2/26/2016.
 * This app was created to test the drones. The bots will each go to an assigned waypoint.
 * Once both bots have arrived at their respective waypoints, they will then go to the next waypoints.
 */

import edu.illinois.mitra.cyphyhouse.comms.RobotMessage;
import edu.illinois.mitra.cyphyhouse.functions.DSMMultipleAttr;
import edu.illinois.mitra.cyphyhouse.functions.GroupSetMutex;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.DSM;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.interfaces.MutualExclusion;
import edu.illinois.mitra.cyphyhouse.motion.MotionParameters;
import edu.illinois.mitra.cyphyhouse.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;


public class FollowApp extends LogicThread {
    private static final String TAG = "Follow App";

    private int robotIndex;

    //Dsm initializations
    private DSM dsm;
    private boolean wait0 = false;
    private MutualExclusion mutex0;

    //reading from ui
    int lineno = 0;
    private static final int DEST_MSG = 23;
    private HashSet<RobotMessage> receivedMsgs = new HashSet<RobotMessage>();

    //list of destinations
    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();


    //motion module declaration
    ItemPosition currentDestination;


    private enum Stage {
        PICK, GO, DONE, WAIT
    }

    public int testindex = 0;
    ObstacleList obs;

    private Stage stage = Stage.PICK;

    public FollowApp(GlobalVarHolder gvh) {
        super(gvh);
        MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
        settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);
        gvh.comms.addMsgListener(this, DEST_MSG);
        obs = gvh.gps.getObspointPositions();

        // bot names must be bot0, bot1, ... botn for this to work
        String intValue = name.replaceAll("[^0-9]", "");
        robotIndex = Integer.parseInt(intValue);
        dsm = new DSMMultipleAttr(gvh);
        mutex0 = new GroupSetMutex(gvh, 0);

    }

    @Override
    public List<Object> callStarL() {
        dsm.createMW("testindex", 0);

        while (true) {
            switch (stage) {
                case PICK:
                    if (robotIndex == 0) {
                        updatedests("tasks.txt", DEST_MSG, name, lineno);
                        lineno = lineno + 1;
                    }
                    if (destinations.isEmpty() || robotIndex == 0) {
                        stage = Stage.WAIT;
                    } else {
                        testindex = Integer.parseInt(dsm.get("testindex", "*"));
                        if (testindex >= destinations.size()) {
                            stage = Stage.WAIT;
                            break;
                        }
                        try {
                            if (!wait0) {
                                mutex0.requestEntry(0);
                                wait0 = true;
                                break;
                            }
                            if (mutex0.clearToEnter(0)) {
                                testindex = Integer.parseInt(dsm.get("testindex", "*"));
                                System.out.println("robot "+ robotIndex + " has testindex "+testindex);
                                currentDestination = getDestination(destinations, testindex);
                                testindex = testindex + 1;
                                dsm.put("testindex", "*", testindex);

                                //exit conditions
                                wait0 = false;
                                mutex0.exit(0);

                            } else {
                                break;
                            }
                        } catch (NullPointerException e) {
                            stage = Stage.WAIT;
                            //lineno = lineno - 1;
                            break;
                        }
                        if (currentDestination == null) {
                            stage = Stage.WAIT;
                            break;
                        }

                        gvh.plat.moat.goTo(currentDestination);
                        if (currentDestination.getZ() == 0) {
                            stage = Stage.WAIT;
                        } else {
                            stage = Stage.GO;
                        }
                    }
                    break;
                case GO:
                    if (!gvh.plat.moat.inMotion) {
                        if (!gvh.plat.moat.done && currentDestination != null) {
                            stage = Stage.WAIT;
                        } else {
                            stage = Stage.PICK;
                            break;
                        }


                    }
                    break;
                case WAIT:
                    if (!gvh.plat.moat.done && robotIndex != 0) {
                        stage = Stage.PICK;
                    }
                    if (robotIndex == 0)
                        stage = Stage.PICK;
                    stage = Stage.GO;
                    break;
            }
            sleep(300);
        }
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
        if (m.getMID() == DEST_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            receivedMsgs.add(m);
            gvh.log.d(TAG, "received destination message from " + m.getFrom());

            String dest = m.getContents().toString();
            dest = dest.replace(" ", ",").replace("`", "");
            String[] parts = dest.split(",");
            int x = (int) (Float.parseFloat(parts[0]) * 1000);
            int y = (int) (Float.parseFloat(parts[1]) * 1000);
            int z = (int) (Float.parseFloat(parts[2]) * 1000);
            String name = Integer.toString(i) + "-A";
            ItemPosition p = new ItemPosition(name, x, y, z);
            destinations.put(p.getName(), p);

        }
    }

    private void updatedests(String filename, int msgtype, String robotname, int lineno) {
        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            String line = lines.skip(lineno).findFirst().get();
            RobotMessage inform = new RobotMessage("ALL", robotname, msgtype, line);
            gvh.comms.addOutgoingMessage(inform);
        } catch (IOException e) {
        } catch (NoSuchElementException e) {
        } catch (IllegalArgumentException e) {
        }
    }

    @SuppressWarnings("unchecked")
    private <X, T> T getDestination(Map<X, T> map, int index) {
        String key = Integer.toString(index) + "-A";
        return map.get(key);
    }
}
