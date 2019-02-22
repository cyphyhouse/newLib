package testmain;

import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;

import java.io.BufferedReader;
import java.io.FileReader;
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
        private static int selectedRobot;
        volatile private static GlobalVarHolder gvh;
        private static LogicThread appToRun = null;

        public static void main(String args[]) {

            // Load the participants
            //participants = IdentityLoader.loadIdentities(IDENTITY_FILE_URL);
            // Put number of robots being used here
            readConfigFile("testmain_config.txt");
            System.out.println("numRobots:"+numRobots+", selectedRobot"+selectedRobot);


            //botInfo[2] = new BotInfoSelector("blue", Common.ARDRONE2, Common.NEXUS7);
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
	
            gvh = new RealGlobalVarHolder(participants[0][selectedRobot], hm_participants, botInfo[selectedRobot].type, participants[1][selectedRobot], botInfo[selectedRobot].vrpn);
            gvh.num_robots = numRobots;
            //((RealGlobalVarHolder)gvh).position_data_topic = botInfo[selectedRobot].vrpn;
            /*JavaRosWrapper wrapper;
            wrapper = new JavaRosWrapper("ws://localhost:9090", botInfo[selectedRobot].name, gvh, "Quadcopter");
            wrapper.subscribe_to_ROS(botInfo[selectedRobot].vrpn, "Position");*/


            Initiator init = new Initiator(gvh, participants, botInfo, selectedRobot);


//            create finished
            init.connect();


            appToRun = new FollowApp(gvh);


            init.createAppInstance(appToRun);


            init.launch(6, 10);

        }

        private static void readConfigFile(String filename) {
            try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String st;
                String[] robot_st = new String[3];

                while ((st = reader.readLine()) != null) {
                    String st_split[] = st.split(": ");

                    switch(st_split[0])
                    {
                        case "numRobots":
                            numRobots = Integer.parseInt(st_split[1]);
                            botInfo = new BotInfoSelector[numRobots];
                            break;
                        case "selectedRobot":
                            selectedRobot = Integer.parseInt(st_split[1]);
                            break;
                        case "Robot":
                            int robot_num = Integer.parseInt(st_split[1]);

                            if (robot_num < numRobots) {
                                for (int i = 0; i < 3; i++) {
                                    robot_st[i] = reader.readLine();
                                }

                                //For this to work, need to initialize after numRobots
                                botInfo[robot_num] = new BotInfoSelector(robot_st, robot_num);
                            }
                            break;
                    }
                }
            }
            catch(Exception e){
                System.out.println("Failed to read config file");
                e.printStackTrace();
            }
        }

}
