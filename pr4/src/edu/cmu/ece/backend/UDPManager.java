package edu.cmu.ece.backend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

/**
 * Handles the UDP communication for the server. Receives incoming packets and
 * sends outgoing ones.
 * 
 * @author michaels
 * 
 */
public class UDPManager implements Runnable {
	private static int packetLength = 1 << 16;
	private static int portNum;
	private DatagramSocket socket;
	private static UDPManager instance = null;

	public static void setPort(int port) {
		portNum = port;
	}

	/**
	 * Singleton class
	 * 
	 * @return the instance
	 */
	public static UDPManager getInstance() {
		if (instance == null) {
			instance = new UDPManager();
		}
		return instance;
	}

	private UDPManager() {
	}

	/**
	 * Blocks waiting for new sockets to come in, then calls a handler on each
	 * one.
	 */
	@Override
	public void run() {
		// Open a socket, set its timeout to let us send outgoing packets
		try {
			socket = new DatagramSocket(portNum);
			System.out.format("Now listening for UDP on port %d\n", portNum);
		} catch (IOException e) {
			System.err.format("Could not listen for UDP on port %d\n", portNum);
			System.exit(-1);
		}

		// Main UDP loop
		while (true) {
			// Try to receive a packet
			DatagramPacket packet;
			try {
				packet = new DatagramPacket(new byte[packetLength],
						packetLength);
				socket.receive(packet);

				// Handle packet then loop back
				UDPPacketHandler handle = new UDPPacketHandler(packet);
				new Thread(handle).start();
			} catch (SocketTimeoutException e) {
				// Do nothing, this is fine
			} catch (IOException e) {
				System.err.println("Error receiving packet on UDP.");
			}
		}
	}

	/**
	 * Sends a packet. The r variable helps simulate packet loss - this was used
	 * in place of nf.
	 * 
	 * @param packet
	 */
	public void sendPacket(DatagramPacket packet) {
		try {
			int r = (int) (100 * Math.random()); // random number
			if (r >= 0) {
				// check whether or not we actually send the packet.
				// Change the number to change drop rate.
				socket.send(packet);
			} else {
				System.out.println("DROP IT LIKE ITS HOT, " + r);
			}
		} catch (IOException e) {
			System.err
					.println("Could not send packet on UDP." + e.getMessage());
		}
	}
}
