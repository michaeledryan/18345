package edu.cmu.ece.frontend;

import java.io.BufferedReader;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import edu.cmu.ece.backend.PeerData;
import edu.cmu.ece.backend.UDPManager;
import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.HTTPResponseHeader;
import edu.cmu.ece.packet.HTTPResponses;
import edu.cmu.ece.packet.ResponseFileData;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;
import edu.cmu.ece.routing.Gossiper;
import edu.cmu.ece.routing.GraphPeer;
import edu.cmu.ece.routing.Neighbor;
import edu.cmu.ece.routing.NetworkGraph;
import edu.cmu.ece.routing.NetworkGraph.CostPathPair;
import edu.cmu.ece.routing.RoutingTable;

/**
 * Generates a response packet to be sent back to the client.
 * 
 * @author Michaels
 * 
 */
public class HTTPRequestHandler {
	private UDPManager udp = UDPManager.getInstance();
	private RoutingTable router = RoutingTable.getInstance();
	private NetworkGraph graph = NetworkGraph.getInstance();
	private HTTPRequestPacket request;
	private HTTPClientHandler handler;

	private Socket socket;
	private BufferedReader textIn;
	private OutputStream out;
	private PrintWriter textOut;

	private static String contentPath = "content/";
	private int clientID;
	private String clientIP;

	/**
	 * Constructor. Sets necessary fields.
	 * 
	 * @param id
	 * @param request
	 * @param output
	 * @param textoutput
	 */
	public HTTPRequestHandler(int id, String ip, HTTPRequestPacket request,
			BufferedReader input, OutputStream output, PrintWriter textoutput,
			Socket socket, HTTPClientHandler handler) {
		this.clientID = id;
		this.clientIP = ip;
		this.request = request;
		this.handler = handler;

		this.socket = socket;
		this.textIn = input;
		this.out = output;
		this.textOut = textoutput;
	}

	/**
	 * Sets the content path to the specified path
	 * 
	 * @param path
	 *            the new content path
	 */
	public static void setContentPath(String path) {
		contentPath = path;
	}

	/**
	 * Determine what the client has requested - either a local file, a file on
	 * one of our backend servers, or a modification to the routing table
	 */
	public void determineRequest() {
		// First check if we have a remote request
		String requested = request.getRequest();
		if (requested.startsWith("/"))
			requested = requested.substring(1);
		if (requested.startsWith("peer/")) {
			if (requested.startsWith("addneighbor?", 5)) {
				addNeighbor(requested.substring(17));
				return;
			}

			// First check if we need to add to the routing table
			if (requested.startsWith("add?", 5)) {
				addToRoutingTable(requested.substring(9));
				return;
			}

			if (requested.startsWith("kill", 5)) {
				HTTPResponses.sendKillMessage(request, textOut);
				System.exit(0);
				return;
			}

			// Return this client's UUID
			if (requested.startsWith("uuid", 5)) {
				HTTPResponses.sendUUID(request, textOut);
				return;
			}

			// Return our neighbors
			if (requested.startsWith("neighbors", 5)) {
				HTTPResponses.sendNeighbors(request, textOut);
				return;
			}

			// Return our network map
			if (requested.startsWith("map", 5)) {
				HTTPResponses.sendNetworkMap(request, textOut);
				return;
			}

			// Returns a list of peers and their distance with our file
			if (requested.startsWith("rank", 5)) {
				HTTPResponses.sendRankResponse(requested.substring(10),
						request, textOut);
				return;
			}

			// Returns something??
			if (requested.startsWith("search", 5)) {
				System.out.println("SEARCH");
				new Gossiper(requested.substring(12), NetworkGraph
						.getInstance().getSearchTTL(), request, textOut);
				return;
			}

			// Next check if we need to configure routing settings
			if (requested.startsWith("config?", 5)) {
				// Handle mystery peer config
				handlePeerConfig(requested.substring(12));
				return;
			}

			// A file request
			if (requested.startsWith("view", 5)) {
				String file = requested.substring(10);
				if (RoutingTable.getInstance().checkPath(file)) {
					handleRemoteRequest(file);
					return;
				} else if (NetworkGraph.getInstance().checkFile(file)) {
					handleSearchRequest(file);
					return;
				} else {
					System.out.println(file);
					HTTPResponses.send404(request, textOut);
					return;
				}
			}

			// Otherwise just 404
			HTTPResponses.send404(request, textOut);
			return;
		} else if (requested.startsWith("peering_request/")) {
			String uuid = requested.split("/")[1];
			handlePeeringRequest(UUID.fromString(uuid));
			return;
		}

		// Otherwise, check if the file exists locally
		String filename = contentPath + request.getRequest();
		File target = new File(filename);
		if (target.exists()) {
			request.parseRanges(target);
			this.handleLocalRequest(target);
			return;
		} else {
			System.out.println("HTTP Request, client " + clientID
					+ "\n\t404 Not Found: " + request.getRequest());
			HTTPResponses.send404(request, textOut);
			return;
		}
	}

