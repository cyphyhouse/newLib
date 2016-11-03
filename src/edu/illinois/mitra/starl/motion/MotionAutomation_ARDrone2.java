package edu.illinois.mitra.starl.motion;

import android.graphics.Point;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import de.yadrone.base.command.LEDAnimation;
import de.yadrone.base.navdata.NavDataManager;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.models.ModelARDrone2;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;

/**
 * Created by SC on 6/7/16.
 */
public class MotionAutomation_ARDrone2 extends RobotMotion {
    protected static final String TAG = "MotionAuto ARDrone2";
    protected static final String ERR = "Critical Error";
    final int safeHeight = 1000;//TODO need to see the unit

    protected GlobalVarHolder gvh;

    //Motion Tracking
    protected ItemPosition dest = null;
    private ModelARDrone2 mypos = null;

    protected enum STAGE{
        INIT, MOVE, HOVER, TAKEOFF, LAND, GOAL, STOP
    }
    private STAGE prev=null, next = null;
    protected STAGE stage = STAGE.INIT;
    volatile protected boolean running = false;
    volatile protected boolean inAir = false;
    boolean colliding = false;
    private static int timecount=0;//notice: only for the test of dummyGPS

    //control logic related
    double saturationLimit = 8;
    double windUpLimit = 50;
    int filterLength = 2;
    double Kpx = 0.0013;
    double Kpy = 0.0013;
    double Kpyaw = 0.16;
    double Kix = 0;
    double Kiy = 0;
    double Kdx = -0.0030;//notice: last working -0.0028
    double Kdy = -0.0030;
    double Kdyaw = -0.15;
    PIDController PID_x = new PIDController(Kpx, Kix, Kdx, saturationLimit, windUpLimit, filterLength);
    PIDController PID_y = new PIDController(Kpy, Kiy, Kdy, saturationLimit, windUpLimit, filterLength);
    PIDController PID_yaw = new PIDController(Kpyaw, 0, Kdyaw, 0.3, 5, 1);


    //=========================hardware related==========================
    public IARDrone droneInstance = null;
    protected CommandManager cmd;//need? TODO, see ModelARDrone2.java
    private NavDataManager nav;
    private int maxSpeed = 2;
    private boolean outdoor = false;
    class ARDrone2_AttitudeListn implements de.yadrone.base.navdata.AttitudeListener{
        private String tag = "AttitudeInfo";
        ARDrone2_AttitudeListn(String name){
            tag = name;
        }
        public void attitudeUpdated(float pitch, float roll, float yaw)
        {
            //Log.i(tag + " rcvData", "Pitch: " + pitch + " Roll: " + roll + " Yaw: " + yaw);
        }

        public void attitudeUpdated(float pitch, float roll) { ;}
        public void windCompensation(float pitch, float roll) { ;}
    }
    class ARDrone2_BatteryListn implements de.yadrone.base.navdata.BatteryListener{
        private String TAG = "Battery Info";
        public void batteryLevelChanged(int var1){
            if(var1<20)
                Log.e(TAG, "Low battery:"+var1+"%" + "         Low battery:"+var1+"%");
//            else if(var1 % 10 == 0)
//                Log.e(TAG, "Battery:"+var1+"%" + "        Battery:"+var1+"%");
        }
        public void voltageChanged(int var1){;}
    }
    private void HardwareInit(){
        if(droneInstance == null)
            Log.e(TAG, "wrong order in init hardware. droneInstance=null.");
        try{
            droneInstance.reset();
            //droneInstance.start();
            cmd = droneInstance.getCommandManager();
            nav = droneInstance.getNavDataManager();
//            for(int tt=0;tt<100;tt++)//clear the potential emergency signal
                cmd.emergency();
            cmd.setOutdoor(outdoor, outdoor);
            nav.addAttitudeListener(new ARDrone2_AttitudeListn(mypos.name));
            nav.addBatteryListener(new ARDrone2_BatteryListn());
            droneInstance.setSpeed(maxSpeed);
            cmd.setMaxAltitude(400);
            cmd.setMinAltitude(70);
            cmd.setLedsAnimation(LEDAnimation.BLINK_ORANGE, 3, 10);//some sig for us
        }catch (Exception exc)
        {
            exc.printStackTrace();
        }
        transBackUDPInit();
    }
    //====================================================
    @Override
    public synchronized void start() {
        mypos = (ModelARDrone2) gvh.plat.getModel();
        droneInstance = new ARDrone(mypos.ipAddr, null);
        Log.i(TAG, "drone instance created with IP "+mypos.ipAddr);
        HardwareInit();
        running = true;
        inMotion = true;
        super.start();
        gvh.log.d(TAG, "STARTED!");
    }


    public void cancel(){
        running = false;
        //TODO maybe? some command to disconnect the hardware.
    }


