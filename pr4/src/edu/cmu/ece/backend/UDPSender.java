package edu.cmu.ece.backend;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Manages the sending of UDPPackets as responses to remote file requests.
 * 
 * @author michaels
 * 
 */
public class UDPSender implements Runnable {
	private static UDPSender instance = null;
	private UDPManager udp = UDPManager.getInstance();

	private static long timeout = 20; // ms

	Map<UDPRequestHandler, ConcurrentSkipListSet<Integer>> acked = new ConcurrentHashMap<UDPRequestHandler, ConcurrentSkipListSet<Integer>>();
	Map<UDPRequestHandler, ConcurrentSkipListSet<Integer>> nacked = new ConcurrentHashMap<UDPRequestHandler, ConcurrentSkipListSet<Integer>>();
	Queue<UDPPacketSender> queue = new PriorityBlockingQueue<UDPPacketSender>();
	Queue<UDPPacketSender> resendQueue = new PriorityBlockingQueue<UDPPacketSender>();
	Map<UDPRequestHandler, Integer> clientsToBitrates = new ConcurrentHashMap<UDPRequestHandler, Integer>();

	/**
	 * Singleton class
	 * 
	 * @return the instance
	 */
	public static UDPSender getInstance() {
		if (instance == null) {
			instance = new UDPSender();
		}
		return instance;
	}

	private UDPSender() {
	}

	/**
	 * Attempts to send any extant packets.
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
			// Prioritize resends over new packets.
			if (!resendQueue.isEmpty() || !queue.isEmpty()) {
				while (!resendQueue.isEmpty() || !queue.isEmpty()) {
					if (!resendQueue.isEmpty()) {

						sender = resendQueue.remove();
						UDPRequestHandler requester = sender.getRequester();
						if (requester.canISend()) {

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

						if (requester.canISend()) {
							sender.send(udp);
						} else {
							queue.add(sender);
						}
					}

				}
			}
		}
	}

	/**
	 * Adds packets to our sendQueue.
	 */
	public void requestToSend(UDPRequestHandler request, int numPackets) {
		// Create ACK/NACK lists
		acked.put(request, new ConcurrentSkipListSet<Integer>());
		nacked.put(request, new ConcurrentSkipListSet<Integer>());

		// Queue up packet requests
		for (int i = request.getPhase(); i < numPackets; i += request
				.getPeriod()) {
			UDPPacketSender sender = new UDPPacketSender(request, i, timeout, 5);
			queue.add(sender);
		}
	}

	/**
	 * Allows a UDPPacketSender to add itself back to the queue for resending.
	 * If that packet was acked, then we don't resend it.
	 */
	public void requestResend(UDPPacketSender request) {
		UDPRequestHandler requester = request.getRequester();
		int seqNum = request.getSeqNum();

		ConcurrentSkipListSet<Integer> ackedSet = acked.get(requester);
		if (!ackedSet.contains(seqNum)) {

			// Recreate requester so we can reschedule its TimerTask.
			UDPPacketSender newRequest = new UDPPacketSender(requester, seqNum,
					timeout, request.getTTL() - 1);
			resendQueue.add(newRequest);
		}
	}

	/**
	 * Note that we successfully sent a packet and received the ACK.
	 * 
	 * @param requester
	 *            the UDPRequestHandler in charge.
	 * @param seqNum
	 *            the packet that was ACKed
	 */
	public void ackPacket(UDPRequestHandler requester, int seqNum) {
		ConcurrentSkipListSet<Integer> ackedSet = acked.get(requester);
		if (ackedSet != null)
			ackedSet.add(seqNum);
	}

	/**
	 * Handles a NACK from the peer to which we are sending data.
	 * 
	 * @param requester
	 *            the UDPRequestHandler in charge of the client
	 * @param seqNum
	 *            the packet that we need to resend
	 */
	public void nackPacket(UDPRequestHandler requester, int seqNum) {
		ConcurrentSkipListSet<Integer> nackedSet = nacked.get(requester);
		if (!nackedSet.contains(seqNum)) {
			UDPPacketSender newRequest = new UDPPacketSender(requester, seqNum,
					timeout, 5);
			nackedSet.add(seqNum);
			resendQueue.add(newRequest);
		}
	}

	/**
	 * Clears the ack/nack sets for a given requester.
	 * 
	 * @param requester
	 */
	public void clearRequester(UDPRequestHandler requester) {
		acked.get(requester).clear();
		nacked.get(requester).clear();
	}
}