	/**
	 * Contacts remote peers that have been found via the search protocol. Gets
	 * the list of UUIDs of peers that have the file and then sends a request on
	 * the nearest hop to that file.
	 * 
	 * @param filename
	 *            the filename for which we are searching.
	 */
	private void handleSearchRequest(String filename) {
		Set<UUID> uuids = graph.getNodesWithFile(filename);
		boolean hasMe = uuids.contains(graph.getUUID());
		int actualSize = uuids.size() - (hasMe ? 1 : 0);
		int phaseOffset = 0;
		Map<UUID, CostPathPair> paths = graph.getShortestPaths();

		for (UUID u : uuids) {
			if (!u.equals(graph.getUUID())) {

				// Get first neighbor on path to final destination
				Neighbor n = graph.getNeighbor(paths.get(u).path.get(0));

				// Sends request, incrementing phaseOffset
				n.sendViewRequest(clientID, actualSize, phaseOffset++, u,
						request);

			}
		}
	}

	private void addNeighbor(String parameters) {
		String[] paramList = parameters.split("&");
		String uuid = "";
		String host = "";
		String frontend = "";
		String backend = "";
		String metric = "";

		for (int i = 0; i < paramList.length; i++) {
			String[] keyValue = paramList[i].split("=");
			if (keyValue[0].equals("metric"))
				metric = keyValue[1];
			else if (keyValue[0].equals("host"))
				host = keyValue[1];
			else if (keyValue[0].equals("backend"))
				backend = keyValue[1];
			else if (keyValue[0].equals("frontend"))
				frontend = keyValue[1];
			else if (keyValue[0].equals("uuid"))
				uuid = keyValue[1];
		}

		Neighbor n = new Neighbor(UUID.fromString(uuid), host,
				Integer.parseInt(frontend), Integer.parseInt(backend),
				Integer.parseInt(metric), true);
		graph.addNeighbor(n);

		HTTPResponses.sendAddNeighborMessage(request, textOut, uuid, host,
				frontend, backend);

	}

	private void addToRoutingTable(String parameters) {
		String[] paramList = parameters.split("&");
		String path = "";
		String host = "";
		String port = "";
		String rate = "";
		String uuid = "";

		for (int i = 0; i < paramList.length; i++) {
			String[] keyValue = paramList[i].split("=");
			if (keyValue[0].equals("path"))
				path = keyValue[1];
			else if (keyValue[0].equals("host"))
				host = keyValue[1];
			else if (keyValue[0].equals("port"))
				port = keyValue[1];
			else if (keyValue[0].equals("rate"))
				rate = keyValue[1];
			else if (keyValue[0].equals("peer"))
				uuid = keyValue[1];
		}

		if (uuid.equals("")) {

			PeerData peerdata = new PeerData(host, Integer.parseInt(port),
					Integer.parseInt(rate), 0);
			router.addtofileNames(path, peerdata);

			HTTPResponses.sendPeerConfigMessage(path, request, textOut,
					peerdata);

		} else {

			router.addContentToGraph(path, new GraphPeer(UUID.fromString(uuid),
					Integer.parseInt(rate)));

			HTTPResponses.sendPeerUUIDConfigMessage(path, request, textOut,
					uuid, rate);
		}

		// Adds parameters to the Routing table.
	}

