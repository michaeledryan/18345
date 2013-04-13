package edu.cmu.ece.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class NetworkGraph {
	private static NetworkGraph instance = null;
	private RoutingTable router = RoutingTable.getInstance();

	private Map<UUID, Neighbor> neighbors = new ConcurrentHashMap<UUID, Neighbor>();
	private Map<UUID, Map<UUID, Integer>> adjacencies = new ConcurrentHashMap<UUID, Map<UUID, Integer>>();

	// Represents the highest sequence number we have seen so far
	private int sequenceNumber = 0;

	private UUID myUUID;
	private String myName;
	private int frontendPort;
	private int backendPort;

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
		// Add ourself to the network graph
		// adjacencies.put(myUUID, new ConcurrentSkipListSet<Peer>());
	}

	/**
	 * Get/set this server's properties
	 */
	public UUID getUUID() {
		return myUUID;
	}

	public int getFrontendPort() {
		return frontendPort;
	}

	public int getBackendPort() {
		return backendPort;
	}

	public String getName() {
		return myName;
	}

	public void setUUID(UUID newUUID) {
		myUUID = newUUID;
		adjacencies.put(myUUID, new HashMap<UUID, Integer>());
	}

	public void setName(String newName) {
		myName = newName;
	}

	public void setFrontendPort(int frontendPort) {
		this.frontendPort = frontendPort;
	}

	public void setBackendPort(int backendPort) {
		this.backendPort = backendPort;
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
	 * Remove a neighbor by UUID
	 */
	public Neighbor removeNeighbor(UUID u) {
		return neighbors.remove(u);
	}

	/**
	 * Get a Collection of all our neighbors
	 */
	public Collection<Neighbor> getNeighbors() {
		return neighbors.values();
	}

	/**
	 * Get all neighbors
	 */
	public Map<UUID, Map<UUID, Integer>> getAllNeighbors() {
		return adjacencies;
	}

	/**
	 * Puts the serializable fields in a map, then uses gson to parse
	 * 
	 * @return a string containing JSONified fields.
	 */
	public String getNeighborJSONforWeb() {
		ArrayList<Map<String, String>> neighborMaps = new ArrayList<>();
		for (Neighbor neighbor : neighbors.values()) {
			neighborMaps.add(neighbor.getJSONMap());
		}

		Gson gson = new Gson();

		return gson.toJson(neighborMaps);
	}

	/**
	 * Add a neighbor to our set of neighbors, and to our adjacencies
	 */
	public void addNeighbor(Neighbor n) {
		neighbors.put(n.getUuid(), n);

		// Also add neighbor to network graph
		adjacencies.get(myUUID).put(n.getUuid(), n.getDistanceMetric());
	}

	/*
	 * Given a node and a peer it has as a neighbor, we need to update our
	 * adjacency graph. Replaces any node's neighbor with the new peer so as to
	 * make maintaining distances easy. Returns true if we have changed
	 */
	public boolean addAjacency(UUID node, UUID edge, int distance) {
		Map<UUID, Integer> nodeMap;
		boolean changes = true;

		if (adjacencies.containsKey(node)) {
			nodeMap = adjacencies.get(node);

			Integer oldInt = nodeMap.put(edge, new Integer(distance));
			int old;
			if (oldInt == null) {
				old = -1; // TODO: Is this correct? Ones that previously did not
							// exist had an infinite distance
			} else {
				old = oldInt.intValue();
			}

			changes = (old != distance);
		} else {
			nodeMap = new HashMap<UUID, Integer>();
			nodeMap.put(edge, distance);
			adjacencies.put(node, nodeMap);
		}

		return true;
	}

	/*
	 * Returns a set of adjacent peers to a given node
	 */
	public Map<UUID, Integer> getAdjacencies(UUID node) {
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
		q.add(new GraphNode(myUUID, 0, new LinkedList<UUID>()));

		while (!q.isEmpty()) {
			// Get a node, discard it if we saw it
			GraphNode n = q.poll();
			if (paths.containsKey(n.uuid))
				continue;

			// Add it to results
			paths.put(n.uuid, new CostPathPair(n.cost, n.path));

			// Follow every edge and add to queue
			Map<UUID, Integer> edges = getAdjacencies(n.uuid);
			for (Map.Entry<UUID, Integer> p : edges.entrySet()) {
				LinkedList<UUID> path = new LinkedList<UUID>();
				Collections.copy(path, n.path);
				path.add(p.getKey());

				q.add(new GraphNode(p.getKey(), n.cost + p.getValue(), path));
			}
		}

		// Finally, return our results
		return paths;
	}
}
