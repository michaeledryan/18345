package edu.cmu.ece.backend;

import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.util.UUID;

import edu.cmu.ece.frontend.HTTPClientHandler;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;
import edu.cmu.ece.routing.Neighbor;
import edu.cmu.ece.routing.RoutingTable;

/**
 * Given a UDPPacket, determines the type and responds accordingly.
 * 
 * @author michaels
 * 
 */
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
		// System.out.println("UDP packet with client " + packet.getClientID()
		// + " and request " + packet.getRequestID());
		PeerData pd = new PeerData(packet.getRemoteIP(),
				packet.getRemotePort(), packet.getClientID(),
				packet.getRequestID());

		switch (packet.getType()) {
		case REQUEST:
			if (router.getRequest(pd) != null) {
				router.getRequest(pd).kill();
			}
			System.out.println("Request for content over UDP.");
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
			// System.out.println("\tGot UDP ACK " +
			// packet.getSequenceNumber());
			if (router.getRequest(pd) != null) {
				UDPRequestHandler request = router.getRequest(pd);
				sender.ackPacket(request, packet.getSequenceNumber());
			}
			return;
			// We got the packet, so let's send another. We still have to
			// implement timeouts.

		case NAK:
			// Request that a packet be resent.
			// System.out
			// .println("\tNAK for seqNum " + packet.getSequenceNumber());
			if (router.getRequest(pd) != null) {
				UDPRequestHandler request = router.getRequest(pd);
				sender.nackPacket(request, packet.getSequenceNumber());
			}
			return;

		case KILL:
			// Tells the server to stop handling a request
			System.out.println("Got kill request from client "
					+ packet.getClientID());
			if (router.getRequest(pd) != null) {
				router.getRequest(pd).kill();
			}
			return;

		case END:
		case DATA:
			// Get the client that requested the packet and give him the data
			// to respond over TCP
			// System.out
			// .println("\tResponse content received over UDP with seqNum = "
			// + packet.getSequenceNumber());
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

		case PEERING_REQUEST:
			String requestUuid = new String(packet.getData());
			Neighbor requestN = router
					.getNeighbor(UUID.fromString(requestUuid));

			String response = router.getUUID() + "\n" + requestN.getInPort();
			try {
				UDPPacket packet = new UDPPacket(0, 0, pd.getIP(),
						pd.getPort(), response.getBytes(),
						UDPPacketType.PEERING_RESPONSE, 0);
				udp.sendPacket(packet.getPacket());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return;

		case PEERING_RESPONSE:
			String peeringResponse = new String(packet.getData());
			String responseUuid = peeringResponse.split("\n")[0];
			int port = Integer.parseInt(peeringResponse.split("\n")[1]);

			Neighbor responseN = router.getNeighbor(UUID
					.fromString(responseUuid));
			responseN.receivePeering(port);
			return;

		case CONFIG:
			// TODO: For now, fall through to default
			// System.out.println("\tUDP config request");

		default:
			// Do nothing - ignore invalid requests
			return;
		}
	}
}
