package edu.cmu.ece.backend;

import java.net.DatagramPacket;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;

public class UDPSender implements Runnable {
	private static UDPSender instance = null;
	private UDPManager udp = UDPManager.getInstance();

	private static long timeout = 20; // ms

	Map<UDPRequestHandler, ConcurrentSkipListSet<Integer>> acked = new ConcurrentHashMap<UDPRequestHandler, ConcurrentSkipListSet<Integer>>();
	Map<UDPRequestHandler, ConcurrentSkipListSet<Integer>> nacked = new ConcurrentHashMap<UDPRequestHandler, ConcurrentSkipListSet<Integer>>();
	Queue<UDPPacketSender> queue = new PriorityBlockingQueue<UDPPacketSender>();
	Queue<UDPPacketSender> resendQueue = new PriorityBlockingQueue<UDPPacketSender>();
	Map<UDPRequestHandler, Integer> clientsToBitrates = new ConcurrentHashMap<UDPRequestHandler, Integer>();

	public static UDPSender getInstance() {
		if (instance == null) {
			instance = new UDPSender();
		}
		return instance;
	}

	private UDPSender() {
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
						UDPRequestHandler requester = sender.getRequester();
						if (requester.canISend(65000)) {

							sender.send(udp);
							// Remove from nacked - we finally responded
							nacked.get(sender.getRequester()).remove(
									sender.getSeqNum());
						} else {
							resendQueue.add(sender);
						}

					}

					else if (!queue.isEmpty()) {
						sender = queue.remove();
						UDPRequestHandler requester = sender.getRequester();
						if (requester.canISend(65000)) {

							sender.send(udp);

						} else {
							queue.add(sender);
						}
					}

				}
			}
		}
	}

	/*
	 * Add a request to send numPackets packets to our send queue.
	 */
	public void requestToSend(UDPRequestHandler request, int numPackets) {
		// Create ACK/NACK lists
		acked.put(request, new ConcurrentSkipListSet<Integer>());
		nacked.put(request, new ConcurrentSkipListSet<Integer>());

		// Queue up packet requests
		for (int i = 0; i < numPackets; i++) {
			UDPPacketSender sender = new UDPPacketSender(request, i, timeout, 5);
			queue.add(sender);
		}
	}

	/*
	 * Allow a PacketSender to add itself back to the queue for resending. If
	 * that packet was acked, then we don't resend it.
	 */
	public void requestResend(UDPPacketSender request) {
		UDPRequestHandler requester = request.getRequester();
		int seqNum = request.getSeqNum();

		ConcurrentSkipListSet<Integer> ackedSet = acked.get(requester);
		if (!ackedSet.contains(seqNum)) {
			// System.err.print(" " + seqNum + ";");
			// System.out.println(acked.toString());
			// Recreate requester so we can reschedule it... TimerTask is dumb
			UDPPacketSender newRequest = new UDPPacketSender(requester, seqNum,
					timeout, request.getTTL() - 1);
			resendQueue.add(newRequest);
		}
	}

	public void ackPacket(UDPRequestHandler requester, int seqNum) {
		ConcurrentSkipListSet<Integer> ackedSet = acked.get(requester);
		if (ackedSet != null)
			ackedSet.add(seqNum);
	}

	public void nackPacket(UDPRequestHandler requester, int seqNum) {
		if (requester == null)
			System.out.println("REQUESTER IS NULL");
		if (nacked == null)
			System.out.println("NACKED IS NULL");
		ConcurrentSkipListSet<Integer> nackedSet = nacked.get(requester);
		if (nackedSet == null)
			System.out.println("NACKEDSET IS NULL");
		if (!nackedSet.contains(seqNum)) {
			UDPPacketSender newRequest = new UDPPacketSender(requester, seqNum,
					timeout, 5);
			nackedSet.add(seqNum);
			resendQueue.add(newRequest);
		}
	}

	public void clearRequester(UDPRequestHandler requester) {
		acked.get(requester).clear();
		nacked.get(requester).clear();
	}
}
