package edu.illinois.mitra.starl.models;

import android.util.Log;

import edu.illinois.mitra.starl.exceptions.ItemFormattingException;
import edu.illinois.mitra.starl.interfaces.TrackedRobot;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.illinois.mitra.starl.objects.Point3d;
import edu.illinois.mitra.starl.objects.PositionList;

/**
 * Created by SC on 6/6/16.
 */
public class ModelARDrone2 extends ItemPosition implements TrackedRobot{
    String TAG = "ModelARDrone2";
    public String ipAddr;
    //the position/velocity/status information
    //angular velocity in radius/second, regular velocity in meter/second
    //position x,y,z; name defined in ItemPosition
    public double currYaw, currPitch, currRoll;
    public double vYaw, vPitch, vRoll;
    public double vX, vY, vZ;

    //configurations supported by the drone
    //TODO: public double maxAltitude, minAltitude;

    public ModelARDrone2(String descStr) throws ItemFormattingException {
        String[] parts = descStr.replace(",", "").split("\\|");
        if(parts.length==10){
            this.name = parts[1];
            this.x = Integer.parseInt(parts[2]);
            this.y = Integer.parseInt(parts[3]);
            this.z = Integer.parseInt(parts[4]);
            this.currRoll = Integer.parseInt(parts[5]);
            this.currPitch = Integer.parseInt(parts[6]);
            this.currYaw = Integer.parseInt(parts[7]);
            this.ipAddr = parts[8];
//            Log.i(TAG, this.name + '|' + this.x + '|' + this.y + '|' + this.z + '|' +
//                    this.currRoll + '|' + this.currPitch + '|'  + this.currYaw + '|'  +
//                    this.ipAddr);
        } else {
            throw new ItemFormattingException("Should be length 10, is length " + parts.length);
        }
        initHelper();
    }


    public ModelARDrone2(String name, int x, int y){
        super(name, x, y, 0);
        this.currYaw = 0;
        this.currPitch = 0;
        this.currRoll = 0;
        initHelper();
    }

    public ModelARDrone2(String name, int x, int y, int z){
        super(name, x, y, z);
        this.currYaw = 40;
        this.currPitch = 0;
        this.currRoll = 0;
        initHelper();
        Log.i("Model ARDrone2", this.name + "constructor called, this.ip="+this.ipAddr);
    }

    private void initHelper(){
        if(this.ipAddr==null)
            this.ipAddr = "192.168.1.10";
//            this.ipAddr = "10.195.40.51";
        vX=0;
        vY=0;
        vZ=0;
        vYaw=0;
        vPitch=0;
        vRoll=0;
    }

    @Override
    public void initialize(){
    }

    //TODO, temp stub here
    @Override
    public Point3d predict(double[] noises, double timeSinceUpdate) {
        return new Point3d(x,y,z);
    }

    //TODO, temp stub here
    @Override
    public void collision(Point3d collision_point) {
    }

    //TODO, temp stub here
    @Override
    public void updatePos(boolean followPredict) {
    }

    //TODO: extend angular velocity
    @Override
    public boolean inMotion() {
        return (vX != 0 || vY != 0 || vZ != 0);
    }

    //TODO, temp stub here
    @Override
    public void updateSensor(ObstacleList obspoint_positions,
                             PositionList<ItemPosition> sensepoint_positions) {
    }

}
