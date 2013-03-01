package edu.cmu.ece.backend;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ece.frontend.HTTPClientHandler;

public class RoutingTable {

	private static RoutingTable instance = null;
	private Map<String, PeerData> fileNamesToPeerData = new HashMap<String, PeerData>();
	private Map<Integer, HTTPClientHandler> idsToClientHandlers = new HashMap<Integer, HTTPClientHandler>();
	private int bitRate = 1000; // What's the default value here?

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
			System.out.println(path);
			return fileNamesToPeerData.put(path, ip);
		}
	}

	/**
	 * Checks if a clientID is in teh table.
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
	 * Removes a clientID from the table.
	 */
	public HTTPClientHandler getClientHandler(int clientID) {
		return idsToClientHandlers.get(new Integer(clientID));
	}

	/**
	 * Gets PeerData givena file path. 
	 */
	public PeerData getPeerData(String path) {
		return fileNamesToPeerData.get(path);
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
