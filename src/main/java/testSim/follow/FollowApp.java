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
import edu.illinois.mitra.cyphyhouse.objects.Point3d;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;

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
    private static final int ASGN_MSG = 25;

    private HashSet<RobotMessage> receivedMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> pathMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> assignedMsgs = new HashSet<RobotMessage>();


    //list of destinations
    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
    final Map<String, Task> taskLocations = new HashMap<String, Task>();


    public GlobalVarHolder gvh;
    //motion module declaration
    ItemPosition currentDestination;


    private enum Stage {
        PICK, GO, DONE, WAIT
    }
    int asgndsize;
    int asgnIndex;
    public int testindex = 0;
    Vector<Stack<ItemPosition>> obs;
    public Stack<ItemPosition> path;
    public Vector<Integer> assigned;

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
        gvh.comms.addMsgListener(this, ASGN_MSG);

        obs = new Vector<>();
        assigned = new Vector<>();
        for (int i=0; i<12; i++){
            assigned.add(0);
        }

        String intValue = name.replaceAll("[^0-9]", "");
        robotIndex = Integer.parseInt(intValue);
        dsm = new DSMMultipleAttr(gvh);
        mutex0 = new GroupSetMutex(gvh, 0);
        this.gvh = gvh;
    }

    @Override
    public List<Object> callStarL() {
        dsm.createMW("testindex", 0);

        pos = gvh.gps.get_robot_Positions();
        Iterator it = pos.iterator();
        while (it.hasNext()) {

            ItemPosition ipos = (ItemPosition) it.next();
            if (ipos.getName() == name) {
                continue;
            }
            Stack<ItemPosition> o = new Stack<ItemPosition>();
            o.push(ipos);
            obs.add(o);
        }

        //System.out.println(name + " " +obs.size());


        while (true) {
            //System.out.println(name + "'s assigned list is: " + assigned);
            //System.out.println(stage + " "+ gvh.plat.reachAvoid.doneFlag+" "+name);
            //System.out.println(stage + " robotindex " + robotIndex);
            //System.out.println("assigned list is " + assigned.toString() + " for " + name);
            switch (stage) {
                case PICK:
                    updatePath = false;
                    if (robotIndex == 0) {
                        updatedests("tasks.txt", DEST_MSG, name, lineno);
                        lineno = lineno + 1;
                    }
                    if (destinations.isEmpty() || robotIndex == 0) {
                        RobotMessage pathmsg = new RobotMessage("ALL", name, PATH_MSG, gvh.gps.getMyPosition().toString() + "###mypos");
                        gvh.comms.addOutgoingMessage(pathmsg);
                        stage = Stage.WAIT;
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
                                Random ran = new Random();
                                sleep(ran.nextInt(200)+100);
                                break;
                            }
                            if (mutex0.clearToEnter(0)) {
                                //testindex = Integer.parseInt(dsm.get("testindex", "*"));
                                //System.out.println("robot "+ robotIndex + " has testindex "+testindex);
                                //currentDestination = getDestination(destinations, testindex);
                                System.out.println(name + " in mutex");
                                asgndsize = 12;//assigned.size();
                                asgnIndex = 0;
                                Random r = new Random();
                               // asgnIndex = r.nextInt(asgndsize);
                                boolean foundpath = false;
                                for (asgnIndex = r.nextInt(asgndsize); asgnIndex < asgndsize; asgnIndex++) {
                                    System.out.println(name + " asgnIDX is " + asgnIndex + " value is " + assigned.get(asgnIndex));
                                    if (assigned.get(asgnIndex) == 0) {
                                        System.out.println(name + " gotten index: " + asgnIndex);
                                        currentDestination = getDestination(destinations, asgnIndex);
                                        ItemPosition mypos = gvh.gps.getMyPosition();
                                        SimplePP newp = new SimplePP(mypos, currentDestination, 4);
                                        path = newp.getPath();
                                        sleep(5000);
                                        boolean breakpath = false;
                                        for (int i = 0; i < obs.size(); i++) {
                                            //System.out.println(name+ " i is " + i);
                                            if (isClose(path, obs.get(i), 1200)) {
                                                //mutex0.exit(0);
                                                //wait0 = false;
                                                //System.out.println("DISTANCE TOO CLOSE BREAKING "+name);
                                                breakpath = true;
                                            }
                                            else{
                                            }
                                        }
                                        System.out.println(breakpath);
                                        if (breakpath) {breakpath = false;}
                                        else {
                                            foundpath = true;
                                            break;
                                        }
                                    }
                                }
                                if (!foundpath) {
                                    break;
                                }
                                else{
                                    foundpath = false;
                                }
                                //currentDestination = getDestination(destinations,asgnIndex);
                                //testindex = testindex + 1;
                                //ItemPosition mypos = gvh.gps.getMyPosition();

                                //pathnode = new RRTNode(mypos.x,mypos.y);
                                //Obstacles o = new Obstacles(1,1,1);
                                //Vector<Obstacles> v = new Vector<>();
                                //SimplePP newp = new SimplePP(mypos, currentDestination, 4);
                                //path = newp.getPath();
                                //boolean breakpath = false;
                                /*for (int i = 0; i < obs.size(); i++) {
                                    //System.out.println(name+ " i is " + i);
                                    if (isClose(path, obs.get(i), 1200)) {
                                        mutex0.exit(0);
                                        wait0 = false;
                                        //System.out.println("DISTANCE TOO CLOSE BREAKING "+name);
                                        breakpath = true;
                                        continue;
                                    }
                                }
                                if (breakpath) {
                                    stage = Stage.PICK;
                                    break;
                                }*/
                                //System.out.println(name + " going to " + path.get(0).toString())
                                // ;
                                currentDestination = path.peek();
                                RobotMessage asgnmsg = new RobotMessage("ALL", name, ASGN_MSG, String.valueOf(asgnIndex));
                                RobotMessage pathmsg = new RobotMessage("ALL", name, PATH_MSG, constPathMsg(path) + "###path");
                                gvh.comms.addOutgoingMessage(pathmsg);
                                gvh.comms.addOutgoingMessage(asgnmsg);
                                assigned.set(asgnIndex,1);

                                updatePath = true;
                                //System.out.println(pathmsg);
                                //PEEK, GOTO , POP. (repeat untill null) .
                                //System.out.println(mkObstacles(path).obstacle);
                                //dsm.put("testindex", "*", testindex);
                                Random ran = new Random();
                                sleep(ran.nextInt(300)+100);
                                inMutex0 = true;
                                //exit conditions
                                wait0 = false;
                                //mutex0.exit(0);

                            } else {
                                Random ran = new Random();
                                sleep(ran.nextInt(800)+100);
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
                        //System.out.println("calling goto for point: " + currentDestination);

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
                    //System.out.println("IN GO STAGE " + name);
                    if (!gvh.plat.moat.inMotion) {
                        if (!gvh.plat.moat.done && currentDestination != null) {
                            stage = Stage.GO;
                        } else {
                            path.pop();
                            if (path.empty()) {
                                //System.out.println("GOING BACK TO PICK " + name);
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
            sleep(ran.nextInt(500)+100);
            if (inMutex0) {
                mutex0.exit(0);
                sleep(ran.nextInt(800)+100);
                inMutex0 = false;
            }
        }
    }

    @Override
    protected void receive(RobotMessage m) {
        boolean alreadyReceived = false;
        for (RobotMessage msg : assignedMsgs) {
            if (msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
                alreadyReceived = true;
                break;
            }
        }

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
        int k = assignedMsgs.size();


        if (m.getMID() == ASGN_MSG && !alreadyReceived && !(robotIndex == 0)) {
            assignedMsgs.add(m);
            gvh.log.d(TAG, "received assignment message from " + m.getFrom());

            System.out.println(name + " beginning asgnmsg set");
            String asgnmsg = m.getContents().toString().replace("`", "");
            int vectorid = Integer.parseInt(asgnmsg);
            //System.out.println("reached here on "+ name);
            System.out.println("assigned size " + assigned.size());
            //System.out.println("assigned size " + assigned.size());
            assigned.set(vectorid, 1);
            System.out.println(name + " done asgnmsg set");
        }

        if (m.getMID() == DEST_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            receivedMsgs.add(m);
            gvh.log.d(TAG, "received destination message from " + m.getFrom());

            String iposmsg = m.getContents().toString();
            ItemPosition p = msgtoipos(iposmsg, i, 1000);
            destinations.put(p.getName(), p);
            taskLocations.put(p.getName(), new Task(p, i));
            //assigned.add(assigned.size(), 0);

        }

        if (m.getMID() == PATH_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            pathMsgs.add(m);
            gvh.log.d(TAG, "received path message from " + m.getFrom());
            String mc = m.getContents().toString().replace("`", "");
            String type = mc.split("###")[1];
            String contents = mc.split("###")[0];
            int sentIndex = Integer.parseInt(m.getFrom().replaceAll("[^0-9]", ""));
            if (type.equalsIgnoreCase("mypos")) {
                Stack<ItemPosition> path = msgtoiposstack(contents, j, 1);
                //System.out.println(path+" "+name);
                if (sentIndex > robotIndex) {

                    obs.set(sentIndex - 1, path);

                    //System.out.println((sentIndex-1)+" "+sentIndex+" "+robotIndex+ " \n");

                    //System.out.println(obs.get(sentIndex-1)+" "+sentIndex+" "+robotIndex);
                } else {
                    obs.set(sentIndex, path);

                    //System.out.println((sentIndex)+" "+sentIndex+" "+robotIndex+ " \n");

                    //System.out.println(obs.get(sentIndex)+" "+sentIndex+" "+robotIndex);

                }


            } else {
                Stack<ItemPosition> path = msgtopathstack(contents, j, 1);
                if (sentIndex > robotIndex) {
                    obs.set(sentIndex - 1, path);

                    //System.out.println((sentIndex-1)+" "+sentIndex+" "+robotIndex+ " \n");

                    //System.out.println(obs.get(sentIndex-1)+" "+sentIndex+" "+robotIndex);
                } else {
                    obs.set(sentIndex, path);

                    //System.out.println((sentIndex)+" "+sentIndex+" "+robotIndex+ " \n");

                    //System.out.println(obs.get(sentIndex)+" "+sentIndex+" "+robotIndex);

                }
                //String[] pathelements = contents.split("@@@");
            }

        }


    }


    private ItemPosition msgtoipos(String iposmsg, int i, int scale) {
        iposmsg = iposmsg.replace(" ", ",").replace("`", "");
        String[] parts = iposmsg.split(",");
        int x = (int) (Float.parseFloat(parts[0]) * scale);
        int y = (int) (Float.parseFloat(parts[1]) * scale);
        int z = (int) (Float.parseFloat(parts[2]) * scale);
        String name = Integer.toString(i) + "-A";
        ItemPosition p = new ItemPosition(name, x, y, z);
        return p;
    }

    private Stack<ItemPosition> msgtopathstack(String pathmsg, int j, int scale) {
        pathmsg = pathmsg.replace(".", ",").replace(" ", "");
        String[] pathpoints = pathmsg.split("@@@");
        Stack<ItemPosition> path = new Stack<ItemPosition>();
        int i = pathpoints.length;
        for (int k = 0; k < i; k++) {
            pathpoints[k] = pathpoints[k].split(":")[1];
            pathpoints[k] = pathpoints[k].split(";")[0];
            String[] parts = pathpoints[k].split(",");
            int x = (int) (Float.parseFloat(parts[0]) * scale);
            int y = (int) (Float.parseFloat(parts[1]) * scale);
            int z = (int) (Float.parseFloat(parts[2]) * scale);
            String name = Integer.toString(j) + "-A";
            ItemPosition p = new ItemPosition(name, x, y, z);
            path.push(p);
        }
        return path;
    }

    private Stack<ItemPosition> msgtoiposstack(String iposmsg, int j, int scale) {
        iposmsg = iposmsg.replace(".", ",").replace(" ", "");
        Stack<ItemPosition> path = new Stack<ItemPosition>();

        iposmsg = iposmsg.split(":")[1];
        iposmsg = iposmsg.split(";")[0];
        String[] parts = iposmsg.split(",");
        int x = (int) (Float.parseFloat(parts[0]) * scale);
        int y = (int) (Float.parseFloat(parts[1]) * scale);
        int z = (int) (Float.parseFloat(parts[2]) * scale);
        String name = Integer.toString(j) + "-A";
        ItemPosition p = new ItemPosition(name, x, y, z);
        path.push(p);

        return path;
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
            //System.out.println("here " + it.next().toString());


        }


    }

    private boolean isclosetobots(Stack<ItemPosition> pathstack, Vector<Stack<ItemPosition>> robotStack, double mindist) {
        int i = robotStack.size();
        for (int j = 0; j < i; j++) {
            if (isClose(pathstack, robotStack.get(j), mindist)) return true;
            else continue;
        }
        return false;
    }

    private String constPathMsg(Stack<ItemPosition> path) {
        Iterator pathit = path.iterator();
        String s = "";
        while (pathit.hasNext()) {
            s += pathit.next();
            s += "@@@";
        }
        return s;

    }

    private int closestDist(ItemPosition S1, ItemPosition E1, ItemPosition S2, ItemPosition E2) {
        Point3d u = new Point3d(S1.x - E1.x, S1.y - E1.y, S1.z - E1.z);
        Point3d v = new Point3d(S2.x - E2.x, S2.y - E2.y, S2.z - E2.z);
        Point3d w = new Point3d(E1.x - E2.x, E1.y - E2.y, E1.z - E2.z);

        double a = dot(u, u);
        double b = dot(u, v);
        double c = dot(v, v);
        double d = dot(u, w);
        double e = dot(v, w);

        double D = a * c - b * b;
        double sc;
        double sN;
        double sD = D;
        double tc;
        double tN;
        double tD = D;

        double SMALL_NUM = 0.000001;

        if (D < SMALL_NUM) {
            sN = 0.0;
            sD = 1.0;
            tN = e;
            tD = c;
        } else {
            sN = (b * e - c * d);
            tN = (a * e - b * d);
            if (sN < 0.0) {
                sN = 0.0;
                tN = e;
                tD = c;
            } else if (sN > sD) {
                sN = sD;
                tN = e + b;
                tD = c;
            }
        }

        if (tN < 0.0) {
            tN = 0.0;
            if (-d < 0.0)
                sN = 0.0;
            else if (-d > a)
                sN = sD;
            else {
                sN = -d;
                sD = a;
            }
        } else if (tN > tD) {
            tN = tD;
            if ((-d + b) < 0.0)
                sN = 0;
            else if ((-d + b) > a)
                sN = sD;
            else {
                sN = (-d + b);
                sD = a;
            }
        }

        if (Math.abs(sN) < SMALL_NUM)
            sc = 0.0;
        else
            sc = sN / sD;

        if (Math.abs(tN) < SMALL_NUM)
            tc = 0.0;
        else
            tc = tN / tD;


        // sc * u
        u.x = (int) sc * u.x;
        u.y = (int) sc * u.y;
        u.z = (int) sc * u.z;

        // tc * v
        v.x = (int) tc * v.x;
        v.y = (int) tc * v.y;
        v.z = (int) tc * v.z;

        Point3d Dp = new Point3d(w.x + u.x - v.x, w.y + u.y - v.y, w.z + u.z - v.z);

        double distance = Math.sqrt(Dp.x * Dp.x + Dp.y * Dp.y + Dp.z * Dp.z);

        return (int) distance;


    }

    private float dot(Point3d a, Point3d b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    private boolean isClose(Stack<ItemPosition> pathstack, Stack<ItemPosition> obstack, double mindist) {
        int i = pathstack.size();
        int k = obstack.size();
        //System.out.println("obstack size is " + k);

        for (int j = 1; j < i; j++) {
            ItemPosition start = pathstack.get(j - 1);
            ItemPosition next = pathstack.get(j);
            if (k == 1) {
                /*System.out.println(" %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% ");
                System.out.println(" %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% ");
                System.out.println(pathstack);
                System.out.println(next);
                System.out.println(start);
                System.out.println(obstack.peek());*/

                int distance = closestDist(start, next, obstack.peek(), obstack.peek());
                /*System.out.println(distance);
                System.out.println(" %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% ");
                System.out.println(" %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% ");*/
                //System.out.println(distance);
                if (distance <= mindist)
                    return true;
            } else {
                for (int p = 1; p < k; p++) {
                    //System.out.println("checking for cross paths");
                    ItemPosition start_stack = obstack.get(p - 1);
                    ItemPosition next_stack = obstack.get(p);
                    int distance = closestDist(start, next, start_stack, next_stack);
                    //System.out.println(distance);
                    if (distance <= mindist)
                        return true;
                }
            }
        }
        return false;

    }
}
