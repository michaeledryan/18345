package edu.cmu.ece.backend;

import java.util.Timer;
import java.util.TimerTask;

public class UDPPacketSender extends TimerTask {
	private UDPSender sender = UDPSender.getInstance();
	private UDPRequestHandler requester;
	private int seqNum;
	private long timeout;

	public UDPPacketSender(UDPRequestHandler requester_in, int seqNum_in,
			long timeout_in) {
		requester = requester_in;
		seqNum = seqNum_in;
		timeout = timeout_in;
	}

	public void send(UDPManager udp) {
		if (requester.isAlive()) {
			udp.sendPacket(requester.getPacket(seqNum).getPacket());
			new Timer().schedule(this, timeout);
		}
	}

	@Override
	public void run() {
		if (requester.isAlive()) {
			sender.requestResend(this);
		}
	}

	public UDPRequestHandler getRequester() {
		return requester;
	}

	public int getSeqNum() {
		return seqNum;
	}
}
