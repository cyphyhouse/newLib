package edu.illinois.mitra.cyphyhouse.Handler;

/**
 * Created by SC on 4/6/17.
 */
public class LooperThread implements Runnable{
    private volatile Looper looperRef = null;
    Thread myTh = null;

    public LooperThread(){}

    public void initThread(){
        if(myTh != null)
            return;//TODO or do something?
        myTh = new Thread(this);
        myTh.start();
        while(looperRef==null){;}
    }

    public void run() {
        Looper.prepare();
        while(looperRef == null)
            looperRef = Looper.getMyLooper();
        Looper.loop();
    }

    public Looper getLooperRef(){
        return looperRef;
    }

}

