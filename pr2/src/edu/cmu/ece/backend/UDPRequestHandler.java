package edu.cmu.ece.backend;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	private List<UDPPacket> packetsToSend = new ArrayList<UDPPacket>();
	private int lastSent = 0;
	private static int dataLength = 65507 - 12; // 2^16 - 20 (IP

	// header) - 8 (UDP
	// header) - 12 (header)

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

		// get header bytes, then data bytes. We need to split data into several
		// packets of dataLength size each.

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

		// Now that we have a final array of both the headers and the entirety
		// of the file (is that bad for memory?), we can stagger packet sending.

		int bytesSent = 0;
		int seqNum = 1;

		// Send multiple UDPPackets, each of an appropriate length.
		/*
		 * TODO: ACKs/NAKs. Currently testing with 100% packet reliability (this
		 * is bad!)
		 */
		while (bytesSent < finalArray.length) {

			// Get current packet length and copy what we need into a new array.
			// This is not terribly memory efficient. I think we may need to
			// write temp files? That could have weird repercussions on both
			// sides, as well as managing total length vs header and data
			// lengths. 
			int packetLength = (finalArray.length - bytesSent > dataLength) ? dataLength
					: finalArray.length - bytesSent;

			byte[] currentByteArray = Arrays.copyOfRange(finalArray, bytesSent,
					bytesSent + packetLength);

			UDPPacketType type = (bytesSent + packetLength == finalArray.length) ? UDPPacketType.END
					: UDPPacketType.DATA;

			UDPPacket finalPacket = new UDPPacket(backendRequest.getClientID(),
					backendRequest.getRemoteIP(),
					backendRequest.getRemotePort(), currentByteArray, type,
					seqNum);


			packetsToSend.add(finalPacket);

			bytesSent += packetLength;
			seqNum++;

		}

		udp.sendPacket(packetsToSend.get(lastSent).getPacket());
		System.out
				.println("bytesSent: " + bytesSent + "; " + finalArray.length);
	}

	private void handle404() throws UnknownHostException {
		// Also ugly... easier way?
		StringWriter response = new StringWriter();
		PrintWriter responseBuffer = new PrintWriter(response);
		HTTPResponses.send404(frontendRequest, responseBuffer);

		// Create UDP response packet from result, send out
		UDPPacket out = new UDPPacket(backendRequest.getClientID(),
				backendRequest.getRemoteIP(), backendRequest.getRemotePort(),
				response.toString().getBytes(), UDPPacketType.END, 1);
		udp.sendPacket(out.getPacket());
	}

	/**
	 * Resends the last packet.
	 */
	public void resend() {
		udp.sendPacket(packetsToSend.get(lastSent).getPacket());
	}

	/**
	 * Sends the next packet
	 */
	public void sendNext(int seqNum) {

		// if (seqNum != lastSent + 1) {
		// lastSent = seqNum;
		// }

		if (lastSent + 1 < packetsToSend.size()) {

			System.out.println("Sending packet " + (lastSent + 1));
			udp.sendPacket(packetsToSend.get(++lastSent).getPacket());
		}
	}

}
