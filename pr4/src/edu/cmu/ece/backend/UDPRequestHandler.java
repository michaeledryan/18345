package edu.cmu.ece.backend;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import edu.cmu.ece.DCException;
import edu.cmu.ece.frontend.HTTPRequestHandler;
import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.HTTPResponseHeader;
import edu.cmu.ece.packet.HTTPResponses;
import edu.cmu.ece.packet.ResponseFileData;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;
import edu.cmu.ece.routing.RoutingTable;

/**
 * Handles a reqeust for data to be sent over the UDP connection to a remote
 * peer.
 * 
 * @author michaels
 * 
 */
public class UDPRequestHandler {
	private UDPPacket backendRequest;
	private HTTPRequestPacket frontendRequest;

	private int id;
	private String header;
	private ResponseFileData fileData;
	private int numPackets;
	private int period;
	private int phase;
	private boolean alive = true;
	private int byteRate;
	private int dataLength;
	private long timeLastSent = 0;
	private int bytesSent = 0;

	private static int maxDataLength = 65000;
	private static int requests = 0;

	/**
	 * Constructor. Sets necessary fields.
	 * 
	 * @param request
	 * @param output
	 * @param textoutput
	 */
	public UDPRequestHandler(UDPPacket incoming) {
		// Create request
		id = ++requests;
		backendRequest = incoming;

		System.out.println("\tAssigned request id " + id);

		// Parse request data out
		byte[] requestData = incoming.getData();

		// Get byterate
		byteRate = ByteBuffer.wrap(Arrays.copyOfRange(requestData, 0, 4))
				.getInt();

		// Set dataLength based on byterate
		dataLength = maxDataLength;
		
		System.out.println("\tRequest has byteRate: " + byteRate);
		System.out.println("\tUsing packets of size: " + dataLength);

		// Get period
		period = ByteBuffer.wrap(Arrays.copyOfRange(requestData, 4, 8))
				.getInt();

		// Get phase
		phase = ByteBuffer.wrap(Arrays.copyOfRange(requestData, 8, 12))
				.getInt();

		// Get HTTP header
		header = new String(Arrays.copyOfRange(requestData, 4,
				requestData.length));
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

		System.out.println("\tRequested file: " + frontendRequest.getRequest());
	}

	
	
	/**
	 * Returns the number of packets;
	 */
	public int initializeRequest() {
		// Check if the file exists locally
		String filename = frontendRequest.getRequest();
		System.out.println("Looking for:" + filename);
		File target = new File(filename);
		if (!target.exists()) {
			target = new File(HTTPRequestHandler.getContentPath() + 
					filename);
		}
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

	/**
	 * Generates the header for a response.
	 * 
	 * @param target
	 *            the requested file
	 * @throws UnknownHostException
	 */
	private void generateFileHeader(File target) throws UnknownHostException {
		// Generate and write headers to client.
		header = HTTPResponseHeader.makeHeader(target, frontendRequest);
	}

	/**
	 * Sends a 404 message.
	 */
	private void generate404Header() {
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
		if (seqNum == numPackets - 1) {
			System.out.println("SENDING LAST PACKET");
		}
		UDPPacket packet = null;
		if (seqNum == 0) {
			try {
				packet = new UDPPacket(backendRequest.getClientID(), id,
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
			packet = new UDPPacket(backendRequest.getClientID(), id,
					backendRequest.getRemoteIP(),
					backendRequest.getRemotePort(), out.toByteArray(), type,
					seqNum);
		} catch (UnknownHostException e) {
			System.err.println("Couldn't respond to UDP client.");
		}
		return packet;
	}

	/**
	 * Returns whether or not we can send a new packet and stay within the
	 * bitrate limit.
	 * 
	 * @return
	 */
	public boolean canISend() {
		if (byteRate == 0) {
			return true;
		}

		int numBytes = dataLength;
		if (timeLastSent == 0) {
			timeLastSent = System.currentTimeMillis();
			return true;
		}

		// Running average bitrate
		long now = System.currentTimeMillis();
		if ((bytesSent + numBytes) >= ((now - timeLastSent) * byteRate / 2000)) {
			return false;
		} else {
			bytesSent += numBytes;
			return true;
		}

	}

	/**
	 * Kills the UDPRequestHandler, preventing it from sending any more packets.
	 */
	public void kill() {
		if (alive) {
			System.out.println("\tWe have slain client "
					+ backendRequest.getClientID() + " request " + id);
			alive = false;
			UDPSender.getInstance().clearRequester(this);
		}
	}

	public boolean isAlive() {
		return alive;
	}

	public int getID() {
		return id;
	}

	public int getPeriod() {
		return period;
	}

	public int getPhase() {
		return phase;
	}
}
