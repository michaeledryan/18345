package edu.cmu.ece.backend;

import java.net.DatagramPacket;
import java.net.UnknownHostException;

import edu.cmu.ece.frontend.HTTPClientHandler;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;

public class UDPPacketHandler implements Runnable {
	private static RoutingTable router = RoutingTable.getInstance();
	private static UDPSender sender = UDPSender.getInstance();
	private static UDPManager udp = UDPManager.getInstance();
	private UDPPacket packet;

	public UDPPacketHandler(DatagramPacket incoming) {
		packet = new UDPPacket(incoming);
	}

	@Override
	public void run() {
		// Get peer data to look up in routing table
	//	System.out.println("UDP packet with client " + packet.getClientID()
	//			+ " and request " + packet.getRequestID());
		PeerData pd = new PeerData(packet.getRemoteIP(),
				packet.getRemotePort(), packet.getClientID(),
				packet.getRequestID());

		switch (packet.getType()) {
		case REQUEST:
			if (router.getRequest(pd) != null) {
				router.getRequest(pd).kill();
			}
	//		System.out.println("\tRequest for content over UDP.");
			UDPRequestHandler handler = new UDPRequestHandler(packet);
			int numPackets = handler.initializeRequest();
			router.addToRequests(
					new PeerData(packet.getRemoteIP(), packet.getRemotePort(),
							packet.getClientID(), handler.getID()), handler);
			sender.requestToSend(handler, numPackets);
			return;
			// Trigger a UDPRequestHandler to find the file and send it out.
			// Add the send request to our sending manager
			// MAke the handler visible so that we can get it later for ACKs

		case ACK:
		//	System.out.println("\tGot UDP ACK " + packet.getSequenceNumber());
			if (router.getRequest(pd) != null) {
				UDPRequestHandler request = router.getRequest(pd);
				sender.ackPacket(request, packet.getSequenceNumber());
			} else {
				System.out.println("\tRequester ACK" + pd.getRequest() + " is NULL?");
			}
			return;
			// We got the packet, so let's send another. We still have to
			// implement timeouts.

		case NAK:
//			System.out
	//				.println("\tNAK for seqNum " + packet.getSequenceNumber());
			if (router.getRequest(pd) != null) {
				UDPRequestHandler request = router.getRequest(pd);
				sender.nackPacket(request, packet.getSequenceNumber());
			} else {
				System.out.println("\tRequester NAK is NULL?");
			}
			return;
			// Currently there is no support for NAKs

		case KILL:
		//	System.out.println("\tGot kill request from client "
			//		+ packet.getClientID());
			if (router.getRequest(pd) != null) {
				router.getRequest(pd).kill();
			} else {
				System.out.println("\tRequester KILL is NULL?");
			}
			return;
			// Kill a request if the client hung up

		case END:
		case DATA:
			// Get the client that requested the packet and give him the data
			// to respond over TCP
			//System.out
			//		.println("\tResponse content received over UDP with seqNum = "
		//					+ packet.getSequenceNumber());
			HTTPClientHandler client = router.getClientHandler(packet
					.getClientID());
			if (client == null) {
				System.err.println("Client ID not found.");
				return;
			}

			// Add to the HTTPClientHandler's queue.
			client.addToQueue(packet);

			// Ack this packet
			try {
				udp.sendPacket(new UDPPacket(client.getClientID(), packet
						.getRequestID(), packet.getRemoteIP(), packet
						.getRemotePort(), new byte[0], UDPPacketType.ACK,
						packet.getSequenceNumber()).getPacket());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			return;

		case CONFIG:
			// TODO: I have no idea. For now, fall through to default
			//System.out.println("\tUDP config request");

		default:
			// Do nothing - ignore invalid requests
			return;
		}
	}
}
