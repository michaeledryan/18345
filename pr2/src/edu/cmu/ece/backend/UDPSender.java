package edu.cmu.ece.backend;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

public class UDPSender implements Runnable {
	private static UDPSender instance = null;
	private UDPManager udp = UDPManager.getInstance();

	private static long timeout = 10; // ms

	ConcurrentHashMap<UDPRequestHandler, ConcurrentSkipListSet<Integer>> received;
	ConcurrentLinkedQueue<UDPPacketSender> queue;
	ConcurrentLinkedQueue<UDPPacketSender> resendQueue;

	public static UDPSender getInstance() {
		if (instance == null) {
			instance = new UDPSender();
		}
		return instance;
	}

	private UDPSender() {
		received = new ConcurrentHashMap<UDPRequestHandler, ConcurrentSkipListSet<Integer>>();
		queue = new ConcurrentLinkedQueue<UDPPacketSender>();
		resendQueue = new ConcurrentLinkedQueue<UDPPacketSender>();
	}

	/*
	 * This just keeps attempting to send a packet if there are any TODO: add
	 * bandwidth limiting
	 */
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			UDPPacketSender sender;
			if (!resendQueue.isEmpty() || !queue.isEmpty()) {
				while (!resendQueue.isEmpty() || !queue.isEmpty()) {
					if (!resendQueue.isEmpty()) {
						sender = resendQueue.remove();
						sender.send(udp);
					}

					else if (!queue.isEmpty()){
						sender = queue.remove();
						sender.send(udp);
					}
				}
			}
		}
	}

	/*
	 * Add a request to send numPackets packets to our send queue.
	 */
	public void requestToSend(UDPRequestHandler request, int numPackets) {
		for (int i = 0; i < numPackets; i++) {
			UDPPacketSender sender = new UDPPacketSender(request, i, timeout, 5);
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

		ConcurrentSkipListSet<Integer> acked = received.get(requester);
		if (!acked.contains(seqNum)) {
			System.err.print(" " + seqNum + ";");
			// System.out.println(acked.toString());
			// Recreate requester so we can reschedule it... TimerTask is dumb
			UDPPacketSender newRequest = new UDPPacketSender(requester, seqNum,
					timeout, request.getTTL() - 1);
			resendQueue.add(newRequest);
		}
	}

	public void ackPacket(UDPRequestHandler requester, int seqNum) {
		ConcurrentSkipListSet<Integer> acked = received.get(requester);
		acked.add(seqNum);
		received.put(requester, acked);

	}
}
