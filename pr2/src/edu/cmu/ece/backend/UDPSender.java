package edu.cmu.ece.backend;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;


public class UDPSender implements Runnable {
	private static UDPSender instance = null;
	private UDPManager udp = UDPManager.getInstance();
	
	private static long timeout = 1000; // ms
	
	ConcurrentHashMap<UDPRequestHandler, ConcurrentSkipListSet<Integer>> received;
	ConcurrentLinkedQueue<UDPPacketSender> queue;

	public static UDPSender getInstance() {
		if (instance == null) {
			instance = new UDPSender();
		}
		return instance;
	}

	private UDPSender() {
		received = new ConcurrentHashMap<UDPRequestHandler, ConcurrentSkipListSet<Integer>>();
		queue = new ConcurrentLinkedQueue<UDPPacketSender>();
	}

	/*
	 * This just keeps attempting to send a packet if there are any TODO: add
	 * bandwidth limiting
	 */
	@Override
	public void run() {
		while (true) {
			if (!queue.isEmpty()) {
				UDPPacketSender sender = queue.remove();
				sender.send(udp);
			}
		}
	}

	/*
	 * Add a request to send numPackets packets to our send queue.
	 */
	public void requestToSend(UDPRequestHandler request, int numPackets) {
		for (int i = 0; i < numPackets; i++) {
			UDPPacketSender sender = new UDPPacketSender(request, i, timeout);
			queue.add(sender);
			received.put(request, new ConcurrentSkipListSet<Integer>());
		}
	}

	/*
	 * Allow a PacketSender to add itself back to the queue for resending. If
	 * that packet was acked, then we don't resend it.
	 */
	public void requestResend(UDPPacketSender request) {
		UDPRequestHandler requester = request.getRequester();
		int seqNum = request.getSeqNum();

		// Recreate requester so we can reschedule it... TimerTask is dumb
		UDPPacketSender newRequest = new UDPPacketSender(requester, seqNum,
				timeout);

		ConcurrentSkipListSet<Integer> acked = received.get(requester);
		if (!acked.contains(seqNum)) {
			queue.add(newRequest);
		}
	}

	public void ackPacket(UDPRequestHandler request, int seqNum) {
		ConcurrentSkipListSet<Integer> acked = received.get(request);
		acked.add(seqNum);
	}
}
