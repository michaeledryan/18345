package edu.cmu.ece.routing;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Random;

import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.HTTPResponses;

public class Gossiper implements Runnable {
	private String file;
	private int ttl;
	private int interval; // ms
	private PrintWriter out;
	private HTTPRequestPacket request;

	private NetworkGraph network = NetworkGraph.getInstance();

	public Gossiper(String file, int ttl, HTTPRequestPacket request,
			PrintWriter out) {

		this.file = file;
		this.ttl = ttl;
		this.interval = network.getSearchInterval();
		this.out = out;
		this.request = request;
		System.out.println("Gossiper for: " + file);

		// Add ourself to the table
		network.addGossiper(file, this);

		// Start running rounds
		new Thread(this).start();
	}

	@Override
	public void run() {
		Random rand = new Random(System.currentTimeMillis());

		// While TTL is nonzero, run rounds
		while (ttl > 0) {
			// Get all neighbors, choose one at random
			Collection<Neighbor> nc = network.getNeighbors();
			if (nc.size() == 0) {
				break;
			}
			Neighbor[] ns = nc.toArray(new Neighbor[nc.size()]);
			Neighbor n = ns[rand.nextInt(nc.size())];

			// Tell that neighbor to gossip with this file
			n.sendGossipRequest(file, --ttl);

			// Decrement TTL and wait for next round
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		while (--ttl < -10) {

		}

		// Remove ourselves from the table
		if (request != null && out != null) {
			HTTPResponses.sendSearchResponse(file, request, out);
		}
		network.removeGossiper(file);
	}
}
