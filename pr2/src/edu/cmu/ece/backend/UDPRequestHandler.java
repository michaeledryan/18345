package edu.cmu.ece.backend;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

import edu.cmu.ece.DCException;
import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.HTTPResponses;
import edu.cmu.ece.packet.HTTPResponseHeader;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;

public class UDPRequestHandler {
	private UDPManager udp = UDPManager.getInstance();
	private UDPPacket backendRequest;
	private HTTPRequestPacket frontendRequest;

	/**
	 * Constructor. Sets necessary fields.
	 * 
	 * @param request
	 * @param output
	 * @param textoutput
	 */
	public UDPRequestHandler(UDPPacket incoming) {
		// Ugly quadruple conversion... easier way?
		backendRequest = incoming;
		String header = new String(incoming.getData());
		BufferedReader packetReader = new BufferedReader(new CharArrayReader(
				header.toCharArray()));
		try {
			frontendRequest = new HTTPRequestPacket(packetReader);
		} catch (DCException e) {
			// Shouldn't happen
			e.printStackTrace();
		} catch (IOException e) {
			System.out
					.println("Couldn't convert UDP packet request to HTTP header.");
		}
	}

	/**
	 * Determine what the client has requested - either a local file, a file on
	 * one of our backend servers, or a modification to the routing table
	 */
	public void determineRequest() {
		// Check if the file exists locally
		String filename = frontendRequest.getRequest();
		File target = new File(filename);
		if (target.exists()) {
			frontendRequest.parseRanges(target);
			try {
				this.handleFileRequest(target);
			} catch (UnknownHostException e) {
				System.out.println("Couldn't respond to UDP client.");
			}
			return;
		} else {
			try {
				this.handle404();
			} catch (UnknownHostException e) {
				System.out.println("Couldn't respond to UDP client.");
			}
			return;
		}
	}

	private void handleFileRequest(File target) throws UnknownHostException {
		// Generate and write headers to client.
		String header = HTTPResponseHeader.makeHeader(target, frontendRequest);
		UDPPacket headerPacket = new UDPPacket(backendRequest.getClientID(),
				backendRequest.getRemoteIP(), backendRequest.getRemotePort(),
				header.toString().getBytes(), UDPPacketType.DATA);
		System.out.println("Sending over UDP Connection");
		udp.sendPacket(headerPacket.getPacket());

		// TODO: figure out how to use ResponseFileData to send file buffers
	}
		
	private void handle404() throws UnknownHostException {
		// Also ugly... easier way?
		StringWriter response = new StringWriter();
		PrintWriter responseBuffer = new PrintWriter(response);
		HTTPResponses.send404(frontendRequest, responseBuffer);

		// Create UDP response packet from result, send out
		UDPPacket out = new UDPPacket(backendRequest.getClientID(),
				backendRequest.getRemoteIP(), backendRequest.getRemotePort(),
				response.toString().getBytes(), UDPPacketType.DATA);
		udp.sendPacket(out.getPacket());
	}
}
