package edu.cmu.ece.routing;

import java.util.Collection;
import java.util.Random;
import java.util.Timer;

public class Gossiper implements Runnable {
	private String file;
	private int ttl;
	private int interval; // ms

	private NetworkGraph network = NetworkGraph.getInstance();

	public Gossiper(String file) {
		this.file = file;
		this.ttl = network.getSearchTTL();
		this.interval = network.getSearchInterval();

		// Add ourself to the table
		network.addGossiper(file, this);

		// Start running rounds
		new Thread(this).start();
	}

	@Override
	public void run() {
		Timer timer = new Timer();
		Random rand = new Random(System.currentTimeMillis());

		// While TTL is nonzero, run rounds
		while (ttl > 0) {
			// Get all neighbors, choose one at random
			Collection<Neighbor> nc = network.getNeighbors();
			Neighbor[] ns = nc.toArray(new Neighbor[nc.size()]);
			Neighbor n = ns[rand.nextInt(nc.size())];

			// Tell that neighbor to gossip with this file
			n.sendGossipRequest(file, ttl);

			// Decrement TTL and wait for next round
			ttl--;
			try {
				timer.wait(interval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Remove ourselves from the table
		network.removeGossiper(file);
	}
}
