package edu.cmu.ece.routing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.google.gson.Gson;

import edu.cmu.ece.frontend.HTTPRequestHandler;
import edu.cmu.ece.routing.Neighbor.NeighborJSON;

public class NetworkGraph {
	private static NetworkGraph instance = null;

	// Keeps track of our direct neighbors over TCP
	private Map<UUID, Neighbor> neighbors = new ConcurrentHashMap<UUID, Neighbor>();

	// Keeps track of last sequence number from every node in the network
	private Map<UUID, Integer> seqNums = new ConcurrentHashMap<UUID, Integer>();
	int nextSeqNum = 0;

	// Keeps track of our entire network state graph
	private Map<UUID, Map<UUID, Integer>> adjacencies = new ConcurrentHashMap<UUID, Map<UUID, Integer>>();
	private Map<UUID, String> nameTable = new ConcurrentHashMap<UUID, String>();

	// Keeps track of files on each peer
	private Map<String, Set<UUID>> filesToNodes = new ConcurrentHashMap<String, Set<UUID>>();

	// Stuff for gossiping
	private int searchTTL = 15;
	private int searchInterval = 100; // ms
	private Map<String, Gossiper> activeGossipers = new ConcurrentHashMap<String, Gossiper>();

	// Stats for this node
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

	public int getNextSeqNum() {
		return nextSeqNum;
	}

	public void setUUID(UUID newUUID) {
		myUUID = newUUID;
		adjacencies.put(myUUID, new HashMap<UUID, Integer>());
	}

	public void setName(String newName) {
		myName = newName;
		nameTable.put(myUUID, myName);
	}

	public void setFrontendPort(int frontendPort) {
		this.frontendPort = frontendPort;
	}

	public void setBackendPort(int backendPort) {
		this.backendPort = backendPort;
	}

	public int getSearchTTL() {
		return searchTTL;
	}

	public void setSearchTTL(int searchTTL) {
		this.searchTTL = searchTTL;
	}

	public int getSearchInterval() {
		return searchInterval;
	}

	public void setSearchInterval(int searchInterval) {
		this.searchInterval = searchInterval;
	}

	public boolean hasGossiper(String file) {
		Gson gson = new Gson();
		System.out.println(gson.toJson(activeGossipers.keySet()));
		return activeGossipers.containsKey(file);
	}

	public void addGossiper(String file, Gossiper g) {
		activeGossipers.put(file, g);
	}

	public void removeGossiper(String file) {
		activeGossipers.remove(file);
	}

	public void incNextSeqNum() {
		this.nextSeqNum++;
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
		ArrayList<NeighborJSON> neighborMaps = new ArrayList<>();
		for (Neighbor neighbor : neighbors.values()) {
			// If this node is connected, add it
			if (neighbor.getDistanceMetric() >= 0) {
				neighborMaps.add(neighbor.getJSONClass());
			}
		}

		Gson gson = new Gson();

		return gson.toJson(neighborMaps);
	}

	/**
	 * Add a neighbor to our set of neighbors, and to our adjacencies
	 */
	public void addNeighbor(Neighbor n) {
		neighbors.put(n.getUuid(), n);
	}

	/*
	 * Get the last sequence number seen from a certain node
	 */
	public int lastSeqNum(UUID from) {
		Integer value = seqNums.get(from);
		if (value == null)
			return -1;
		else
			return value.intValue();
	}

	/*
	 * Set the last sequence number from a certain node
	 */
	public void setLastSeqNum(UUID from, int newSeqNum) {
		seqNums.put(from, newSeqNum);
	}

	/*
	 * Given a node and a peer it has as a neighbor, we need to update our
	 * adjacency graph. Replaces any node's neighbor with the new peer so as to
	 * make maintaining distances easy. Returns true if we have changed
	 */
	public boolean addAdjacency(UUID node, UUID edge, int distance) {
		Map<UUID, Integer> nodeMap;
		boolean changed = true; // Assume the value changed, check below

		if (adjacencies.containsKey(node)) {
			nodeMap = adjacencies.get(node);

			// Replace and determine whether the value changed
			Integer oldInt = nodeMap.put(edge, new Integer(distance));
			if (oldInt != null) {
				changed = (oldInt.intValue() != distance);
			}

		} else {
			nodeMap = new HashMap<UUID, Integer>();
			nodeMap.put(edge, distance);
			adjacencies.put(node, nodeMap);
		}

		return changed;
	}

	/*
	 * Returns a set of adjacent peers to a given node
	 */
	public Map<UUID, Integer> getAdjacencies(UUID node) {
		return adjacencies.get(node);
	}

	/*
	 * Removes a node from the network graph, and also clears any unreachable
	 * nodes.
	 */
	public void removeAdjacencyNode(UUID node) {
		adjacencies.remove(node);

		// Then traverse graph to find what we can't see
		Set<UUID> invisibleNodes = adjacencies.keySet();
		Set<UUID> visibleNodes = getShortestPaths().keySet();
		invisibleNodes.removeAll(visibleNodes);

		// Remove these from our graph
		for (UUID n : invisibleNodes)
			adjacencies.remove(n);
	}