    //TODO need to pass some more parameters into this param
    private volatile MotionParameters param = MotionParameters.defaultParameters();

    public MotionAutomation_ARDrone2(GlobalVarHolder gvhin){
        super(gvhin.id.getName());
        gvh = gvhin;
    }

    public void setParameters(MotionParameters param){
        this.param = param;
    }

    public void turnTo(ItemPosition dest){
        goTo(dest);
    }

    public void goTo(ItemPosition dest, ObstacleList obsList) {
        goTo(dest);
    }

    public void goTo(ItemPosition dest) {
        Log.i("Automation ARDrone2", "GoTo called!!!");
        if( this.dest == null || (!inMotion && !this.dest.equals(dest))) {
            done = false;
            this.dest = new ItemPosition(dest.name,dest.x,dest.y,dest.z);
            motionStart();
            Log.i("Automation ARDrone2", "GoTo Executed!!!, new dest("+dest.x+","+dest.y+","+dest.z+")");
            running = true;
        }
    }

    //====================================================
    //fixme: this three func seems like need some commands toward the HW, but not sure yet.
    private void motionStart(){
        stage = STAGE.INIT;
        next = STAGE.INIT;
        inMotion = true;
    }

    @Override
    public void motion_stop() {
        stage = STAGE.LAND;
        next = STAGE.LAND;
        inMotion = false;
    }

    @Override
    public void motion_resume() {
        running = true;
    }

