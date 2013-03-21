package edu.cmu.ece.backend;

import java.util.Timer;
import java.util.TimerTask;

public class UDPPacketSender extends TimerTask {
	private UDPSender sender = UDPSender.getInstance();
	private UDPRequestHandler requester;
	private int seqNum;
	private long timeout;
	private int ttl;

	public UDPPacketSender(UDPRequestHandler requester_in, int seqNum_in,
			long timeout_in, int ttl) {
		requester = requester_in;
		seqNum = seqNum_in;
		timeout = timeout_in;
		this.ttl = ttl;
	}

	public void send(UDPManager udp) {
		if (requester.isAlive()) {
			udp.sendPacket(requester.getPacket(seqNum).getPacket());
			new Timer().schedule(this, timeout);
		}
	}

	@Override
	public void run() {
		if (requester.isAlive() && ttl != 0) {
			sender.requestResend(this);
		}
	}

	public UDPRequestHandler getRequester() {
		return requester;
	}

	public int getSeqNum() {
		return seqNum;
	}
	
	public int getTTL() {
		return ttl;
	}
}
