package edu.cmu.ece.backend;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

import edu.cmu.ece.DCException;
import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.HTTPResponseHeader;
import edu.cmu.ece.packet.HTTPResponses;
import edu.cmu.ece.packet.ResponseFileData;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;

public class UDPRequestHandler {
	private UDPPacket backendRequest;
	private HTTPRequestPacket frontendRequest;
	
	private String header;
	private ResponseFileData fileData;
	private int numPackets;
	private boolean alive = true;

	private static int dataLength = 65000;


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
		header = new String(incoming.getData());
		BufferedReader packetReader = new BufferedReader(new CharArrayReader(
				header.toCharArray()));
		try {
			frontendRequest = new HTTPRequestPacket(packetReader);
		} catch (DCException e) {
			// Shouldn't happen
			e.printStackTrace();
		} catch (IOException e) {
			System.err
					.println("Couldn't convert UDP packet request to HTTP header.");
		}
	}

	/**
	 * Determine what the client has requested - either a local file, a file on
	 * one of our backend servers, or a modification to the routing table.
	 * 
	 * Returns the number of packets;
	 */
	public int initializeRequest() {
		// Check if the file exists locally
		String filename = frontendRequest.getRequest();
		File target = new File(filename);
		if (target.exists()) {
			frontendRequest.parseRanges(target);
			
			try {
				fileData = new ResponseFileData(target, frontendRequest);
				numPackets = 1 + fileData.getNumPackets(dataLength);
				generateFileHeader(target);
			} catch (UnknownHostException e) {
				System.err.println("Couldn't respond to UDP client.");
			}
		} else {
				numPackets = 1;
				this.generate404Header();
		}
		return numPackets;
	}

	private void generateFileHeader(File target) throws UnknownHostException {
		// Generate and write headers to client.
		header = HTTPResponseHeader.makeHeader(target, frontendRequest);
	}

	private void generate404Header() {
		// Also ugly... easier way?
		StringWriter response = new StringWriter();
		PrintWriter responseBuffer = new PrintWriter(response);
		HTTPResponses.send404(frontendRequest, responseBuffer);
		header = response.toString();
	}

	/**
	 * Gets the seqNum-th packet to send
	 * 
	 * @throws UnknownHostException
	 */
	public UDPPacket getPacket(int seqNum) {
		// sequence number 0 is the header
		UDPPacket packet = null;
		if (seqNum == 0) {
			try {
				packet = new UDPPacket(backendRequest.getClientID(),
						backendRequest.getRemoteIP(),
						backendRequest.getRemotePort(), header.getBytes(),
						UDPPacketType.END, 0);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			return packet;
		}

		// Otherwise, we need to dig into the file for the packet
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		fileData.getPacketData(out, seqNum - 1, dataLength);
		UDPPacketType type = (seqNum == numPackets - 1) ? UDPPacketType.END
				: UDPPacketType.DATA;

		// Create a packet from this output stream
		try {
			packet = new UDPPacket(backendRequest.getClientID(),
					backendRequest.getRemoteIP(),
					backendRequest.getRemotePort(), out.toByteArray(), type,
					seqNum);
		} catch (UnknownHostException e) {
			System.err.println("Couldn't respond to UDP client.");
		}
		return packet;
	}
	
	public void kill() {
		alive = false;
	}
	
	public boolean isAlive() {
		return alive;
	}
}
