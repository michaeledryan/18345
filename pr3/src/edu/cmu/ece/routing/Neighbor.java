package edu.cmu.ece.routing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

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
		if(router.getUUID().compareTo(uuid) < 0) {
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

					// TODO: the second line should be a JSON list of nodes that
					// this packet has traveled through, so we can choose not to
					// serve route updates back to a neighbor that saw them
					// already. Finally comes a series of UUIDs to sets of their
					// neighbors and distances that changed
					Collection<UUID> path;

					// Read every JSON line representing a neighbor and its
					// new adjacencies. An empty line terminates the message.
					// Track changes so we can inform our neighbors
					String line;
					Map<UUID, Collection<Peer>> changes = new HashMap<UUID, Collection<Peer>>();
					while (!(line = in.readLine()).equals("")) {
						// TODO: parse JSON into collection of UUIDs to Peers
						// Not sure what the easiest way is. Currently assuming
						// we have some arbitrary collection of peers as a
						// result so it doesn't matter
						UUID uuid;
						Collection<Peer> peers;
						//changes.put(uuid, new Collection<Peer>());

						for (Peer peer : peers) {
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
				// TODO: update our graph
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
	class KeepAliveTimer extends TimerTask {
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
	public void sendChanges(int seqNum, Collection<UUID> path,
			Map<UUID, Collection<Peer>> changes) {
		// If this neighbor is in the path, discard it
		if (path.contains(uuid))
			return;

		// First send header
		out.println("Updates " + seqNum);

		// Then send path
		// TODO: convert to JSON

		// Then send set of changes
		for (Map.Entry<UUID, Collection<Peer>> e : changes.entrySet()) {
			// TODO: convert to JSON
		}

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


	@Override
	public int compareTo(Neighbor o) {
		return uuid.compareTo(o.uuid);
	}
}
