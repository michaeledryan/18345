package edu.cmu.ece.packet;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Wrapper class for our DatagramPackets.
 * 
 * @author michael
 * 
 */
public class UDPPacket implements Comparable<UDPPacket> {
	// Core packet elements
	private DatagramPacket datagram;
	private InetAddress remoteIP;
	private int remotePort;

	// Custom packet header
	public final int HEADER_SIZE = 4;
	private int clientID;
	private int requestID;

	private int sequenceNumber;
	private UDPPacketType type;

	// Custom packet body
	private byte[] body;
	private int dataLength; // Length of the data being sent, excluding header

	// Constructor to parse in received packet
	public UDPPacket(DatagramPacket packet) {
		// Get raw packet information from UDP
		datagram = packet;
		body = packet.getData();
		dataLength = packet.getLength() - HEADER_SIZE * Integer.SIZE / 8;
		// lengths on DatagramPackets are weird.
		remoteIP = packet.getAddress();
		remotePort = packet.getPort();

		// Parse out custom packet header from data
		clientID = ByteBuffer.wrap(Arrays.copyOfRange(body, 0, 4)).getInt();
		requestID = ByteBuffer.wrap(Arrays.copyOfRange(body, 4, 8)).getInt();
		sequenceNumber = ByteBuffer.wrap(Arrays.copyOfRange(body, 8, 12))
				.getInt();
		type = UDPPacketType.fromInt(ByteBuffer.wrap(
				Arrays.copyOfRange(body, 12, 16)).getInt());
	}

	/**
	 * Constructor to create new packet to send
	 * 
	 * @param client
	 *            the clientID
	 * @param request
	 *            the request number
	 * @param destinationIP
	 *            target IP
	 * @param destinationPort
	 *            target port
	 * @param data
	 *            data to be send
	 * @param type
	 *            type of UDPPacket
	 * @param seqNum
	 *            sequence number in transmission
	 * @throws UnknownHostException
	 */
	public UDPPacket(int client, int request, String destinationIP,
			int destinationPort, byte[] data, UDPPacketType type, int seqNum)
			throws UnknownHostException {
		// Create the UDP header
		remoteIP = InetAddress.getByName(destinationIP);
		remotePort = destinationPort;

		// Set other data
		clientID = client;
		requestID = request;
		sequenceNumber = seqNum;

		/*
		 * Create full packet body Header consists of the clientID sending the
		 * packet, the sequence of that packet. Then is the actual data to send
		 */
		body = new byte[data.length + HEADER_SIZE * Integer.SIZE / 8];

		byte[] clientArray = ByteBuffer.allocate(4).putInt(client).array();
		byte[] requestArray = ByteBuffer.allocate(4).putInt(request).array();
		byte[] seqArray = ByteBuffer.allocate(4).putInt(sequenceNumber).array();
		byte[] typeArray = ByteBuffer.allocate(4).putInt(type.getValue())
				.array();

		System.arraycopy(clientArray, 0, body, 0, 4);
		System.arraycopy(requestArray, 0, body, 4, 4);
		System.arraycopy(seqArray, 0, body, 8, 4);
		System.arraycopy(typeArray, 0, body, 12, 4);

		System.arraycopy(data, 0, body, HEADER_SIZE * Integer.SIZE / 8,
				data.length);
		dataLength = data.length;

		// Plug the data into a DatagramPacket
		datagram = new DatagramPacket(body, body.length, remoteIP, remotePort);
	}

	public String getRemoteIP() {
		return remoteIP.getHostAddress();
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public int getClientID() {
		return clientID;
	}

	public int getRequestID() {
		return requestID;
	}

	public UDPPacketType getType() {
		return type;
	}

	public byte[] getData() {
		// Returns the data of this packet minus its header
		byte[] data = new byte[dataLength];
		System.arraycopy(body, HEADER_SIZE * Integer.SIZE / 8, data, 0,
				dataLength);
		return data;
	}

	public DatagramPacket getPacket() {
		return datagram;
	}

	@Override
	public int compareTo(UDPPacket other) {
		return sequenceNumber - other.getSequenceNumber();
	}
}
