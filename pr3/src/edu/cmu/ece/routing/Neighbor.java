package edu.cmu.ece.routing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;

import edu.cmu.ece.backend.PeerData;
import edu.cmu.ece.backend.UDPManager;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;

public class Neighbor implements Comparable<Neighbor> {
	private static int nextId = 1;
	
	private int id;
	private UUID uuid;
	private String name;
	private String host;
	private int frontendPort;
	private int backendPort;
	private int distance;
	private int originalDistance;

	private int inPort;
	private int outPort;
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

		inPort = RoutingTable.getInstance().getBackendPort() + id;
		outPort = -1;

		// Begin negotiating connection
		try {
			UDPPacket request = new UDPPacket(0, 0, host, backendPort, uuid
					.toString().getBytes(), UDPPacketType.PEERING_REQUEST, 0);
			requestPeering(request);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Send out a request over UDP to the client to get the port for our TCP
	 * connection. Creates a timeout to resend.
	 */
	public void requestPeering(UDPPacket packet) {
		if (outPort > 0)
			return;
		UDPManager.getInstance().sendPacket(packet.getPacket());

		new NeighborPeeringRequest(this, packet, 500);
	}

	/*
	 * Receives the port for our TCP connection via the UDP response.
	 */
	public void receivePeering(int port) {
		outPort = port;
		try {
			connection = new Socket(InetAddress.getByName(host), port,
					InetAddress.getLocalHost(), inPort);
			in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			out = new PrintWriter(connection.getOutputStream());
		} catch (UnknownHostException e) {
			System.err.println("Couldn't get specified peer address.");
		} catch (IOException e) {
			System.err.println("Couldn't connect to peer.");
		}
	}


	public UUID getUuid() {
		return uuid;
	}
	
	public int getInPort() {
		return inPort;
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
