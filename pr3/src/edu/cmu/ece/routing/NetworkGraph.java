package edu.cmu.ece.routing;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class NetworkGraph {
	private static NetworkGraph instance = null;
	private RoutingTable router = RoutingTable.getInstance();

	private Map<UUID, Neighbor> neighbors = new ConcurrentHashMap<UUID, Neighbor>();
	private Map<UUID, AbstractSet<Peer>> adjacencies = new ConcurrentHashMap<UUID, AbstractSet<Peer>>();
	
	/**
	 * Returns the instance of NetworkGraph.
	 * 
	 * @return the singleton instance.
	 */
	public static NetworkGraph getInstance() {
		if (instance == null)
			instance = new NetworkGraph();
		return instance;
	}

	/**
	 * Private constructor for a singleton.
	 */
	private NetworkGraph() {
		//Add ourself to the network graph
		adjacencies.put(router.getUUID(), new ConcurrentSkipListSet<Peer>());
	}

	/**
	 * Get a neighbor by UUID
	 */
	public Neighbor getNeighbor(UUID u) {
		return neighbors.get(u);
	}

	/**
	 * Get a Collection of all our neighbors
	 */
	public Collection<Neighbor> getNeighbors() {
		return neighbors.values();
	}

	public String getNeighborJSON() {
		// TODO: this whole function
		return "";
	}

	/**
	 * Add a neighbor to our set of neighbors, and to our adjacencies
	 */
	public void addNeighbor(Neighbor n) {
		neighbors.put(n.getUuid(), n);

		// Also add neighbor to network graph
		Peer p = new Peer(n.getUuid(), n.getDistanceMetric());
		adjacencies.get(router.getUUID()).add(p);
	}


	/*
	 * Given a node and a peer it has as a neighbor, we need to update our
	 * adjaceny graph. Replaces any node's neighbor with the new peer so as to
	 * make maintaining distances easy. Returns true if we have replaced
	 * something.
	 */
	public boolean addAjacency(UUID node, Peer peer) {
		AbstractSet<Peer> nodeSet;
		boolean replaced;

		if (adjacencies.containsKey(node)) {
			nodeSet = adjacencies.get(node);
			replaced = nodeSet.contains(peer);
		} else {
			nodeSet = new ConcurrentSkipListSet<Peer>();
			adjacencies.put(node, nodeSet);
			replaced = false;
		}

		// Add and return
		nodeSet.add(peer);
		return replaced;
	}

	/*
	 * Returns a set of adjacent peers to a given node
	 */
	public AbstractSet<Peer> getAdjacenies(UUID node) {
		return adjacencies.get(node);
	}
}
