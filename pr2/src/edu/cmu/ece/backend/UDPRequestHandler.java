package edu.cmu.ece.backend;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.UnknownHostException;

import sun.misc.IOUtils;

import edu.cmu.ece.DCException;
import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.HTTPResponseHeader;
import edu.cmu.ece.packet.HTTPResponses;
import edu.cmu.ece.packet.ResponseFileData;
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
				handleFileRequest(target);
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
		System.out.println("Handling request...");
		String header = HTTPResponseHeader.makeHeader(target, frontendRequest);

		byte[] headerBytes = header.toString().getBytes();

		HTTPRequestPacket requestPacket = null;

		try {
			requestPacket = new HTTPRequestPacket(new BufferedReader(
					new StringReader(header)));
		} catch (DCException | IOException e) {
			e.printStackTrace();
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ResponseFileData.sendFile(target, requestPacket, out);

		// Concatenate headers and data, avoiding concurrency issues without any
		// kind of ARQ
		byte[] outArray = out.toByteArray();
		byte[] finalArray = new byte[headerBytes.length + outArray.length];

		System.arraycopy(headerBytes, 0, finalArray, 0, headerBytes.length);
		System.arraycopy(outArray, 0, finalArray, headerBytes.length,
				outArray.length);

		UDPPacket finalPacket = new UDPPacket(backendRequest.getClientID(),
				backendRequest.getRemoteIP(), backendRequest.getRemotePort(),
				finalArray, UDPPacketType.DATA);

		udp.sendPacket(finalPacket.getPacket());
		System.out.println("Sent data.");

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
