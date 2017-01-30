package testmain;

import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.objects.Common;

import java.util.HashMap;

/**
 * Created by SC on 11/14/16.
 */
public class TestMain {
        // Row 0 = names
        // Row 1 = MACs
        // Row 2 = IPs
        private static String[][] participants;
        private static int numRobots;
        private static BotInfoSelector[] botInfo;
        private static int selectedRobot = 0;
        volatile private static GlobalVarHolder gvh;
        private static LogicThread appToRun = null;

        public static void main(String args[]) {
            // Load the participants
            //participants = IdentityLoader.loadIdentities(IDENTITY_FILE_URL);
            // Put number of robots being used here
            numRobots = 3;
            botInfo = new BotInfoSelector[numRobots];
            // add color, robot type, and device type for each robot here
            botInfo[0] = new BotInfoSelector("red", Common.ARDRONE2, Common.HTCONEM7);
            botInfo[1] = new BotInfoSelector("green", Common.ARDRONE2, Common.HTCONEM7);
            //botInfo[1] = new BotInfoSelector("green", Common.IROBOT, Common.MOTOE);
            botInfo[2] = new BotInfoSelector("blue", Common.ARDRONE2, Common.NEXUS7);
            // botInfo[3] = new BotInfoSelector("white", Common.IROBOT, Common.NEXUS7);

            participants = new String[3][numRobots];
            for (int i = 0; i < numRobots; i++) {
                participants[0][i] = botInfo[i].name;
                participants[1][i] = botInfo[i].bluetooth;
                participants[2][i] = botInfo[i].ip;
            }
            //Notice: the hardware related info is store in model when using the ARDrone2


//            init.create();
            if(participants == null) {
                System.err.println("Error loading identity file!");
                selectedRobot = 0;
            }
            HashMap<String, String> hm_participants = new HashMap<String, String>();
            for(int i = 0; i < participants[0].length; i++) {
                hm_participants.put(participants[0][i], participants[2][i]);
            }
            gvh = new RealGlobalVarHolder(participants[0][selectedRobot], hm_participants, botInfo[selectedRobot].type, participants[1][selectedRobot]);
            Initiator init = new Initiator(gvh, participants, botInfo, selectedRobot);
//            create finished
            init.connect();
            appToRun = new FollowApp(gvh);
            init.createAppInstance(appToRun);
            init.launch(4, 1);
        }

}
