package edu.cmu.ece.routing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;

import edu.cmu.ece.backend.PeerData;

public class Neighbor implements Comparable<Neighbor>, Runnable {
	private static int nextId = 1;
	private static int peerTimeout = 1000; // ms
	private RoutingTable router = RoutingTable.getInstance();
	
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
				} else if (message.startsWith("Updates: ")) {
					// We have reachability updates
					int numUpdates = Integer.parseInt(message.split(" ")[1]);
					System.out.println("Neighbor has sent us "+numUpdates+" updates");

					// TODO: parse updates into Peers
				} else {
					// Invalid message
					System.err.println("Neighbor sent us invalid message.");
				}

			} catch (SocketTimeoutException e) {
				System.err
						.println("Neighbor hasn't reported back, may be dead.");
				// TODO: update our graph
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
