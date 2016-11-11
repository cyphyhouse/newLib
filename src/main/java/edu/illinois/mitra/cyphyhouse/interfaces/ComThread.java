package edu.illinois.mitra.cyphyhouse.interfaces;

import java.util.ArrayList;

import edu.illinois.mitra.cyphyhouse.comms.UdpMessage;

public interface ComThread extends Cancellable {

	abstract void setMsgList(ArrayList<UdpMessage> ReceivedMessageList);
	
	abstract void write(UdpMessage msg, String IP);

	abstract void start();
	
}
