package edu.cmu.ece.backend;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles the sending of a specific UDPPacket.
 * 
 * @author michaels
 * 
 */
public class UDPPacketSender extends TimerTask implements
		Comparable<UDPPacketSender> {
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

	/**
	 * Sends a packet it has not yet been ACKed.
	 * 
	 * @param udp
	 */
	public void send(UDPManager udp) {
		if (requester.isAlive()) {
			udp.sendPacket(requester.getPacket(seqNum).getPacket());
			new Timer().schedule(this, timeout);
		}
	}

	/**
	 * Along with send, keeps resending packets if not ACKed.
	 */
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

	// For PriorityQueue usage
	@Override
	public int compareTo(UDPPacketSender o) {
		return seqNum - o.seqNum;
	}
}
