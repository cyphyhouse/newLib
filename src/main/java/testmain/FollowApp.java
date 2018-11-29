package testmain;


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

import java.io.BufferedReader;
import java.io.FileReader;
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
    private boolean inMutex0 = false;
    private boolean updatePath = false;

    //reading from ui
    int lineno = 0;
    private static final int DEST_MSG = 23;
    private static final int PATH_MSG = 24;
    private static final int ASGN_MSG = 25;
    private static final int MUTEX_REQUEST_MSG = 26;
    private static final int MUTEX_RELEASE_MSG = 27;
    private static final int MUTEX_GRANT_MSG = 28;


    private boolean isLocked = false;
    private boolean takeoff = false;

    private Queue<Integer> requests = new LinkedList();
    private Queue<Integer> requestIDXs = new LinkedList();
    //private PriorityQueue<Integer> requests = new PriorityQueue<Integer>();

    private HashSet<RobotMessage> receivedMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> pathMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> assignedMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> mutexReleaseMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> mutexRequestMsgs = new HashSet<RobotMessage>();
    private HashSet<RobotMessage> mutexGrantMsgs = new HashSet<RobotMessage>();



    //list of destinations
    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
    final Map<String, Task> taskLocations = new HashMap<String, Task>();


    public GlobalVarHolder gvh;
    //motion module declaration
    ItemPosition currentDestination;


    private enum Stage {
        PICK, GO, DONE, WAIT
    }
    private int asgndsize = 0;
    private int asgnIndex;
    private int msgId = 0;
    private boolean hasMutex = false;
    private Vector<Stack<ItemPosition>> obs;
    public Stack<ItemPosition> path;
    private Vector<Integer> assigned;



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
        gvh.comms.addMsgListener(this, MUTEX_GRANT_MSG);
        gvh.comms.addMsgListener(this, MUTEX_RELEASE_MSG);
        gvh.comms.addMsgListener(this, MUTEX_REQUEST_MSG);

        obs = new Vector<>();
        assigned = new Vector<>();
        /*for (int i=0; i<5; i++){
            assigned.add(0);
        }*/

        String intValue = name.replaceAll("[^0-9]", "");
        robotIndex = Integer.parseInt(intValue);
        dsm = new DSMMultipleAttr(gvh);
        this.gvh = gvh;
    }

    @Override
    public List<Object> callStarL() {
        dsm.createMW("testindex", 0);
        ItemPosition[] ipos = new ItemPosition[3];
        ipos[0] = new ItemPosition("quadcopter0",1000,1000,80);
        ipos[1] = new ItemPosition("quadcopter1",1000,1000,0);
        ipos[2] = new ItemPosition("quadcopter2",-20,-20,0);

        for (int i = 0 ; i < 3 ; i ++) {
            Stack<ItemPosition> o = new Stack<ItemPosition>();
            if (i != robotIndex)
                o.push(ipos[i]);
                obs.add(o);
        }
        /*
        pos = gvh.gps.get_robot_Positions();
        Iterator it = pos.iterator();
        while (it.hasNext()) {

            ItemPosition ipos = (ItemPosition) it.next();
            if (ipos.getName().equals(name)) {
                continue;
            }
            Stack<ItemPosition> o = new Stack<ItemPosition>();
            o.push(ipos);
            obs.add(o);
        }*/

        //System.out.println(name + " " +obs.size());


        while (true) {
            //System.out.println("ASSIGNED ARRAY IS: " + assigned);
            /*System.out.println("BEGIN DESTINATIONS ARRAY PRINTOUT:");
            for(int i=0; i<assigned.size(); i++){
                System.out.println(getDestination(destinations, i) + "\n");
            }
            System.out.println("END DESTINATIONS ARRAY PRINTOUT\n");*/

            lineno = 0;
            if (robotIndex == 0) {
                updatedests("tasks.txt", DEST_MSG, name, lineno);
            }

            //System.out.println(stage+" "+name);
            switch (stage) {
                case PICK:
                    if (robotIndex == 0) {
                       break;
                    }
                    if (!takeoff) {
                        ItemPosition mypos = gvh.gps.getMyPosition();
                        if (mypos == null) break;
                        else {
                            ItemPosition takeoffpoint = new ItemPosition("takeoff",mypos.x,mypos.y,mypos.z+100);
                            path = new Stack<ItemPosition>();
                            path.push(takeoffpoint);
                            gvh.plat.moat.goTo(takeoffpoint);
                            takeoff =true;
                            RobotMessage pathmsg = new RobotMessage("ALL", name, PATH_MSG, constPathMsg(path) + "###mypos");
                            gvh.comms.addOutgoingMessage(pathmsg);
                            stage = Stage.GO;
                            //System.out.println(name + " SENT TAKEOFF");
                            break;
                        }
                    }
                    updatePath = false;

                    if (destinations.isEmpty()) {
                        ItemPosition mypos = gvh.gps.getMyPosition();
                        if (mypos == null) {
                            break;
                        }
                        else {
                            //System.out.println(mypos.toString());
                        }
                        RobotMessage pathmsg = new RobotMessage("ALL", name, PATH_MSG, mypos.toString() + "###mypos");
                        gvh.comms.addOutgoingMessage(pathmsg);
                        stage = Stage.WAIT;
                        break;
                    } else {

                        try {
                            if (!wait0) {
                                String mutexreqmsg = String.valueOf(robotIndex)+" "+ String.valueOf(msgId) + " REQUEST";
                                RobotMessage mutexrequestmsg = new RobotMessage("ALL", name, MUTEX_REQUEST_MSG,mutexreqmsg );
                                gvh.comms.addOutgoingMessage(mutexrequestmsg);
                                wait0 = true;
                                Random ran = new Random();
                                break;
                            }
                            if (hasMutex) {
                               // System.out.println(name + " HAS MUTEX");
                                //asgndsize = 5;//assigned.size();
                                //Random r = new Random();
                                //asgnIndex = r.nextInt(asgndsize);
                                boolean foundpath = false;


                                int current_shortest_idx=0;
                                double current_shortest_distance = Double.MAX_VALUE;

                                for (asgnIndex=0; asgnIndex < asgndsize; asgnIndex++) {

                                    boolean skip_land_command = false;

                                    if(assigned.get(asgnIndex) == 0){
                                        currentDestination = getDestination(destinations, asgnIndex);
                                        if(currentDestination.z == 0){
                                            //check if there are any non land commands to do
                                            for(int i=0; i < asgndsize; i++){
                                                if(assigned.get(i) == 0) {
                                                    ItemPosition checkDestination = getDestination(destinations, i);
                                                    if(checkDestination.z != 0){
                                                        skip_land_command = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }


                                    if(!skip_land_command) {
                                        if (assigned.get(asgnIndex) == 0) {
                                            currentDestination = getDestination(destinations, asgnIndex);

                                            if(currentDestination.z == 0 && currentDestination != null){
                                                foundpath = true;
                                                current_shortest_idx = asgnIndex;
                                                break;
                                            }

                                            if (currentDestination != null) {
                                                ItemPosition mypos = gvh.gps.getMyPosition();
                                                SimplePP newp = new SimplePP(mypos, currentDestination, 1);
                                                path = newp.getPath();
                                                sleep(100);
                                                boolean breakpath = false;

                                                for (int i = 0; i < obs.size(); i++) {
                                                    if (isClose(path, obs.get(i), 150)) {
                                                        breakpath = true;
                                                        break;
                                                    } else {
                                                    }
                                                }
                                                if (!breakpath) {
                                                    //Calculate distance and check if it is the shortest
                                                    //If it is, store this points IDX so we can get it again later
                                                    double distance = Math.sqrt(Math.pow(mypos.x-currentDestination.x,2)+Math.pow(mypos.y-currentDestination.y,2)+Math.pow(mypos.z-currentDestination.z,2));
                                                    if(distance < current_shortest_distance){
                                                        current_shortest_distance = distance;
                                                        current_shortest_idx = asgnIndex;
                                                    }
                                                    foundpath = true;
                                                    //break;
                                                }
                                            } else
                                                break;
                                        }
                                    }
                                }

                               // System.out.println("ASSIGN INDEX IS: " + asgnIndex);
                                asgnIndex = current_shortest_idx;

                                //If a path is found, use the stored IDX to get the closest waypoint and set to currentDestination
                                if(foundpath){
                                   // System.out.println("FOUND PATH AND OUT OF FOR LOOP");
                                    currentDestination = getDestination(destinations, current_shortest_idx);
                                    ItemPosition mypos = gvh.gps.getMyPosition();
                                    SimplePP newp = new SimplePP(mypos, currentDestination, 1);
                                    path = newp.getPath();
                                    //System.out.println(currentDestination);
                                   // System.out.println("DONE GETTING DESTINATION");
                                }

                                //System.out.println("FOR LOOP DONE");
                                if (!foundpath) {
                                   // System.out.println("COULD NOT FIND A PATH");
                                    inMutex0 = true;
                                    wait0 = false;
                                    break;
                                }
                               // System.out.println("THE PATH IS: " + path);
                                path.pop();
                                currentDestination = path.peek();
                                RobotMessage asgnmsg = new RobotMessage("ALL", name, ASGN_MSG, String.valueOf(asgnIndex));
                                RobotMessage pathmsg = new RobotMessage("ALL", name, PATH_MSG, constPathMsg(path) + "###path");
                                gvh.comms.addOutgoingMessage(pathmsg);
                                gvh.comms.addOutgoingMessage(asgnmsg);
                                //sleep(800);
                                assigned.set(asgnIndex,1);

                                updatePath = true;
                                inMutex0 = true;
                                wait0 = false;

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

                     //   System.out.println("CALLING GOTO. DESTINATION IS: " + currentDestination);
                        gvh.plat.moat.goTo(currentDestination);
                        stage = Stage.GO;

                    }

                    break;
                case GO:
                    if (gvh.plat.moat.inMotion) {
                            stage = Stage.GO;
                        } else {
                            sleep(800);
                            ItemPosition ip = path.pop();


                            if (path.empty()) {
                                if(!assigned.contains(0))
                                    stage = Stage.PICK;
                                else {
                                    if (ip.z  == 0) {
                                        stage = Stage.DONE;
                                    }
                                    else {
                                        stage = Stage.PICK;
                                    }
                                    //System.out.println(name + "Done a point " + ip);
                                }
                            } else {
                                currentDestination = path.peek();
                                gvh.plat.moat.goTo(currentDestination);
                            }
                            break;
                        }

                    break;
                case WAIT:
                    //System.out.println("in wait");
                    stage = Stage.PICK;
                    break;

                case DONE:
                    ItemPosition mypos = gvh.gps.getMyPosition();
                    RobotMessage pathmsg = new RobotMessage("ALL", name, PATH_MSG, mypos.toString() + "###mypos");
                    gvh.comms.addOutgoingMessage(pathmsg);
                    break;

            }
            Random ran = new Random();
            if (inMutex0) {
              //  System.out.println(name + " RELEASING MUTEX");
                hasMutex = false;
                String releaseMutex = String.valueOf(robotIndex) + " "+ String.valueOf(msgId);
                RobotMessage mutexreleasemsg = new RobotMessage("ALL", name, MUTEX_RELEASE_MSG,releaseMutex);
                gvh.comms.addOutgoingMessage(mutexreleasemsg);
                msgId = msgId+1;
                inMutex0 = false;
            }
            sleep(600);
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

        for (RobotMessage msg : mutexRequestMsgs) {
            if (msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
                alreadyReceived = true;
                break;
            }
        }
        for (RobotMessage msg : mutexReleaseMsgs) {
            if (msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
                alreadyReceived = true;
                break;
            }
        }
        for (RobotMessage msg : mutexGrantMsgs) {
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
        int l = mutexRequestMsgs.size();
        int o = mutexReleaseMsgs.size();
        int n = mutexGrantMsgs.size();


        if (m.getMID() == MUTEX_REQUEST_MSG && robotIndex == 0 && !alreadyReceived) {
            mutexRequestMsgs.add(m);
            gvh.log.d(TAG, "received request message from " + m.getFrom());
            String requestmsg = m.getContents().toString().replace("`","");
            int requestid = Integer.parseInt(requestmsg.split(" ")[0]);
            int msgid = Integer.parseInt(requestmsg.split(" ")[1]);

            requests.add(requestid);
            requestIDXs.add(msgid);

            if (!isLocked) {
                String grantmsgstr = String.valueOf(requestid)+" "+String.valueOf(msgid);
                RobotMessage grantmsg = new RobotMessage("ALL", name, MUTEX_GRANT_MSG, grantmsgstr  );
                gvh.comms.addOutgoingMessage(grantmsg);
                isLocked = true;
            }

        }

        if (m.getMID() == MUTEX_RELEASE_MSG){
            //System.out.println(name + " got release msg " + alreadyReceived);
        }
        if (m.getMID() == MUTEX_RELEASE_MSG && robotIndex == 0 && !alreadyReceived) {
            mutexReleaseMsgs.add(m);
            gvh.log.d(TAG, "received release message from " + m.getFrom());
            String releasemsg = m.getContents().toString().replace("`","");

            //System.out.println(name + " " + releasemsg);
            //System.out.println("the queue is: " + requests);

            int releaseid = Integer.parseInt(releasemsg.split(" ")[0]);
            int msgid = Integer.parseInt(releasemsg.split(" ")[1]);

            if (releaseid == requests.peek()) {
                requests.poll();
                requestIDXs.poll();
                if (requests.size() == 0) {
                    isLocked = false;
                }
                else {
                    int requestid = requests.peek();
                    msgid = requestIDXs.peek();
                    String grantmsgstr = String.valueOf(requestid)+" "+String.valueOf(msgid);
                    RobotMessage grantmsg = new RobotMessage("ALL", name, MUTEX_GRANT_MSG, grantmsgstr  );
                    //System.out.println(grantmsgstr);
                    gvh.comms.addOutgoingMessage(grantmsg);
                    isLocked = true;

                }
            }

        }


        if (m.getMID() == MUTEX_GRANT_MSG && !alreadyReceived) {
            mutexGrantMsgs.add(m);
            gvh.log.d(TAG, "received grant message from " + m.getFrom());
            String grantmsg = m.getContents().toString().replace("`","");
            //System.out.println(name + " " + grantmsg);
            int grantrobotid = Integer.parseInt(grantmsg.split(" ")[0]);
            int grantmsgid = Integer.parseInt(grantmsg.split(" ")[1]);

            //System.out.println(name + " " + grantmsg + " " + msgId);
            if (grantrobotid == robotIndex && grantmsgid == msgId) {
                hasMutex = true;
            }
        }




        if (m.getMID() == ASGN_MSG && !alreadyReceived && !(robotIndex == 0)) {
            assignedMsgs.add(m);
            gvh.log.d(TAG, "received assignment message from " + m.getFrom());

            //System.out.println(name + " beginning asgnmsg set");
            String asgnmsg = m.getContents().toString().replace("`", "");
            int vectorid = Integer.parseInt(asgnmsg);
            //System.out.println("reached here on "+ name);
            //System.out.println("assigned size " + assigned.size());
            //System.out.println("assigned size " + assigned.size());
            assigned.set(vectorid, 1);
            //System.out.println(name + " done asgnmsg set");
        }

        if (m.getMID() == DEST_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            receivedMsgs.add(m);
            gvh.log.d(TAG, "received destination message from " + m.getFrom());

            String iposmsg = m.getContents().toString();
            ItemPosition p = msgtoipos(iposmsg, i, 100);
            destinations.put(p.getName(), p);
            taskLocations.put(p.getName(), new Task(p, i));
            assigned.add(0);
            asgndsize++;
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
        int idx = (int) (Float.parseFloat(parts[3]));
        String name = Integer.toString(idx) + "-A";
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

        try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            int numlines = 0;
            while (reader.readLine() != null) numlines++;
            reader.close();

            for (lineno = 0; lineno < numlines; lineno++) {
                try (Stream<String> lines = Files.lines(Paths.get(filename))) {
                    String line = lines.skip(lineno).findFirst().get();
                    line = line + " " + lineno;
                    RobotMessage inform = new RobotMessage("ALL", robotname, msgtype, line);
                    gvh.comms.addOutgoingMessage(inform);
                } catch (IOException e) {
                } catch (NoSuchElementException e) {
                } catch (IllegalArgumentException e) {
                }
            }
        }
        catch(Exception e){
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

        double SMALL_NUM = 0.000000001;

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
        double u_x = sc * u.x;
        double u_y = sc * u.y;
        double u_z = sc * u.z;

        // tc * v
        double v_x = tc * v.x;
        double v_y = tc * v.y;
        double v_z = tc * v.z;

        double Dp_x = w.x + u_x - v_x;
        double Dp_y = w.y + u_y - v_y;
        double Dp_z = w.z + u_z - v_z;
        //Point3d Dp = new Point3d(w.x + u.x - v.x, w.y + u.y - v.y, w.z + u.z - v.z);

        double distance = Math.sqrt(Dp_x * Dp_x + Dp_y * Dp_y + Dp_z * Dp_z);

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
                //System.out.println(name + " path point distance " + distance);
                if (distance <= mindist) {
                    //System.out.println("DIST IS: " + distance);
                    return true;
                }
            } else {
                for (int p = 1; p < k; p++) {
                    //System.out.println("checking for cross paths");
                    ItemPosition start_stack = obstack.get(p - 1);
                    ItemPosition next_stack = obstack.get(p);
                    int distance = closestDist(start, next, start_stack, next_stack);
                    //System.out.println(name + " path stack distance " + distance);
                    if (distance <= mindist) {
                        //System.out.println("DIST IS: " + distance);
                        return true;
                    }
                }
            }
        }
        return false;

    }
}
