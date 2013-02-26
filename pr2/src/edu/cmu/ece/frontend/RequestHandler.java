package edu.cmu.ece.frontend;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;

import edu.cmu.ece.packet.RequestPacket;
import edu.cmu.ece.packet.Response404;
import edu.cmu.ece.packet.ResponseFileData;
import edu.cmu.ece.packet.ResponseHeader;

/**
 * Generates a response packet to be sent back to the client.
 * 
 * @author Michaels
 * 
 */
public class RequestHandler {
	private RequestPacket request;
	private OutputStream out;
	private PrintWriter textOut;

	private int clientId;
	private String header;

	/**
	 * Constructor. Sets necessary fields.
	 * 
	 * @param id
	 * @param request
	 * @param output
	 * @param textoutput
	 */
	public RequestHandler(int id, RequestPacket request, OutputStream output,
			PrintWriter textoutput) {
		this.clientId = id;
		this.request = request;
		this.out = output;
		this.textOut = textoutput;
	}

	/**
	 * Determine what the client has requested - either a local file, a file on
	 * one of our backend servers, or a modification to the routing table
	 */
	@SuppressWarnings("unused")
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
			if (false) { // (isInRoutingTable(file)) {
				this.handleRemoteRequest(file);
				return;
			} else {
				Response404.send404(request, textOut);
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
			Response404.send404(request, textOut);
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
		// Add to routing table now...
	}

	private void handlePeerConfig(String parameters) {
		System.out.println("Config request with parameters:");
		System.out.println(parameters);

		// TODO: figure out how to implement bitrate stuff
	}

	private void handleRemoteRequest(String file) {
		// TODO: handle remote requests
	}

	public void handleLocalRequest(File target) {
		// Generate and write headers to client.
		String header = ResponseHeader.makeHeader(target, request);
		textOut.write(header);
		textOut.flush();
		System.out.println(header);

		ResponseFileData.sendFile(target, request, out);
	}
}