	/*
	 * Update the name table
	 */
	public void addName(UUID node, String name) {
		nameTable.put(node, name);
	}

	public String getName(UUID node) {
		String name = nameTable.get(node);
		if (name == null)
			return node.toString();
		else
			return name;
	}

	public Map<UUID, String> getAllNames() {
		return nameTable;
	}

	/**
	 * Puts the serializable fields in a map, then uses gson to parse
	 * 
	 * @return a string containing JSONified fields.
	 */
	public String getNetworkMapJSONforWeb() {
		Map<String, Map<String, Integer>> networkMap = new HashMap<String, Map<String, Integer>>();

		// Loop over every node in the network. For each node create a map of
		// that nodes edges
		for (Map.Entry<UUID, Map<UUID, Integer>> node : adjacencies.entrySet()) {
			Map<String, Integer> edges = new HashMap<String, Integer>();
			networkMap.put(getName(node.getKey()), edges);

			// Loop over every edge for a node, add it to map, if its distance
			// is not infinity
			for (Map.Entry<UUID, Integer> edge : node.getValue().entrySet()) {
				if (edge.getValue().intValue() >= 0)
					edges.put(getName(edge.getKey()), edge.getValue()
							.intValue());
			}
		}

		// Convert to JSON string
		Gson gson = new Gson();
		return gson.toJson(networkMap);
	}

	/*
	 * Gets the shortest paths to every node from this server. Uses dijkstra's.
	 * Class GraphNode is simply a sortable pair of (UUID,cost,path).
	 */
	private class GraphNode implements Comparable<GraphNode> {
		public UUID uuid;
		public int cost;
		public List<UUID> path;

		public GraphNode(UUID uuid, int cost, List<UUID> path) {
			this.uuid = uuid;
			this.cost = cost;
			this.path = path;
		}

		@Override
		public int compareTo(GraphNode o) {
			return cost - o.cost;
		}

		public String toJSON() {
			return "{\"" + getName(uuid) + "\":" + cost + "}";
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
			if (edges == null)
				continue;
			for (Map.Entry<UUID, Integer> p : edges.entrySet()) {
				// Discard infinity
				if (p.getValue() < 0)
					continue;

				LinkedList<UUID> path = new LinkedList<UUID>();
				path.addAll(n.path);
				path.add(p.getKey());

				q.add(new GraphNode(p.getKey(), n.cost + p.getValue(), path));
			}
		}

		// Finally, return our results
		return paths;
	}

	public String getRank(String path) {
		Set<GraphPeer> peersWithFile = RoutingTable.getInstance()
				.getContentFromGraph(path);
		Map<UUID, CostPathPair> shortestPaths = getShortestPaths();
		List<String> result = new ArrayList<String>();

		if (peersWithFile == null) {
			return "[]";
		}

		for (GraphPeer peer : peersWithFile) {
			if (shortestPaths.containsKey(peer.getUuid())) {
				CostPathPair tmp = shortestPaths.get(peer.getUuid());
				result.add(new GraphNode(tmp.path.get(tmp.path.size() - 1),
						tmp.cost, tmp.path).toJSON());
			}
		}

		return result.toString();
	}

	
	public boolean checkFile(String file) {
		System.out.print(filesToNodes.keySet());
		return filesToNodes.containsKey(file);
	}
	
	public void addNodeForFile(String file, UUID node) {
		Set<UUID> nodes = filesToNodes.get(file);
		if (nodes == null) {
			nodes = new ConcurrentSkipListSet<UUID>();
			filesToNodes.put(file, nodes);
		}

		nodes.add(node);
	};

	public void addNodeSetForFile(String file, Set<UUID> nodeset) {
		Set<UUID> nodes = filesToNodes.get(file);
		if (nodes == null) {
			nodes = new ConcurrentSkipListSet<UUID>();
			filesToNodes.put(file, nodes);
		}

		nodes.addAll(nodeset);
	}

	public Set<UUID> getNodesWithFile(String file) {
		Set<UUID> result = filesToNodes.get(file);
		File f = new File(HTTPRequestHandler.getContentPath() + file);
		if (result == null) {
			result = new ConcurrentSkipListSet<UUID>();
		}
		if (f.exists()) {
			result.add(myUUID);
		}

		return result;
	}

	/*
	 * Given a file, consults our network graph to
	 */
	public String getSearchResults(String file) {
		String result = "[{\"content\":\"" + file + "\", \"peers\":";

		// Convert peer set to JSON
		Set<UUID> nodes = getNodesWithFile(file);
		Gson gson = new Gson();
		result += gson.toJson(nodes);

		result += "}]";

		return result;
	}
}
