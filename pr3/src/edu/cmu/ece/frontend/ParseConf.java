package edu.cmu.ece.frontend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.UUID;

import edu.cmu.ece.routing.Neighbor;
import edu.cmu.ece.routing.NetworkGraph;

/**
 * Class for parsing data from a node.conf file.
 * 
 * @author Michaels
 * 
 */
public class ParseConf {

	private UUID myUUID = null;
	private String name = null;
	private int frontendPort = 18345;
	private int backendPort = 18346;
	private ArrayList<NeighborContainer> neighborsToAdd = new ArrayList<NeighborContainer>();

	/**
	 * Constructor. Parses all data and sets private fields.
	 * 
	 * @param targetName
	 *            the filename that you are reading.
	 */
	public ParseConf(String targetName) {
		File conf = new File(targetName);

		// No such file exists
		if (!conf.exists()) {
			System.err.println("Specified config file does not exist.");
			System.exit(1);
		}

		// Read the file.
		BufferedReader confReader = null;
		try {
			confReader = new BufferedReader(new FileReader(conf));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String line;

		try {
			while ((line = confReader.readLine()) != null) {
				parseLine(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (myUUID != null) {
			if (name == null) {
				name = myUUID.toString();
			}

			NetworkGraph.getInstance().setUUID(myUUID);
			NetworkGraph.getInstance().setName(name);
			

		} else {
			myUUID = UUID.randomUUID();
			if (name == null) {
				System.out.print("NAME IS NULL");
				name = myUUID.toString();
			}

			NetworkGraph.getInstance().setUUID(myUUID);
			NetworkGraph.getInstance().setName(name);
			

			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new FileWriter(targetName, true)));
				out.println("uuid = " + myUUID);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		if (name != null) {
			NetworkGraph.getInstance().setName(name);
		}

		for (NeighborContainer nc : neighborsToAdd) {
			NetworkGraph.getInstance().addNeighbor(
					new Neighbor(nc.uuid, nc.hostname, nc.peerFrontPort,
							nc.peerBackPort, nc.peerMetric, false));
		}

	}

	/**
	 * Parse one line of the the file.
	 * 
	 * @param line
	 */
	private void parseLine(String line) {
		String[] kv = line.split(" ?= ?");

		if (kv.length != 2) {
			System.err.println("Malformed node.conf. Exiting.");
			System.exit(1);
		}

		String key = kv[0];
		String val = kv[1];

		// Set correct field based on key-value pair
		if (key.equals("uuid")) {
			myUUID = UUID.fromString(val);
			NetworkGraph.getInstance().setUUID(myUUID);
		} else if (key.equals("name")) {
			name = val;
		} else if (key.equals("frontend_port")) {
			frontendPort = Integer.parseInt(val);
		} else if (key.equals("backend_port")) {
			backendPort = Integer.parseInt(val);
		} else if (key.equals("content_dir")) {
			HTTPRequestHandler.setContentPath(val);
		} else if (key.matches("peer_\\d*")) {
			String[] peerInfo = val.split(",");
			if (peerInfo.length != 5) {
				System.err
						.println("Malformed peer info in node.conf. Exiting.");
				System.exit(1);
			}

			String uuid = peerInfo[0];
			String hostname = peerInfo[1];
			int peerFrontPort = Integer.parseInt(peerInfo[2]);
			int peerBackPort = Integer.parseInt(peerInfo[3]);
			int peerMetric = Integer.parseInt(peerInfo[4]);

			neighborsToAdd.add(new NeighborContainer(UUID.fromString(uuid),
					hostname, peerFrontPort, peerBackPort, peerMetric));

		}
	}

	private class NeighborContainer {
		public UUID uuid;
		public String hostname;
		public int peerFrontPort;
		public int peerBackPort;
		public int peerMetric;

		public NeighborContainer(UUID uuid, String hostname, int peerFrontPort,
				int peerBackPort, int peerMetric) {
			this.uuid = uuid;
			this.hostname = hostname;
			this.peerFrontPort = peerFrontPort;
			this.peerBackPort = peerBackPort;
			this.peerMetric = peerMetric;
		}

	}

	public int getBackendPort() {
		return backendPort;
	}

	public int getFrontendPort() {
		return frontendPort;
	}

}
