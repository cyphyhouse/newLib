package testmain;

import edu.illinois.mitra.cyphyhouse.comms.RobotMessage;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.interfaces.MessageListener;
import edu.illinois.mitra.cyphyhouse.objects.Common;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by SC on 11/14/16.
 */
public class Initiator implements MessageListener{
    private static final String TAG = "Initiator";

    private static final boolean ENABLE_TRACING = false;

    private GlobalVarHolder gvh = null;
    public boolean launched = false;

    // SharedPreferences variables
    private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
    private int selectedRobot = 0;

    // Logic thread executor
    private ExecutorService executor = Executors.newFixedThreadPool(1);
    private Future<List<Object>> results;
    private LogicThread runThread;

    // Row 0 = names
    // Row 1 = MACs
    // Row 2 = IPs
    private String[][] participants;
    private int numRobots;
    private BotInfoSelector[] botInfo;
    private int i;

    public Initiator(GlobalVarHolder gvh, String[][] participants, BotInfoSelector[] botInfo, int selectedRobot){
        this.gvh = gvh;
        this.participants = participants;
        this.botInfo = botInfo;
        this.selectedRobot = selectedRobot;
        this.numRobots = botInfo.length;
    }

    public void create(){
        if(participants == null) {
            System.err.println("Error loading identity file!");
            selectedRobot = 0;
        }
        HashMap<String, String> hm_participants = new HashMap<String, String>();
        for(int i = 0; i < participants[0].length; i++) {
            hm_participants.put(participants[0][i], participants[2][i]);
        }
        this.gvh = new RealGlobalVarHolder(participants[0][selectedRobot], hm_participants, botInfo[selectedRobot].type, participants[1][selectedRobot]);
    }

    public void connect(){
        gvh.log.d(TAG, gvh.id.getName());

        // Begin persistent background threads
        gvh.comms.startComms();
        gvh.gps.startGps();

        // Register this as a listener
        gvh.comms.addMsgListener(this, Common.MSG_ACTIVITYLAUNCH, Common.MSG_ACTIVITYABORT);
    }

    public void disconnect(){
        gvh.log.i(TAG, "Disconnecting and stopping all background threads");

        // Shut down the logic thread if it was running
        if(launched) {
            runThread.cancel();
            executor.shutdownNow();
        }
        launched = false;

        // Shut down persistent threads
        gvh.comms.stopComms();
        gvh.gps.stopGps();
        gvh.plat.moat.cancel();
    }

    public void createAppInstance(LogicThread appToRun){
        runThread = appToRun;
    }

    public void launch(int numWaypoints, int runNum) {
        if(!launched) {
            if(gvh.gps.getWaypointPositions().getNumPositions() == numWaypoints) {
                if(ENABLE_TRACING)
                    gvh.trace.traceStart(runNum);
                launched = true;

                gvh.trace.traceSync("APPLICATION LAUNCH", gvh.time());

                //RobotMessage informLaunch = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYLAUNCH, new MessageContents(Common.intsToStrings(numWaypoints, runNum)));
                //gvh.comms.addOutgoingMessage(informLaunch);
                results = executor.submit(runThread);
            } else {
                System.err.println("Should have " + numWaypoints + " waypoints, but I have " + gvh.gps.getWaypointPositions().getNumPositions());
            }
        }
    }

    public void abort(){
        runThread.cancel();
        results.cancel(true);
        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();
        createAppInstance(runThread);//TODO, not sure if should use this internal variable again
    }

    @Override
    public void messageReceived(RobotMessage m) {
        switch(m.getMID()) {
            case Common.MSG_ACTIVITYLAUNCH:
                gvh.log.i(TAG, "MSG_ACTIVITYLAUNCH");
                break;
            case Common.MSG_ACTIVITYABORT:
                gvh.log.i(TAG, "MSG_ACTIVITYABORT");
                break;
        }
    }
}
