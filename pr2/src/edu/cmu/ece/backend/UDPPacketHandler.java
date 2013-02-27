package edu.cmu.ece.backend;

import java.net.DatagramPacket;

public class UDPPacketHandler implements Runnable {
	@SuppressWarnings("unused")
	private UDPManager udp;
	private DatagramPacket packet;

	public UDPPacketHandler(DatagramPacket incoming, UDPManager udp_man) {
		udp = udp_man;
		packet = incoming;
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
	}

}
