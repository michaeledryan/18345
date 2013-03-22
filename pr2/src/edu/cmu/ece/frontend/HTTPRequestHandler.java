package edu.cmu.ece.frontend;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import edu.cmu.ece.backend.PeerData;
import edu.cmu.ece.backend.RoutingTable;
import edu.cmu.ece.backend.UDPManager;
import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.HTTPResponseHeader;
import edu.cmu.ece.packet.HTTPResponses;
import edu.cmu.ece.packet.ResponseFileData;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;

/**
 * Generates a response packet to be sent back to the client.
 * 
 * @author Michaels
 * 
 */
public class HTTPRequestHandler {
	private UDPManager udp = UDPManager.getInstance();
	private RoutingTable router = RoutingTable.getInstance();
	private HTTPRequestPacket request;
	private OutputStream out;
	private PrintWriter textOut;
	private HTTPClientHandler handler;

	private int clientID;

	/**
	 * Constructor. Sets necessary fields.
	 * 
	 * @param id
	 * @param request
	 * @param output
	 * @param textoutput
	 */
	public HTTPRequestHandler(int id, HTTPRequestPacket request,
			OutputStream output, PrintWriter textoutput,
			HTTPClientHandler handler) {
		this.clientID = id;
		this.request = request;
		this.out = output;
		this.textOut = textoutput;
		this.handler = handler;
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
			// First check if we need to add to the routing table
			if (requested.startsWith("add?", 5)) {
				addToRoutingTable(requested.substring(9));
				return;
			}

			// Next check if we need to configure routing settings
			if (requested.startsWith("config?", 5)) {
				// Handle mystery peer config
				handlePeerConfig(requested.substring(12));
				return;
			}

			// Otherwise we have a file request... Look it up and either handle
			// the request or response with a 404
			String file = requested.substring(10);
			if (RoutingTable.getInstance().checkPath(file)) {
				handleRemoteRequest(file);
				return;
			} else {
				HTTPResponses.send404(request, textOut);
				return;
			}
		}

		// Otherwise, check if the file exists locally
		String filename = "content/" + request.getRequest();
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

	private void addToRoutingTable(String parameters) {
		String[] paramList = parameters.split("&");
		String path = "";
		String host = "";
		String port = "";
		String rate = "";

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
		}
		System.out.println("HTTP Request, client " + clientID);
		System.out.println("\tConfig request with parameters:");
		System.out.println("\t\tFile: " + path);
		System.out.println("\t\tPath: " + host + ":" + port);
		System.out.println("\t\tBitrate: " + rate);

		PeerData peerdata = new PeerData(host, Integer.parseInt(port),
				Integer.parseInt(rate), 0);
		router.addtofileNames(path, peerdata);

		HTTPResponses.sendPeerConfigMessage(path, request, textOut);

		// Adds parameters to the Routing table.
	}

	private void handlePeerConfig(String parameters) {
		System.out.println("HTTP Request, client " + clientID);
		System.out.println("\tConfig request with parameters:");
		System.out.println("\t\t" + parameters);

		int rate = Integer.parseInt(parameters.split("=")[1]);

		router.setBitRate(clientID, rate);
		// TODO: figure out how to implement bitrate stuff

		HTTPResponses.sendBitRateConfigMessage(rate, request, textOut);
	}

	private void handleRemoteRequest(String target) {
		/*
		 * Look up file in the routing table. If it isn't found, send a 404
		 */
		request.parseRanges(new File(target));
		System.out.println("HTTP Request, client " + clientID);
		System.out.println("\tRemote request for: " + target);
		System.out.println("\tAsking for range: "
				+ request.getFullRangeString());
		PeerData remote = router.getPeerData(target);
		if (remote == null) {
			HTTPResponses.send404(request, textOut);
			return;
		}

		// Send UDP request packet with full HTTP Header copied in
		try {

			byte[] requestStringBytes = ("GET " + target + " HTTP/1.1\r\n" + request
					.getFullHeader()).getBytes();

			byte[] packetData = new byte[4 + requestStringBytes.length];

			System.arraycopy(
					ByteBuffer.allocate(4)
							.putInt(router.getClientBitRate(clientID)).array(),
					0, packetData, 0, 4);
			System.arraycopy(requestStringBytes, 0, packetData, 4,
					requestStringBytes.length);
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
						// TODO Auto-generated catch block
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

		/*
		 * Once the request is sent we can just stop and wait for the UDP
		 * listener to receive an incoming packet. It will pass it directly to
		 * ClientHandler to mirror over TCP
		 */
	}

	private void handleLocalRequest(File target) {
		// Generate and write headers to client.
		System.out.println("HTTP Request, client " + clientID);
		System.out.println("\tLocal request for: " + target);
		System.out.println("\tAsking for range: "
				+ request.getFullRangeString());
		String header = HTTPResponseHeader.makeHeader(target, request);
		textOut.write(header);
		textOut.flush();

		ResponseFileData fileData = new ResponseFileData(target, request);
		fileData.sendFileToStream(out);
	}
}