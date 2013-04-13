package edu.cmu.ece.routing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.cmu.ece.backend.PeerData;

public class Neighbor implements Comparable<Neighbor>, Runnable {
	private static int nextId = 1;
	private static int peerTimeout = 1000; // ms
	private static int connectTimeout = 10000; // ms

	private RoutingTable router = RoutingTable.getInstance();
	private NetworkGraph network = NetworkGraph.getInstance();

	private int id;
	private UUID uuid;
	private String name;
	private String host;
	private int frontendPort;
	private int backendPort;
	private int distance;
	private int originalDistance;


	private Socket connection;
	private BufferedReader in;
	private PrintWriter out;
	private Timer timer;

	// Provide a lock for the comms, so we can't interleave change messages
	// and keepalive messages
	public Object commLock = new Object();


	public Neighbor(UUID newUuid, String newHost, int newFrontendPort,
			int newBackendPort, int newMetric) {
		id = nextId++;

		uuid = newUuid;
		name = uuid.toString();
		frontendPort = newFrontendPort;
		backendPort = newBackendPort;
		distance = -1; // start off at infinity
		originalDistance = newMetric;

		// Add ourself to table
		network.addAjacency(network.getUUID(), uuid, -1);

		// Start main loop
		new Thread(this).start();
	}

	/*
	 * Send a request to the frontend over TCP to set up a TCP connection. Only
	 * the lesser UUID requests a connection
	 */
	public void requestPeering() {
		System.out.println("Requesting peering relationship with: " + uuid);

		boolean connected = false;
		while (!connected) {
			try {
				// Connect to remote neighbor through TCP
				connection = new Socket(InetAddress.getByName(host),
						frontendPort);
				in = new BufferedReader(new InputStreamReader(
						connection.getInputStream()));
				out = new PrintWriter(connection.getOutputStream());

				// Send them our request to peer
				String request = "GET peering_request/" + network.getUUID()
						+ " HTTP/1.1\r\n\r\n";
				out.print(request);
				out.flush();

				// Begin our long and beautiful relationship
				connected = true;
			} catch (UnknownHostException e) {
				System.err.println("Invalid neighbor address.");
			} catch (ConnectException e) {
				// Do nothing but retry to connect after a delay
				// TODO: Take it out of the list of neighbors?
				// network.removeNeighbor(uuid);
				System.out.println("Couldn't find. Waiting...");
				try {
					Thread.sleep(connectTimeout);
				} catch (InterruptedException e1) {
					// Do nothing
				}
				System.out.println("Trying again...");
			} catch (IOException e) {
				System.err
						.println("Could not read/write to socket stream for neighbors.");
				System.err.println(e);
			}
		}
	}

	/*
	 * Receives the port for our TCP connection via the UDP response.
	 */
	public void receivePeering(Socket socket, BufferedReader read,
			PrintWriter write) {
		System.out.println("\tEstablishing peering relationship.");
		connection = socket;
		in = read;
		out = write;

		// Begin our long and beautiful relationship
		new Thread(this).start();
	}

