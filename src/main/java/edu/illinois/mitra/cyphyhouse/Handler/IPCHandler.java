package edu.illinois.mitra.cyphyhouse.Handler;

/**
 * Created by SC on 2/23/17.
 */
public class IPCHandler {
    public String getName(){
        return "";
    }

    IPCMsgQueue myMsgQ;
    Looper myLooper;

    public IPCHandler(Looper looper){
        myLooper = looper;
        myMsgQ = looper.myQueue;
    }

    public IPCHandler(LooperThread looperTh){
        myLooper = looperTh.getLooperRef();
        myMsgQ = looperTh.getLooperRef().myQueue;
    }

    private static void handleCallback(IPCMessage msg){
        Thread th = new Thread(msg.callback);
        th.start();
    }


    public void dispatchMessage(IPCMessage msg){
        if(msg.callback != null){
            msg.callback.run();
        }else{
            handleMessage(msg);
        }
    }

    /*
     * The functions needs to be overridden in the subclasses.
     */
    public void handleMessage(IPCMessage msg){}
    public Object handleDirectly(IPCMessage msg){return null;}

    public IPCMessage obtaintMsg(int what){
        IPCMessage ret = null;
        return IPCMessage.obtain(this, what);
    }

    public IPCMessage obtaintMsg(int what, Object obj){
        IPCMessage ret = null;
        return IPCMessage.obtain(this, what, obj);
    }

    public IPCMessage obtaintMsg(int what, Object obj, double arg1, double arg2){
        IPCMessage ret = null;
        return IPCMessage.obtain(this, what, obj, arg1, arg2);
    }

    public boolean sendMsg(IPCMessage msg){
        return myMsgQ.enqueueMsg(msg, null);
    }

    //send the message to others to be handled directly by calling other functions
    public Object sendDirectly(IPCMessage msg){
        Object ret = handleDirectly(msg);
        msg.destruct();
        return ret;
    }

}
