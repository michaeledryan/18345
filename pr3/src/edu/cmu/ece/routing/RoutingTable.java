package edu.cmu.ece.routing;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import edu.cmu.ece.backend.PeerData;
import edu.cmu.ece.backend.UDPRequestHandler;
import edu.cmu.ece.frontend.HTTPClientHandler;

public class RoutingTable {

	private static RoutingTable instance = null;
	private Map<String, ConcurrentSkipListSet<PeerData>> fileNamesToPeerData = new ConcurrentHashMap<String, ConcurrentSkipListSet<PeerData>>();
	private Map<Integer, HTTPClientHandler> idsToClientHandlers = new ConcurrentHashMap<Integer, HTTPClientHandler>();
	private Map<PeerData, UDPRequestHandler> peersToRequests = new ConcurrentHashMap<PeerData, UDPRequestHandler>();
	private Map<String, Integer> clientsToBitRates = new ConcurrentHashMap<String, Integer>();
	
	private Map<UUID, Neighbor> neighbors = new ConcurrentHashMap<UUID, Neighbor>();
	
	private UUID myUuid;
	private String myName;
	private int host;
	private int frontendPort;
	private int backendPort;
	
	/**
	 * Returns the instance of RoutingTable.
	 * 
	 * @return the singleton instance.
	 */
	public static RoutingTable getInstance() {
		if (instance == null)
			instance = new RoutingTable();
		return instance;
	}

	/**
	 * Private constructor for a singleton.
	 */
	private RoutingTable() {
	}


	/**
	 * Get/set this server's properties
	 */
	public UUID getUUID() {
		return myUuid;
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
		myUuid = newUUID;
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
	 * Checks if a given PeerData is in the table.
	 */
	public boolean CheckPD(PeerData pd) {
		synchronized (peersToRequests) {
			return peersToRequests.containsKey(pd);
		}
	}

	/**
	 * Gets a Request given a PeerData
	 * 
	 */
	public UDPRequestHandler getRequest(PeerData pd) {
		synchronized (peersToRequests) {
			return peersToRequests.get(pd);
		}
	}

	/**
	 * Adds to the map of PeerDatas to UDPRequestHandlers
	 * 
	 */
	public UDPRequestHandler addToRequests(PeerData pd,
			UDPRequestHandler handler) {
		synchronized (peersToRequests) {
			return peersToRequests.put(pd, handler);
		}
	}

	/**
	 * Removes a PeerData from peersToRequests
	 * 
	 */
	public UDPRequestHandler removeRequest(PeerData pd) {
		synchronized (peersToRequests) {
			return peersToRequests.remove(pd);
		}
	}

	/**
	 * Checks if a clientID is in the table.
	 */
	public boolean checkId(int clientId) {
		return idsToClientHandlers.containsKey(clientId);
	}

	/**
	 * Gets a ClientHandler given a clientID
	 */
	public HTTPClientHandler getClientHandler(int clientID) {
		return idsToClientHandlers.get(new Integer(clientID));
	}

	/**
	 * Adds to the map of clientIDs to HTTPClientHandlers
	 * 
	 */
	public HTTPClientHandler addtoIds(int clientId, HTTPClientHandler handler) {
		synchronized (idsToClientHandlers) {
			return idsToClientHandlers.put(new Integer(clientId), handler);
		}
	}

	/**
	 * Removes an ID from the table.
	 */
	public HTTPClientHandler removeId(int clientID) {
		synchronized (idsToClientHandlers) {
			return idsToClientHandlers.remove(new Integer(clientID));
		}
	}

	/**
	 * Checks if a given path is in the table.
	 */
	public boolean checkPath(String path) {
		return fileNamesToPeerData.containsKey(path);
	}

	/**
	 * Gets PeerData given file path.
	 */
	public ConcurrentSkipListSet<PeerData> getPeerData(String path) {
		return fileNamesToPeerData.get(path);
	}

	/**
	 * Adds to the map of files to PeerData
	 * 
	 */
	public PeerData addtofileNames(String path, PeerData ip) {
		synchronized (fileNamesToPeerData) {
			ConcurrentSkipListSet<PeerData> peers;
			if (fileNamesToPeerData.containsKey(path)) {
				peers = fileNamesToPeerData.get(path);
			} else {
				peers = new ConcurrentSkipListSet<PeerData>();
				fileNamesToPeerData.put(path, peers);
			}
			peers.add(ip);

			return ip;
		}
	}

	/**
	 * Removes a path from the table. Probably never used.
	 */
	public void removeFile(String path) {
		synchronized (fileNamesToPeerData) {
			fileNamesToPeerData.remove(path);
		}
	}


	/**
	 * Gets the current bit rate in bits/second for a given client.
	 */
	public int getClientBitRate(String clientIP) {
		return clientsToBitRates.get(clientIP);
	}

	/**
	 * Sets the bitRate.
	 */
	public void setBitRate(String clientIP, int rate) {
		clientsToBitRates.put(clientIP, rate);
	}

	public boolean bitRateSet(String clientIP) {
		return clientsToBitRates.containsKey(clientIP);
	}

	/**
	 * Removes the bitRate.
	 */
	public void removeBitRate(int clientID) {
		clientsToBitRates.remove(new Integer(clientID));
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

	/**
	 * Add a neighbor to our set of neighbors
	 */
	public void addNeighbor(Neighbor n) {
		neighbors.put(n.getUuid(), n);
	}


}