	/*
	 * Main run loop - listens over TCP for the neighbor to send us cool
	 * information. If we time out on a read block, we know this neighbor has
	 * died :(
	 */
	@Override
	public void run() {
		// Wait for connection to be set up if we are subordinate
		if (network.getUUID().compareTo(uuid) > 0 && connection == null)
			return;
		// Set up connection if we are superior
		else if (network.getUUID().compareTo(uuid) < 0)
			requestPeering();
		
		
		// Configure connection
		try {
			connection.setSoTimeout(peerTimeout);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		
		// Send our name
		synchronized (commLock) {
		System.out.println("\tPeer connection established.");
			out.write("Name " + network.getName());
			out.write("\r\n");
			out.flush();
		}

		// Start keep alive messages
		startKeepAlive();
		
		//Update our distance metric
		distance = originalDistance;
		network.addAjacency(network.getUUID(), uuid, distance);

		// Send initial changes
		sendSelfChange();

		int firstSeqNum = network.getNextSeqNum();
		network.incNextSeqNum();
		List<UUID> firstPath = new ArrayList<UUID>();
		firstPath.add(network.getUUID());
		sendChanges(firstSeqNum, firstPath, network.getAllNeighbors());
		sendNames(firstPath, network.getAllNames());


		// Listen until peer disconnects
		while (true) {
			try {
				// Wait for incoming message
				String message = in.readLine();

				// Parse the message from our peer
				// No updates is just a keep alive message sent periodically
				if (message == null) {
					break;
				} else if (message.equals("No updates")) {
					// Keep alive
					continue;
				} else if (message.startsWith("Name ")) {
					name = message.substring(5);
				} else if (message.startsWith("NameUpdate")) {
					// Prep JSON
					Gson gson = new Gson();
					Type pathType = new TypeToken<ArrayList<UUID>>() {
					}.getType();
					Type mapType = new TypeToken<HashMap<UUID, String>>() {
					}.getType();

					// Read in path
					String pathLine = in.readLine();
					List<UUID> path = gson.fromJson(pathLine, pathType);
					path.add(network.getUUID());

					// Read in update
					String namesLine = in.readLine();
					Map<UUID, String> updates = gson.fromJson(namesLine,
							mapType);
					
					// Check JSON parsing
					if (path == null || updates == null)
						throw new IOException("Invalid JSON.");
					
					
					// Push updates to table
					for (Map.Entry<UUID, String> e : updates.entrySet()) {
						network.addName(e.getKey(), e.getValue());
					}
					
					
					// Inform all our other neighbors
					for (Neighbor n : network.getNeighbors()) {
						n.sendNames(path, updates);
					}

				} else if (message.startsWith("Updates ")) {
					// Prep JSON
					Gson gson = new Gson();
					Type pathType = new TypeToken<ArrayList<UUID>>() {
					}.getType();
					Type mapType = new TypeToken<HashMap<UUID, HashMap<UUID, Integer>>>() {
					}.getType();


					// Parse seqNum from update header
					int seqNum = Integer.parseInt(message.split(" ")[1]);
					System.out
							.println("Neighbor has sent updates with seqNum: "
									+ seqNum);

					// Read in path
					String pathLine = in.readLine();
					List<UUID> path = gson.fromJson(pathLine, pathType);
					path.add(network.getUUID());

					// Read in map
					String mapLine = in.readLine();
					Map<UUID, Map<UUID, Integer>> updates = gson.fromJson(
							mapLine, mapType);

					// Check JSON parsing
					if (path == null || updates == null)
						throw new IOException("Invalid JSON.");


					// Ignore old sequence numbers. Just pull whole message to
					// toss it. The sequence number corresponds to the original
					// sender.
					int lastSeqNum = network.lastSeqNum(path.get(0));
					if (seqNum < lastSeqNum) {
						while (!in.readLine().equals(""))
							;
						continue;
					}
					network.setLastSeqNum(path.get(0), seqNum);


					// Push these updates into our table
					for (UUID uuid : updates.keySet()) {
						for (Map.Entry<UUID, Integer> peer : updates.get(uuid)
								.entrySet()) {
							network.addAjacency(uuid, peer.getKey(),
									peer.getValue());
						}
					}

					// Inform all our other neighbors
					for (Neighbor n : network.getNeighbors()) {
						n.sendChanges(seqNum, path, updates);
					}
				} else {
					// Invalid message
					System.err.println("Neighbor sent us invalid message: '"
							+ message + "'");
				}
			} catch (SocketTimeoutException e) {
				System.err
						.println("Neighbor hasn't reported back, may be dead.");
				timer.cancel();
				break;
				// TODO: update our graph - how do we do that?
				// TODO: try to reconnect periodically?

			} catch (IOException e) {
				// We can no longer send to this peer.
				System.err.println("Couldn't read incoming peer message: " + e);
				timer.cancel();
				break;
			}
		}


		// Close socket
		try {
			System.out.println("Disconnecting from neighbor.");
			connection.close();
			connection = null;
		} catch (IOException e) {
			System.err.println("Could not close socket to neighbor.");
		}

		// Reset distance stuff
		distance = -1;
		network.addAjacency(network.getUUID(), uuid, -1);
		network.removeAdjacencyNode(uuid);

		// Inform our neighbors we died
		sendSelfChange();


		// Retry
		run();
	}

	/*
	 * Sends a keep-alive periodically to our neighbor Send at 1/3 the timeout
	 * so we have to miss two in a row
	 */
	private class KeepAliveTimer extends TimerTask {
		public void run() {
			Neighbor.this.sendKeepAlive();
		}
	}

	private void startKeepAlive() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new KeepAliveTimer(), peerTimeout / 3,
				peerTimeout / 3);
	}

	private void sendKeepAlive() {
		// Send no updates message
		synchronized (commLock) {
			out.print("No updates\r\n");
			out.flush();
		}
	}

	/*
	 * Sends changes from this server over this neighbor connection. If this
	 * neighbor is in the path this packet traveled, we return instead.
	 */
	public void sendChanges(int seqNum, List<UUID> path,
			Map<UUID, Map<UUID, Integer>> changes) {
		// If this neighbor is in the path, discard it
		if (path.contains(uuid)) {
			return;
		}

		// Convert to JSON
		Gson gson = new Gson();
		String JSONpath = gson.toJson(path);
		String JSONchanges = gson.toJson(changes);

		// Write out
		if (out != null) {
			synchronized (commLock) {
				out.print("Updates " + seqNum + "\r\n");
				out.print(JSONpath + "\r\n");
				out.print(JSONchanges + "\r\n");
				out.flush();
			}
		}

		// TODO: reset timer? Since a change message tells us we are alive that
		// would be okay to do
	}

	/*
	 * Sends name changes from this server over this neighbor conncetion. If
	 * this neighor is in the path this packet traveled, return instead.
	 */
	public void sendNames(List<UUID> path, Map<UUID, String> changes) {
		// If this neighbor is in the path, discard it
		if (path.contains(uuid)) {
			return;
		}

		// Convert to JSON
		Gson gson = new Gson();
		String JSONpath = gson.toJson(path);
		String JSONchanges = gson.toJson(changes);

		// Write out
		if (out != null) {
			synchronized (commLock) {
				out.print("NameUpdates\r\n");
				out.print(JSONpath + "\r\n");
				out.print(JSONchanges + "\r\n");
				out.flush();
			}
		}
	}

	public void sendSelfChange() {
		int seqNum = network.getNextSeqNum();
		network.incNextSeqNum();

		List<UUID> myself = new ArrayList<UUID>();
		myself.add(network.getUUID());

		Map<UUID, Integer> oneChange = new HashMap<UUID, Integer>();
		oneChange.put(uuid, distance);
		Map<UUID, Map<UUID, Integer>> change = new HashMap<UUID, Map<UUID, Integer>>();
		change.put(network.getUUID(), oneChange);

		Collection<Neighbor> ns = network.getNeighbors();
		for (Neighbor n : ns) {
			if (!n.isAlive())
				continue;

			n.sendChanges(seqNum, myself, change);
		}
	}

	public UUID getUuid() {
		return uuid;
	}

	public int getDistanceMetric() {
		return distance;
	}

	public boolean isAlive() {
		return connection != null;
	}

	/*
	 * Returns the PeerData struct to set up a new request to this neighbor
	 */
	public PeerData getPeerData() {
		return new PeerData(host, frontendPort, backendPort, 0);
	}

	public Map<String, String> getJSONMap() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("uuid", uuid.toString());
		result.put("name", name);
		result.put("host", host);
		result.put("frontend", Integer.toString(frontendPort));
		result.put("backend", Integer.toString(backendPort));
		result.put("metric", Integer.toString(distance));
		return result;
	}

	@Override
	public int compareTo(Neighbor o) {
		return uuid.compareTo(o.uuid);
	}
}
