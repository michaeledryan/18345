package edu.cmu.ece.backend;

import java.net.DatagramPacket;

import edu.cmu.ece.frontend.HTTPClientHandler;
import edu.cmu.ece.packet.UDPPacket;

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

		switch (packet.getType()) {
		case REQUEST:
			UDPRequestHandler handler = new UDPRequestHandler(packet);
			handler.determineRequest();
			// Trigger a UDPRequestHandler to find the file and send it out
			return;

		case DATA:
			// Get the client that requested the packet and give him the data
			// to respond over TCP
			HTTPClientHandler client = router
					.getClientHandler(packet.getClientID());
			byte[] packetData = packet.getData();
			if (client == null){
				System.out.println("client ID not found.");
			}
			client.mirrorPacketToClient(packetData, packetData.length);
			return;

		case CONFIG:
			// TODO: I have no idea. For now, fall through to default

		default:
			
			// Do nothing - ignore invalid requests
			return;
		}
	}

}
