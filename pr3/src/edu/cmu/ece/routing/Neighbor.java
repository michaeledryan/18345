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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.cmu.ece.backend.PeerData;

public class Neighbor implements Comparable<Neighbor>, Runnable {
	private static int nextId = 1;
	private static int peerTimeout = 1000; // ms
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


	public Neighbor(UUID newUuid, String newHost, int newFrontendPort,
			int newBackendPort, int newMetric) {
		id = nextId++;

		uuid = newUuid;
		name = uuid.toString();
		frontendPort = newFrontendPort;
		backendPort = newBackendPort;
		distance = newMetric;
		originalDistance = distance;

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
				System.out.println("Couldn't find. Waiting...");
				try {
					Thread.sleep(10000);
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
		if (network.getUUID().compareTo(uuid) < 0)
			requestPeering();

		// Configure connection
		System.out.println("\tPeer connection established.");
		startKeepAlive();
		try {
			connection.setSoTimeout(peerTimeout);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}

		// Send first changes
		List<UUID> firstPath = new ArrayList<UUID>();
		firstPath.add(network.getUUID());
		sendChanges(network.getSequenceNumber(), firstPath,
				network.getAllNeighbors());

		// Listen until peer disconnects
		boolean listening = true;
		while (listening) {
			try {
				// Wait for incoming message
				String message = in.readLine();
				System.out.println(message);

				// Parse the message from our peer
				// No updates is just a keep alive message sent periodically
				if (message == null) {
					listening = false;
					break;
				} else if (message.equals("No updates")) {
					continue;
				} else if (message.startsWith("Updates ")) {
					// We have reachability updates
					int seqNum = Integer.parseInt(message.split(" ")[1]);
					System.out
							.println("Neighbor has sent updates with seqNum: "
							+ seqNum);

					// Ignore old sequence numbers. Just pull whole message to
					// toss it
					if (seqNum < network.getSequenceNumber()) {
						while (!in.readLine().equals(""))
							;
					}
					network.setSequenceNumber(seqNum);


					// Prep JSON
					Gson gson = new Gson();
					Type pathType = new TypeToken<ArrayList<UUID>>() {
					}.getClass();
					Type mapType = new TypeToken<HashMap<UUID, HashSet<Peer>>>() {
					}.getClass();

					// Read in path
					String pathLine = in.readLine();
					List<UUID> path = gson.fromJson(pathLine, pathType);
					System.out.println(path);

					// Read in the map
					Map<UUID, Set<Peer>> changes = new HashMap<UUID, Set<Peer>>();
					String line = in.readLine();
					System.out.println(line);
						
					Map<UUID, Set<Peer>> updates = gson.fromJson(line, mapType);
					System.out.println(updates);

					for (UUID uuid : updates.keySet()) {
						for (Peer peer : updates.get(uuid)) {
							if (network.addAjacency(uuid, peer))
								changes.get(uuid).add(peer);
						}
					}

						
					// TODO: If we saw any changes, inform our neighbors.
					// Skip any neighbor we already saw
					for (Neighbor n : network.getNeighbors()) {
						if (path.contains(n.getUuid()))
							continue;
						n.sendChanges(seqNum + 1, path, changes);
					}
					// Invalid message
					System.err.println("Neighbor sent us invalid message.");
				}

			} catch (SocketTimeoutException e) {
				System.err
						.println("Neighbor hasn't reported back, may be dead.");
				timer.cancel();
				break;
				// TODO: update our graph - how do we do that?
				// TODO: try to reconnect periodically?
			} catch (IOException e) {
				System.err.println("Couldn't read incoming peer message.");
			}
		}

		// Close socket and exit
		try {
			System.out.println("Disconnecting from neighbor.");
			connection.close();
		} catch (IOException e) {
			System.err.println("Could not close socket to neighbor.");
		}
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
		out.print("No updates\r\n\r\n");
		out.flush();
	}

	/*
	 * Sends changes from this server over this neighbor connection. If this
	 * neighbor is in the path this packet traveled, we return instead.
	 */
	public void sendChanges(int seqNum, List<UUID> path,
			Map<UUID, Set<Peer>> changes) {
		// If this neighbor is in the path, discard it
		if (path.contains(uuid))
			return;

		// First send header
		out.println("Updates " + seqNum);

		// Then send path
		// TODO: convert to JSON
		Gson gson = new Gson();
		String JSONpath = gson.toJson(path);
		
		out.println(JSONpath);
		
		out.println(gson.toJson(changes));

		// End with blank line
		out.println("");
		out.flush();

		// TODO: reset timer? Since a change message tells us we are alive that
		// would be okay to do
	}


	public UUID getUuid() {
		return uuid;
	}
	
	public int getDistanceMetric() {
		return distance;
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