	private void handlePeerConfig(String parameters) {
		int rate = Integer.parseInt(parameters.split("=")[1]);

		router.setBitRate(clientIP, rate);

		HTTPResponses.sendBitRateConfigMessage(rate, request, textOut);
	}

	/**
	 * Sends a request to a remote peer for the file.
	 * 
	 * @param target
	 */
	private void handleRemoteRequest(String target) {
		/*
		 * Look up file in the routing table. If it isn't found, send a 404
		 */
		request.parseRanges(new File(target));
		Set<PeerData> peers = router.getPeerData(target);
		if (peers == null || peers.size() == 0) {
			HTTPResponses.send404(request, textOut);
			return;
		}

		// Set up the request packet
		byte[] requestStringBytes = ("GET " + target + " HTTP/1.1\r\n" + request
				.getFullHeader()).getBytes();
		byte[] packetData = new byte[12 + requestStringBytes.length];
		// Add bitrate
		System.arraycopy(
				ByteBuffer
						.allocate(4)
						.putInt(router.getClientBitRate(clientIP)
								/ peers.size()).array(), 0, packetData, 0, 4);
		// Add data
		System.arraycopy(requestStringBytes, 0, packetData, 12,
				requestStringBytes.length);

		// Add period
		System.arraycopy(ByteBuffer.allocate(4).putInt(peers.size()).array(),
				0, packetData, 4, 4);

		// Send UDP request packet with full HTTP Header copied in to each
		// possible server
		int phase = 0;
		for (PeerData remote : peers) {
			try {
				// Add phase offset
				System.arraycopy(ByteBuffer.allocate(4).putInt(phase).array(),
						0, packetData, 8, 4);
				phase++;

				UDPPacket backendRequest = new UDPPacket(clientID, 0,
						remote.getIP(), remote.getPort(), packetData,
						UDPPacketType.REQUEST, 0);
				udp.sendPacket(backendRequest.getPacket());

				// Set up simple spin loop to resend the request
				final HTTPClientHandler myHandler = handler;
				final DatagramPacket myPacket = backendRequest.getPacket();
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						if (!myHandler.getGotAck()) {
							UDPManager.getInstance().sendPacket(myPacket);
							this.run();
						}
					}
				}).start();
			} catch (UnknownHostException e) {
				System.err.println("Invalid host provided in routing table.");
			}
		}

		/*
		 * Once the request is sent we can just stop and wait for the UDP
		 * listener to receive an incoming packet. It will pass it directly to
		 * ClientHandler to mirror over TCP
		 */
	}

	/**
	 * Handles a request for a local file.
	 * 
	 * @param target
	 */
	private void handleLocalRequest(File target) {
		// Generate and write headers to client.
		String header = HTTPResponseHeader.makeHeader(target, request);
		textOut.write(header);
		textOut.flush();

		ResponseFileData fileData = new ResponseFileData(target, request);
		fileData.sendFileToStream(out);
	}

	/**
	 * Handles a peering request - gives socket to neighbor and kills thread
	 */
	private void handlePeeringRequest(UUID uuid) {

		// Get the requesting neighbor from our table and peer
		Neighbor n = graph.getNeighbor(uuid);
		if (n == null) {
			n = new Neighbor(uuid, request.getHeader("host"),
					Integer.parseInt(request.getHeader("frontend")),
					Integer.parseInt(request.getHeader("backend")),
					Integer.parseInt(request.getHeader("metric")), false);
			NetworkGraph.getInstance().addNeighbor(n);
		}
		n.receivePeering(socket, textIn, textOut);

		// Kill this thread - this socket has been passed to the neighbor's
		// own thread
		Thread.currentThread().interrupt();
	}

	public static String getContentPath() {
		return contentPath;
	}
}