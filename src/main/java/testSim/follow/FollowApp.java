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
import edu.illinois.mitra.cyphyhouse.objects.*;
import edu.illinois.mitra.cyphyhouse.motion.RRTNode;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

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
    private boolean inMutex0 = false;
    private boolean updatePath = false;

    //reading from ui
    int lineno = 0;
    private static final int DEST_MSG = 23;
    private static final int PATH_MSG = 24;
    private HashSet<RobotMessage> receivedMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> pathMsgs = new HashSet<RobotMessage>();

    //list of destinations
    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
    final Map<String, Task> taskLocations = new HashMap<String, Task>();


    public GlobalVarHolder gvh;
    //motion module declaration
    ItemPosition currentDestination;


    private enum Stage {
        PICK, GO, DONE, WAIT
    }

    public int testindex = 0;
    ObstacleList obs;
    public Stack<ItemPosition> path;
    RRTNode pathnode;
    PositionList pos;


    private Stage stage = Stage.PICK;

    public FollowApp(GlobalVarHolder gvh) {
        super(gvh);
        MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
        settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);
        gvh.comms.addMsgListener(this, DEST_MSG);
        gvh.comms.addMsgListener(this, PATH_MSG);

        obs = gvh.gps.getObspointPositions();
/*
        pos = gvh.gps.get_robot_Positions();
        Iterator it = pos.iterator();
        while (it.hasNext()) {

            ItemPosition ipos = (ItemPosition) it.next();
            if (ipos.getName() == name) {
                continue;
            }
            obs.add(new Obstacles(ipos.x,ipos.y,ipos.z));
        }*/


        String intValue = name.replaceAll("[^0-9]", "");
        robotIndex = Integer.parseInt(intValue);
        dsm = new DSMMultipleAttr(gvh);
        mutex0 = new GroupSetMutex(gvh, 0);
        this.gvh = gvh;
    }

    @Override
    public List<Object> callStarL() {
        dsm.createMW("testindex", 0);

        while (true) {
            //System.out.println(stage + " "+ gvh.plat.reachAvoid.doneFlag+" "+name);
            //System.out.println(stage + " robotindex " + robotIndex);
            switch (stage) {
                case PICK:
                    updatePath = false;
                    if (robotIndex == 0) {
                        updatedests("tasks.txt", DEST_MSG, name, lineno);
                        lineno = lineno + 1;
                    }
                    if (destinations.isEmpty() || robotIndex == 0) {
                        stage = Stage.WAIT;
                        //System.out.println("Going to wait from pick");
                        break;
                    } else {
                        testindex = Integer.parseInt(dsm.get("testindex", "*"));
                        if (testindex >= destinations.size()) {
                            stage = Stage.WAIT;

                            if (updatePath) {

                            } else {
                                RobotMessage pathmsg = new RobotMessage("ALL", name, PATH_MSG, gvh.gps.getMyPosition().toString() + "###mypos");
                                gvh.comms.addOutgoingMessage(pathmsg);
                                updatePath = false;

                            }
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
                                //System.out.println("robot "+ robotIndex + " has testindex "+testindex);
                                currentDestination = getDestination(destinations, testindex);
                                testindex = testindex + 1;
                                ItemPosition mypos = gvh.gps.getMyPosition();

                                //pathnode = new RRTNode(mypos.x,mypos.y);
                                //Obstacles o = new Obstacles(1,1,1);
                                //Vector<Obstacles> v = new Vector<>();
                                SimplePP newp = new SimplePP(mypos, currentDestination, 4);
                                path = newp.getPath();
                                //System.out.println("my position is :" + mypos);
                                //System.out.println("going to :" + currentDestination);
                                System.out.println("  ");
                                System.out.println("path is: ");
                                System.out.println("peek is " + path.peek());
                                for (int i = path.size() - 1; i >= 0; i--) {
                                    System.out.println(path.get(i));
                                }
                                System.out.println("end of path");
                                currentDestination = path.peek();
                                //System.out.println(" ");
                                //RobotMessage pathmsg = new RobotMessage("ALL", name, PATH_MSG, path.toString()+"###path");
                                //gvh.comms.addOutgoingMessage(pathmsg);
                                //System.out.println(pathmsg);
                                //PEEK, GOTO , POP. (repeat untill null) .
                                //System.out.println(mkObstacles(path).obstacle);
                                dsm.put("testindex", "*", testindex);
                                sleep(800);
                                inMutex0 = true;
                                //exit conditions
                                wait0 = false;
                                //mutex0.exit(0);

                            } else {

                                if (updatePath) {

                                } else {
                                    RobotMessage pathmsg = new RobotMessage("ALL", name, PATH_MSG, gvh.gps.getMyPosition().toString() + "###mypos");
                                    gvh.comms.addOutgoingMessage(pathmsg);
                                    updatePath = false;

                                }
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
                        //System.out.println(obs);
                        //System.out.println("posL\n" + pos);
                        //gvh.plat.reachAvoid.doReachAvoid(gvh.gps.getMyPosition(), currentDestination,obs);
                        System.out.println("calling goto for point: " + currentDestination);

                        gvh.plat.moat.goTo(currentDestination);
                        stage = Stage.GO;
                        /*if (currentDestination.getZ() == 0) {
                            stage = Stage.WAIT;
                        } else {
                            stage = Stage.GO;
                        }*/
                    }

                    break;
                case GO:
                    System.out.println("IN GO STAGE " + name);
                    if (!gvh.plat.moat.inMotion) {
                        if (!gvh.plat.moat.done && currentDestination != null) {
                            stage = Stage.GO;
                        } else {
                            path.pop();
                            if (path.empty()) {
                                System.out.println("GOING BACK TO PICK " + name);
                                stage = Stage.PICK;
                            } else {
                                currentDestination = path.peek();
                                gvh.plat.moat.goTo(currentDestination);
                            }
                            break;
                        }
                    }
                    break;
                case WAIT:
                    //System.out.println("GOING TO PICK FROM WAIT STAGE " + robotIndex);
                    stage = Stage.PICK;
                    break;
            }

            Random ran = new Random();
            sleep(ran.nextInt(200) + 100);
            if (inMutex0) {
                mutex0.exit(0);
                inMutex0 = false;
            }
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

        for (RobotMessage msg : pathMsgs) {
            if (msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
                alreadyReceived = true;
                break;
            }
        }

        int i = receivedMsgs.size();
        int j = pathMsgs.size();


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
            taskLocations.put(p.getName(), new Task(p, i));

        }
/*
        if (m.getMID() == PATH_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            pathMsgs.add(m);
            gvh.log.d(TAG, "received path message from " + m.getFrom());
            String mc = m.getContents().toString().replace("`","").replace(" ",",");
            String type = mc.split("###")[1];
            int sentIndex = Integer.parseInt(m.getFrom().replaceAll("[^0-9]", ""));
            if (type.equalsIgnoreCase("mypos")) {
                if (sentIndex > robotIndex) {

                    System.out.println(obs.ObList.get(sentIndex-1)+" "+sentIndex+" "+robotIndex);
                }
                else{
                    System.out.println(obs.ObList.get(sentIndex)+" "+sentIndex+" "+robotIndex);

                }


            }
            else {

            }

        }*/


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

    private <X, T> T getTask(Map<X, T> map, int index) {
        String key = Integer.toString(index) + "-A";
        return map.get(key);
    }


    private <X, T> void getUnassignedTask(Map<X, T> map) {
        Iterator it = map.values().iterator();
        while (it.hasNext()) {
            System.out.println("here " + it.next().toString());


        }


    }

    private boolean isClose(Stack<ItemPosition> pathstack, Stack<ItemPosition> obstack, double mindist) {
        int i = pathstack.size();
        int k = obstack.size();

        for (int j = 1; j < i; j++) {
            ItemPosition start = pathstack.get(j - 1);
            ItemPosition next = pathstack.get(j);
            if (k == 1) {
                ItemPosition robotPos = obstack.pop();
                double x1 = (double) robotPos.x;
                double y1 = (double) robotPos.y;
                double z1 = (double) robotPos.z;
                double x2 = (double) start.x;
                double y2 = (double) start.y;
                double z2 = (double) start.z;
                double x3 = (double) next.x;
                double y3 = (double) next.y;
                double z3 = (double) next.z;

                double b = Math.sqrt(Math.pow((x2 - x3), 2)
                        + Math.pow((y2 - y3), 2)
                        + Math.pow((z2 - z3), 2));

                double S = Math.sqrt(Math.pow((y2 - y1) * (z3 - z1) - (z2 - z1) * (y3 - y1), 2) +
                        Math.pow((z2 - z1) * (x3 - x1) - (x2 - x1) * (z3 - z1), 2) +
                        Math.pow((x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1), 2)) / 2;

                double distance = 2 * S / b;
                if (distance <= mindist) return true;
                else continue;
            } else {
                for (int p = 1; p < k; p++) {
                    ItemPosition obstart = obstack.get(p - 1);
                    ItemPosition obnext = obstack.get(p);

                    double distance = 0;

                    if (distance <= mindist) return true;
                    else continue;
                }
            }
        }g
        return false;

    }
}
