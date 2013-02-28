package edu.cmu.ece.packet;

import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class UDPPacket {
	// Core packet elements
	private DatagramPacket datagram;
	private InetAddress remoteIP;
	private int remotePort;

	// Custom packet header
	public final int HEADER_SIZE = 3;
	private int clientID;
	@SuppressWarnings("unused")
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
		dataLength = body.length - HEADER_SIZE * Integer.SIZE / 8;
		remoteIP = packet.getAddress();
		remotePort = packet.getPort();

		// Parse out custom packet header from data
		clientID = Array.getInt(body, 0);
		sequenceNumber = Array.getInt(body, 1);
		type = UDPPacketType.fromInt(Array.getInt(body, 3));
	}

	// Constructor to create new packet to send
	public UDPPacket(int client, String destinationIP, int destinationPort,
			byte[] data, UDPPacketType type) throws UnknownHostException {
		// Create the UDP header
		remoteIP = InetAddress.getByName(destinationIP);
		remotePort = destinationPort;

		// Set other data
		clientID = client;
		sequenceNumber = 1;

		/*
		 * Create full packet body Header consists of the clientID sending the
		 * packet, the sequence of that packet. Then is the actual data to send
		 */
		body = new byte[data.length + HEADER_SIZE * Integer.SIZE / 8];
		Array.setInt(body, 0, client);
		Array.setInt(body, 1, 1);
		Array.setInt(body, 2, type.getValue());

		System.arraycopy(data, 0, body, HEADER_SIZE * Integer.SIZE / 8,
				data.length);
		dataLength = data.length;

		// Plug the data into a DatagramPacket
		datagram = new DatagramPacket(body, body.length, remoteIP, remotePort);
	}

	public String getRemoteIP() {
		return remoteIP.getHostAddress();
	}

	public int getRemotePort() {
		return remotePort;
	}

	public int getClientID() {
		return clientID;
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
}
