package edu.cmu.ece.backend;

import java.net.DatagramPacket;

import edu.cmu.ece.packet.UDPPacket;

public class UDPPacketHandler implements Runnable {

	private static UDPManager udp = UDPManager.getInstance();
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

		switch (packet.getType()) {
		case REQUEST:
			// Trigger a UDPRequestHandler to find the file and send it out
			return;
		case DATA:
			// Read client directory to get clienthandler, then pass data to
			// that client
			return;
		default:
			// Do nothing
			return;
		}
	}

}
