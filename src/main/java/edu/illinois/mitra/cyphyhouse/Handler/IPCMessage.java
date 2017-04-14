package edu.illinois.mitra.cyphyhouse.Handler;

/**
 * Created by SC on 2/23/17.
 */
public class IPCMessage {
    private final static int IN_USE_FLAG = 1;
    private final static String TAG = "IPCMessage";
    public int what;
    public Double arg1=null;
    public Double arg2=null;
    public Double arg3=null;
    public Double arg4=null;
    public Object obj=null;
    int myFlag = 0;
    Runnable callback = null;

    long when;
    IPCHandler target;

    private static IPCMessage memPool = null;
    private static final Object memPoolSync = new Object();
    private final static short POOL_SIZE_LIMIT = 100;
    private static short poolSize = 0;
    IPCMessage next = null;

    /*
     * Here we prefer to get an instance of IPCMessage from the obtain().
     * This is due to the fact that many instances of IPCMessage may be
     * created and constantly allocate/free the memory. We are trying
     * to implement a simple memory pool here to leverage this situation.
     *
     * Therefore, also, the constructor is private.
     */
    private IPCMessage(){}
    public static IPCMessage construct(){
        IPCMessage ret;
        synchronized (memPoolSync){
            if(memPool != null){
                ret = memPool;
                memPool = ret.next;
                ret.next = null;
                poolSize--;
                return ret;
            }
        }
        return new IPCMessage();
    }

    private static void cleanMsg(IPCMessage m){
        m.arg1 = null;
        m.arg2 = null;
        m.arg3 = null;
        m.arg4 = null;
        m.obj = null;
        m.callback = null;
        m.target = null;
        m.myFlag = 0;
    }

    public void destruct(){
        synchronized (memPoolSync){
            if(poolSize<POOL_SIZE_LIMIT){
                IPCMessage.cleanMsg(this);
                next = memPool;
                memPool = this;
                poolSize++;
            }
        }
    }

    public static IPCMessage obtain(IPCHandler h, int what) {
        IPCMessage m = construct();
        m.target = h;
        m.what = what;
        return m;
    }

    public static IPCMessage obtain(IPCHandler h, int what, Object obj) {
        IPCMessage m = construct();
        m.target = h;
        m.what = what;
        m.obj = obj;
        return m;
    }

    public static IPCMessage obtain(IPCHandler h, int what, Object obj, double arg1, double arg2) {
        IPCMessage m = construct();
        m.target = h;
        m.what = what;
        m.obj = obj;
        m.arg1 = arg1;
        m.arg2 = arg2;
        return m;
    }

    public static IPCMessage obtain(IPCHandler h, int what, Runnable callback) {
        IPCMessage m = construct();
        m.target = h;
        m.what = what;
        m.callback = callback;
        return m;
    }

    boolean isInUse(){
        return ((myFlag & IN_USE_FLAG) == IN_USE_FLAG);
    }

    void setInUse(){
        myFlag |= IN_USE_FLAG;
    }

    public void sendToHandler(){
        this.target.sendMsg(this);
    }

    public String toString(){
        return TAG +
                "->" +
                target.getName() +
                ",t" +
                what +
                ":(" +
                arg1 +
                "," +
                arg2 +
                "," +
                arg3 +
                "," +
                arg4 +
                ")";
    }
}
