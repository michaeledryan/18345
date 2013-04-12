package edu.cmu.ece.routing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
		
		// The lesser UUID establishes the connection
		// And the greater UUID waits for it to come in
		if(network.getUUID().compareTo(uuid) < 0) {
			requestPeering();
		}
	}

	/*
	 * Send a request to the frontend over TCP to set up a TCP connection
	 */
	public void requestPeering() {
		System.out.println("Requesting peering relationship");
		try {
			// Connect to remote neighbor through TCP
			connection = new Socket(InetAddress.getByName(host),
					frontendPort);
			in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			out = new PrintWriter(connection.getOutputStream());

			// Send them our request to peer
			String request = "GET peering_request/" + uuid + "\r\n\r\n";
			out.print(request);

			// Begin our long and beautiful relationship
			startKeepAlive();
			new Thread(this).start();
		} catch (UnknownHostException e) {
			System.err.println("Invalid neighbor address.");
		} catch (IOException e) {
			System.out
					.println("Could not read/write to socket stream for neighbors.");
		}
	}

	/*
	 * Receives the port for our TCP connection via the UDP response.
	 */
	public void receivePeering(Socket socket, BufferedReader read,
			PrintWriter write) {
		System.out.println("Establishing peering relationship.");
		connection = socket;
		in = read;
		out = write;

		// Begin our long and beautiful relationship
		startKeepAlive();
		new Thread(this).start();
	}

	/*
	 * Main run loop - listens over TCP for the neighbor to send us cool
	 * information. If we time out on a read block, we know this neighbor has
	 * died :(
	 */
	@Override
	public void run() {
		System.out.println("Peer loop established.");
		// Listen until peer disconnects
		boolean listening = true;
		while (listening) {
			try {
				// Wait for incoming message
				String message = in.readLine();

				// Parse the message from our peer
				// No updates is just a keep alive message sent periodically
				if (message.equals("No updates")) {
					continue;
				} else if (message.startsWith("Update ")) {
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


					
					String pathLine = in.readLine();
					Gson gson = new Gson();
					Type collType = new TypeToken<List<UUID>>(){}.getClass();

					List<UUID> path = gson.fromJson(pathLine, collType); 

					// Read every JSON line representing a neighbor and its
					// new adjacencies. An empty line terminates the message.
					// Track changes so we can inform our neighbors
					String line;
					Map<UUID, Collection<Peer>> changes = new HashMap<UUID, Collection<Peer>>();
					line = in.readLine();
						
						collType = new TypeToken<HashMap<UUID, Collection<Peer>>>(){}.getClass();
						changes = gson.fromJson(line, collType);
						
						for (UUID uuid : changes.keySet()) {
							for (Peer peer : changes.get(uuid)) {
								if (network.addAjacency(uuid, peer))
									changes.get(uuid).add(peer);
							}
						}

						
					// TODO: If we saw any changes, inform our neighbors.
					// Skip any neighbor we already saw
					for (Neighbor n : network.getNeighbors()) {
						if (path.contains(n.getUuid()))
							continue;
						n.sendChanges(seqNum, path, changes);
					}
					// Invalid message
					System.err.println("Neighbor sent us invalid message.");
				}

			} catch (SocketTimeoutException e) {
				System.err
						.println("Neighbor hasn't reported back, may be dead.");
				timer.cancel();
				// TODO: update our graph - how do we do that?
				// TODO: try to reconnect periodically?
			} catch (IOException e) {
				System.err.println("Couldn't read incoming peer message.");
			}
		}

		// Close socket and exit
		try {
			connection.close();
		} catch (IOException e) {
			System.err.println("Could not close socket to neighbor.");
		}
	}
	
	
	/*
	 * Sends a keep-alive periodically to our neighbor
	 */
	private class KeepAliveTimer extends TimerTask {
		public void run() {
			Neighbor.this.sendKeepAlive();
		}
	}

	private void startKeepAlive() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new KeepAliveTimer(), peerTimeout,
				peerTimeout);
	}

	private void sendKeepAlive() {
		// Send no updates message
		out.print("No updates\n\n");
	}

	/*
	 * Sends changes from this server over this neighbor connection. If this
	 * neighbor is in the path this packet traveled, we return instead.
	 */
	public void sendChanges(int seqNum, List<UUID> path,
			Map<UUID, Collection<Peer>> changes) {
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
