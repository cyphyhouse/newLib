package edu.illinois.mitra.cyphyhouse.Handler;

/**
 * Created by SC on 2/23/17.
 */
public class Looper {
    static final ThreadLocal<Looper> myThreadLocal = new ThreadLocal<Looper>();

    final IPCMsgQueue myQueue;
    final Thread myThread;

    public static void prepare(){
        if (myThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        myThreadLocal.set(new Looper());
    }

    public void quit(){
        myQueue.quit(false);
        //TODO: let's only support force quit first :(
    }

    public static Looper getMyLooper(){
        return myThreadLocal.get();
    }


    private Looper(){
        myQueue = new IPCMsgQueue();
        myThread = Thread.currentThread();
    }

    public boolean isCurrentThread() {
        return Thread.currentThread() == myThread;
    }

    public static void loop(){
        final Looper myself = getMyLooper();
        final Thread thRunOn = myself.myThread;
        if(myself == null){
            throw new RuntimeException("No Looper object to be associated with");
        }
        final IPCMsgQueue msgq = myself.myQueue;
        while(true){
            IPCMessage nextMsg = msgq.getNext();
            if(nextMsg == null)
                return;
            Thread myTh = Thread.currentThread();
            if(myTh != thRunOn)
                throw new RuntimeException("The thread identity is changing ");

            //dispatch message, connect to IPCHandler
            nextMsg.target.dispatchMessage(nextMsg);

            myTh = Thread.currentThread();
            if(myTh != thRunOn)
                throw new RuntimeException("The thread identity is changing ");
        }
    }
}
