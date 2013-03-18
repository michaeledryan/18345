package edu.cmu.ece.backend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.cmu.ece.frontend.HTTPClientHandler;

public class RoutingTable {

	private static RoutingTable instance = null;
	private Map<String, PeerData> fileNamesToPeerData = new ConcurrentHashMap<String, PeerData>();
	private Map<Integer, HTTPClientHandler> idsToClientHandlers = new ConcurrentHashMap<Integer, HTTPClientHandler>();
	private int bitRate = 1000; // What's the default value here?
	private Map<PeerData, UDPRequestHandler> peersToRequests = new ConcurrentHashMap<PeerData, UDPRequestHandler>();

	/**
	 * Returns the instance of RoutingTable.  
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
	 * Adds to the map of PeerDatas to UDPRequestHandlers 
	 * 
	 */
	public UDPRequestHandler addToRequests(PeerData pd, UDPRequestHandler handler) {
		synchronized (peersToRequests) {
			return peersToRequests.put(pd, handler);
		}
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
	 * Adds to the map of files to PeerData
	 * 
	 */
	public PeerData addtofileNames(String path, PeerData ip) {
		synchronized (fileNamesToPeerData) {
			return fileNamesToPeerData.put(path, ip);
		}
	}

	/**
	 * Checks if a given PeerData is in the table. 
	 * 
	 */
	public boolean CheckPD(PeerData pd) {
		synchronized (peersToRequests) {
			return peersToRequests.containsKey(pd);
		}
	}
	
	/**
	 * Checks if a clientID is in the table.
	 */
	public boolean checkId(int clientId) {
		return idsToClientHandlers.containsKey(clientId);
	}

	/**
	 * Checks if a given path is in the table.
	 */
	public boolean checkPath(String path) {
		return fileNamesToPeerData.containsKey(path);
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
	 *	Removes an ID from the table. 
	 */
	public HTTPClientHandler removeId(int clientID) {
		synchronized (idsToClientHandlers) {
			return idsToClientHandlers.remove(new Integer(clientID));
		}
	}

	/**
	 * Removes a path from the table. Probably never used.
	 */
	public PeerData removeFile(String path) {
		synchronized (fileNamesToPeerData) {
			return fileNamesToPeerData.remove(path);
		}
	}

	/**
	 * Gets a ClientHandler given a clientID
	 */
	public HTTPClientHandler getClientHandler(int clientID) {
		return idsToClientHandlers.get(new Integer(clientID));
	}

	/**
	 * Gets PeerData given file path. 
	 */
	public PeerData getPeerData(String path) {
		return fileNamesToPeerData.get(path);
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
	 * Gets the current bit rate in bits/second.
	 */
	public int getBitRate() {
		return bitRate;
	}

	/**
	 * Sets the bitRate.
	 */
	public void setBitRate(int rate) {
		bitRate = rate;
	}

}
