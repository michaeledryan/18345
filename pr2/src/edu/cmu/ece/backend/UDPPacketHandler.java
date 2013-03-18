package edu.cmu.ece.backend;

import java.net.DatagramPacket;
import java.net.UnknownHostException;

import edu.cmu.ece.frontend.HTTPClientHandler;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;

public class UDPPacketHandler implements Runnable {

	private static RoutingTable router = RoutingTable.getInstance();
	private UDPPacket packet;

	public UDPPacketHandler(DatagramPacket incoming) {
		packet = new UDPPacket(incoming);
	}

	@Override
	public void run() {

		/*
		 * TODO: parse packet for request type If it is a response packet, then
		 * look up the clientID in the routing table, and call
		 * mirrorPacketToClient on the data.
		 * 
		 * If it is a request packet, create a new UDPRequestHandler to send the
		 * data back with the file path in question back to the source client
		 */

		System.out.println("Packet Handler started.");

		PeerData pd = new PeerData(packet.getRemoteIP(),
				packet.getRemotePort(), 0);
		switch (packet.getType()) {
		case REQUEST:
			UDPRequestHandler handler = new UDPRequestHandler(packet);
			handler.determineRequest();
			router.addToRequests(pd, handler);
			// Trigger a UDPRequestHandler to find the file and send it out.
			// MAke the handler visible so that we can get it later for ACKs
			return;

		case ACK:
			if (router.getRequest(pd) != null) {
				System.out.println("Got ACK");
				router.getRequest(pd).sendNext(packet.getSequenceNumber());
			}
			return;
			// We got the packet, so let's send another. We still have to
			// implement timeouts.
		case NAK:
			pd = new PeerData(packet.getRemoteIP(), packet.getRemotePort(), 0);
			if (router.getRequest(pd) != null) {
				router.getRequest(pd).resend();
			}
			return;
			// Send again? This is just stop-and-wait here... Not sure how to do
			// timeouts.

		case END:
		case DATA:
			// Get the client that requested the packet and give him the data
			// to respond over TCP

			HTTPClientHandler client = router.getClientHandler(packet
					.getClientID());
			if (client == null) {
				System.out.println("client ID not found.");
			}

			// Add to the HTTPClientHandler's queue.
			client.addToQueue(packet);
			try {
				System.err.println("Sending ACK");
				UDPManager.getInstance().sendPacket(
						new UDPPacket(client.getClientID(), packet
								.getRemoteIP(), packet.getRemotePort(),
								new byte[0], UDPPacketType.ACK, packet
										.getSequenceNumber() + 1).getPacket());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;

		case CONFIG:
			// TODO: I have no idea. For now, fall through to default

		default:
			// Do nothing - ignore invalid requests
			return;
		}
	}
}
