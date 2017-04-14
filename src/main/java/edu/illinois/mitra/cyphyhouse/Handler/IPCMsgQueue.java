package edu.illinois.mitra.cyphyhouse.Handler;

/**
 * Created by SC on 2/23/17.
 */
public class IPCMsgQueue {
    final static String TAG="IPCMsgQueue";
    IPCMessage myMsgQ;

    public IPCMsgQueue(){
        myMsgQ = null;
        isQuitting = false;
    }

    private boolean isQuitting;

    //Right now this is a blocking function
    public IPCMessage getNext() {
        while(true){
            synchronized (this){
                if(isQuitting)
                    return null;
                IPCMessage ret = myMsgQ;
                if(myMsgQ != null){
                    myMsgQ = myMsgQ.next;
                    ret.next = null;
                    ret.setInUse();
                    return ret;
                }
            }
        }
    }

    void quit(boolean gently){
        synchronized (this){
            if(isQuitting)
                return;
            if(gently){
                //TODO: remove all things
            }
            else{
                //TODO: remove something?
                myMsgQ = null;
            }
        }
    }

    //TODO to utilize the parameter "when"
    final boolean enqueueMsg(IPCMessage msg, Long when){
        if(msg.isInUse()){
            throw new RuntimeException(msg + " Message is already in use");
        }
        synchronized (this){
            if(isQuitting){
                RuntimeException e = new RuntimeException(msg.target+"'s "+TAG+" is quitting");
                System.err.println(e.getMessage());
                return false;
            }

            IPCMessage curr = myMsgQ;
            if(curr==null){
                msg.next = null;
                myMsgQ = msg;
            }
            else{
                //TODO: change to utilize "when"
                msg.next = myMsgQ;
                myMsgQ = msg;
            }
        }
        return true;
    }
}