    //=======================Data Transfer Back to Host PC============================= notice:temporary put in here
    public static final String SERVERIP = "192.168.1.104";
    public static final int SERVERPORT = 9876;
    DatagramSocket transBackSocket;
    InetAddress transBackAddr;
    private void transBackUDPInit(){
        try {
            transBackAddr = InetAddress.getByName(SERVERIP);
            Log.i(TAG, "Client: Start connecting\n");
            transBackSocket = new DatagramSocket(SERVERPORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    long dura;
    private void transBackUDP(){
        String transData = mypos.name + "|" + Integer.toString(mypos.x) + "|" + Integer.toString(mypos.y) + "|"
                + Integer.toString((int)mypos.currYaw) + "|" + Integer.toString(dest.x) + "|"
                + Integer.toString(dest.y) + "|(" + (float)rollOut +", " + (float)pitchOut + ", "
                + (float)vertVOut + ", " + (float)spinVOut + ")|";
        byte[] buf = transData.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length,
                transBackAddr, SERVERPORT);
//        System.out.println("Client: Sending ‘" +  new String(buf) + "’\n");
        try {
            transBackSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //=======================private helpers=============================
    private double getDistance(){
        if(mypos==null) {
            Log.e(TAG, "mypos is null");
            return Math.sqrt(Math.pow((0 - dest.x), 2) + Math.pow((0 - dest.y), 2));
        }
        if(dest==null) {
            Log.e(TAG, "dest is null");
            return Math.sqrt(Math.pow((mypos.x - 0), 2) + Math.pow((mypos.y - 0), 2));
        }
//        timecount++;
//        if(timecount>=5000){
//            timecount = 0;
//            Log.i(TAG, "dist=0");
//            return 0;
//        }
        return Math.sqrt(Math.pow((mypos.x - dest.x), 2) + Math.pow((mypos.y - dest.y), 2));
    }
    private double ScaleByLimit(double in, double absLimit){
        if(in < 0 && in < -absLimit)
            return -absLimit;
        else if(in > 0 && in > absLimit)
            return absLimit;
        return in;
    }
    private double ScaleByLimit(double in){
        if(in < 0 && in < 1.0)
            return -1.0;
        else if(in > 0 && in > 1.0)
            return 1.0;
        return in;
    }

    static Point oldpos = new Point(19381,-238482); //a random impossible starting place
    double desiredAccX = 0;
    double desiredAccY = 0;
    double rollOut, pitchOut, vertVOut=0, yawOut, spinVOut;
    final double MURatio = 0.0485;
    private void CalculatedMove(){
        desiredAccX = PID_x.getCommand(mypos.x, dest.x);
        desiredAccY = PID_y.getCommand(mypos.y, dest.y);
        //calculations================================================
        pitchOut = -Math.asin(MURatio * (desiredAccY * Math.cos(Math.toRadians(mypos.currYaw)) -
                                desiredAccX * Math.sin(Math.toRadians(mypos.currYaw))) );
        rollOut = Math.asin(-MURatio * (desiredAccY * Math.sin(Math.toRadians(mypos.currYaw )) +
                                desiredAccX * Math.cos( Math.toRadians(mypos.currYaw))) );
        yawOut = Math.atan2(dest.y - mypos.y, dest.x - mypos.x);
        if(yawOut < -Math.PI/2)
            yawOut += (3.0/2.0*Math.PI);
        else
            yawOut -= (Math.PI/2.0);
        //to commands==============================================
        //notice: neg const since contradiction between direction of drone's movement and positive yaw direction===
//        spinVOut = -0.09 * (yawOut - Math.toRadians(mypos.currYaw));
        double yawdif = yawOut - Math.toRadians(mypos.currYaw);
        if(yawdif>=Math.PI && yawdif<=2*Math.PI)
            yawdif-=(2*Math.PI);
        else if(yawdif>=-2*Math.PI && yawdif<=-Math.PI)
            yawdif+=(2*Math.PI);
//        if( Math.abs(180 - Math.abs(mypos.currYaw - Math.toDegrees(yawOut))) < 30 ) {
//            spinVOut = Math.signum(-mypos.currYaw + Math.toDegrees(yawOut)) * PID_yaw.getCommand(Math.toRadians(mypos.currYaw), yawOut);
//        }
//        else
//            spinVOut = -PID_yaw.getCommand(Math.toRadians(mypos.currYaw), yawOut);
        spinVOut = -PID_yaw.getCommand(0, yawdif);
        if( Math.abs( mypos.currYaw - Math.toDegrees(yawOut) ) > 10){
            rollOut = 0;
            pitchOut = 0;
            vertVOut = 0;
        }
        else
            spinVOut = 0;
        cmd.move((float)rollOut, (float)pitchOut, (float)vertVOut, (float)spinVOut).doFor(1);
    }


    @Override
    public void run(){
        super.run();
        gvh.threadCreated(this);
        while(true){
            if(!inMotion){
                //Log.i("Automation ARDrone2", "Thread running, but skipping");
                continue;
            }
            //Log.i("Automation ARDrone2", "Thread running, with state "+ StageToString(stage));
            double distance = 0;
            if(colliding) continue;
            mypos = (ModelARDrone2) gvh.gps.getMyPosition();
            if(stage==STAGE.MOVE) {
                distance = getDistance();
            }
            if(mypos==null || dest==null)
                Log.d(TAG, "["+StageToString(stage)+"]");
            else if(!oldpos.equals(mypos.x, mypos.y)) {
                oldpos.set(mypos.x, mypos.y);
                Log.d(TAG, "["+StageToString(stage)+"] ("+mypos.x+","+mypos.y+","+mypos.z+")->("
                        + dest.x + "," + dest.y + "," + dest.z + ")  " + (int)mypos.currYaw +"->" + (int)Math.toDegrees(yawOut)+"deg" + //);
                        "\taccl=" + (float) desiredAccX + ", " + (float) desiredAccY +//);
                        "  \tmove(" + (float)rollOut + ", " + (float)pitchOut+ ", " + (float)vertVOut+ ", " + (float)spinVOut +")");
                transBackUDP();
            }
            switch (stage){
                case INIT:
                    next = STAGE.TAKEOFF;
                    break;
                case TAKEOFF:
                    if(!inAir) {
                        cmd.takeOff();
                        inAir = true;
                        try {
                            sleep(5000 , 0);
                        } catch (Exception exc){
                            exc.printStackTrace();
                        }
                    }
                    next = STAGE.MOVE;
                    break;
                case MOVE:
                    inMotion = true;
//                    if(mypos.z < safeHeight)
//                        cmd.move(0, 0, 1, 0).doFor(1);
                    if(distance <= param.GOAL_RADIUS){ //notice: only for current demos
                        next = STAGE.GOAL;
                    }
                    else {
                        this.CalculatedMove();
                        next = STAGE.MOVE;
                    }
                    break;
                case HOVER:
                    cmd.hover();
                    inMotion = false;
                    try {
                        sleep(5000 , 0);
                    } catch (Exception exc){
                        exc.printStackTrace();
                    }
                    next = STAGE.HOVER;
                    break;
                case GOAL:
                    gvh.log.i(TAG, "At goal!");
                    gvh.log.i("DoneFlag", "write");
                    if(param.STOP_AT_DESTINATION)
                        next = STAGE.HOVER;
                    else
                        next = STAGE.LAND;
                    done = true;
                    inMotion = false;
                    break;
                case LAND:
                    cmd.landing();
                    try {
                        sleep(300, 0);
                    } catch (Exception exc){
                        exc.printStackTrace();
                    }
                    running = false;
                    inMotion = false;
                    inAir = false;
                    break;

            }
            prev = stage;
            stage = next;
        }
    }

    private String StageToString(STAGE curr)
    {
        switch (curr){
            case INIT:
                return "INIT";
            case MOVE:
                return "MOVE";
            case HOVER:
                return "HOVER";
            case TAKEOFF:
                return "TAKEOFF";
            case LAND:
                return "LAND";
            case GOAL:
                return "GOAL";
            case STOP:
                return "STOP";
        }
        return "UNKNOWN";
    }

}
