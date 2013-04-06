package edu.cmu.ece.routing;

import java.util.Timer;
import java.util.TimerTask;

import edu.cmu.ece.packet.UDPPacket;

public class NeighborPeeringRequest extends TimerTask {
	private Neighbor neighbor;
	private UDPPacket packet;


	public NeighborPeeringRequest(Neighbor neighbor_in, UDPPacket packet_in,
			long timeout) {
		neighbor = neighbor_in;
		packet = packet_in;

		new Timer().schedule(this, timeout);
	}


	/**
	 * Tell the Neighbor to renegotiate the connection
	 */
	@Override
	public void run() {
		neighbor.requestPeering(packet);
	}
}
