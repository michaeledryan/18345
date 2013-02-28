package edu.cmu.ece.frontend;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.UnknownHostException;

import edu.cmu.ece.backend.PeerData;
import edu.cmu.ece.backend.RoutingTable;
import edu.cmu.ece.backend.UDPManager;
import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.HTTPResponse404;
import edu.cmu.ece.packet.HTTPResponseHeader;
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
			OutputStream output, PrintWriter textoutput) {
		this.clientID = id;
		this.request = request;
		this.out = output;
		this.textOut = textoutput;
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
		System.out.println("Request: " + requested);
		if (requested.startsWith("peer/")) {
			// First check if we need to add to the routing table
			if (requested.startsWith("add?", 5)) {
				this.addToRoutingTable(requested.substring(9));
				return;
			}

			// Next check if we need to configure routing settings
			if (requested.startsWith("config?", 5)) {
				// Handle mystery peer config
				this.handlePeerConfig(requested.substring(12));
				return;
			}

			// Otherwise we have a file request... Look it up and either handle
			// the request or response with a 404
			String file = requested.substring(10);
			System.out.println("Remote file request for: " + file);
			if (RoutingTable.getInstance().checkPath(file)) {
				this.handleRemoteRequest(file);
				return;
			} else {
				HTTPResponse404.send404(request, textOut);
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
			HTTPResponse404.send404(request, textOut);
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
		System.out.println("Config request with parameters:");
		System.out.println("File " + path + " can be found on " + host + ":"
				+ port + " with bitrate " + rate + ".");

		router.addtofileNames(path,
				new PeerData(host, Integer.parseInt(port), Integer
						.parseInt(rate)));
		// Adds parameters to the Routing table.
	}

	private void handlePeerConfig(String parameters) {
		System.out.println("Config request with parameters:");
		System.out.println(parameters);

		// TODO: figure out how to implement bitrate stuff
	}

	private void handleRemoteRequest(String target) {
		/*
		 * Look up file in the routing table. If it isn't found, send a 404
		 */
		PeerData remote = router.getPeerData(target);
		if (remote == null)
			HTTPResponse404.send404(request, textOut);
		
		// Send UDP request packet with full HTTP Header copied in
		try {
			String requestString = "GET " + target + " HTTP/1.1\r\n"
					+ request.getFullHeader();
			UDPPacket backendRequest = new UDPPacket(clientID, remote.getIP(),
					remote.getPort(), requestString.getBytes(),
					UDPPacketType.REQUEST);
			udp.sendPacket(backendRequest.getPacket());
		} catch (UnknownHostException e) {
			System.out.println("Invalid host provided in routing table.");
		}

		/*
		 * Once the request is sent we can just stop and wait for the UDP
		 * listener to receive an incoming packet. It will pass it directly to
		 * ClientHandler to mirror over TCP
		 */
	}

	private void handleLocalRequest(File target) {
		// Generate and write headers to client.
		String header = HTTPResponseHeader.makeHeader(target, request);
		textOut.write(header);
		textOut.flush();

		ResponseFileData.sendFile(target, request, out);
	}
}