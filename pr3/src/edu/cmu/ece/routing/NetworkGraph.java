package edu.cmu.ece.routing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class NetworkGraph {
	private static NetworkGraph instance = null;
	private RoutingTable router = RoutingTable.getInstance();

	private Map<UUID, Neighbor> neighbors = new ConcurrentHashMap<UUID, Neighbor>();
	private Map<UUID, Set<Peer>> adjacencies = new ConcurrentHashMap<UUID, Set<Peer>>();
	
	// Represents the highest sequence number we have seen so far
	private int sequenceNumber = 0;

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
	 * Get and set sequence number
	 */
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
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
	 * adjacency graph. Replaces any node's neighbor with the new peer so as to
	 * make maintaining distances easy. Returns true if we have replaced
	 * something.
	 */
	public boolean addAjacency(UUID node, Peer peer) {
		Set<Peer> nodeSet;
		boolean replaced = false;
		;

		if (adjacencies.containsKey(node)) {
			nodeSet = adjacencies.get(node);

			// If this peer is already in the graph, remove it so we can read it
			// with the new distance metric
			if (nodeSet.contains(peer)) {
				nodeSet.remove(peer);
				replaced = true;
			}

			nodeSet.add(peer);
		} else {
			nodeSet = new ConcurrentSkipListSet<Peer>();
			nodeSet.add(peer);
			adjacencies.put(node, nodeSet);
		}

		return replaced;
	}

	/*
	 * Returns a set of adjacent peers to a given node
	 */
	public Set<Peer> getAdjacencies(UUID node) {
		return adjacencies.get(node);
	}


	/*
	 * Gets the shortest paths to every node from this server. Uses dijkstra's.
	 * Class GraphNode is simply a sortable pair of (UUID,cost,path).
	 */
	private class GraphNode implements Comparable<GraphNode> {
		public UUID uuid;
		public int cost;
		public LinkedList<UUID> path;

		public GraphNode(UUID uuid, int cost, LinkedList<UUID> path) {
			this.uuid = uuid;
			this.cost = cost;
			this.path = path;
		}

		@Override
		public int compareTo(GraphNode o) {
			return cost - o.cost;
		}
	}

	/*
	 * Really just a pair, but Java makes me define a whole class. Thanks.
	 */
	public class CostPathPair {
		public int cost;
		public List<UUID> path;

		public CostPathPair(int cost, List<UUID> path) {
			this.cost = cost;
			this.path = path;
		}
	}

	public Map<UUID, CostPathPair> getShortestPaths() {
		// Create visited set
		Map<UUID, CostPathPair> paths = new HashMap<UUID, CostPathPair>();

		// Queue for graph traversal - in cost order
		PriorityQueue<GraphNode> q = new PriorityQueue<GraphNode>();
		q.add(new GraphNode(router.getUUID(), 0, new LinkedList<UUID>()));

		while (!q.isEmpty()) {
			// Get a node, discard it if we saw it
			GraphNode n = q.poll();
			if (paths.containsKey(n.uuid))
				continue;

			// Add it to results
			paths.put(n.uuid, new CostPathPair(n.cost, n.path));

			// Follow every edge and add to queue
			Set<Peer> edges = getAdjacencies(n.uuid);
			for (Peer p : edges) {
				LinkedList<UUID> path = new LinkedList<UUID>();
				Collections.copy(path, n.path);
				path.add(p.getUuid());

				q.add(new GraphNode(p.getUuid(),
						n.cost + p.getDistanceMetric(), path));
			}
		}

		// Finally, return our results
		return paths;
	}
}
