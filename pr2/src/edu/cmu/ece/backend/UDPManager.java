package edu.cmu.ece.backend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class UDPManager implements Runnable {
	private static int packetLength = 1 << 16;
	private static int portNum;
	private DatagramSocket socket;
	private static UDPManager instance = null;

	public static void setPort(int port) {
		portNum = port;
	}

	public static UDPManager getInstance() {
		if (instance == null) {
			instance = new UDPManager();
		}
		return instance;
	}

	private UDPManager() {
	}

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
				// System.out.println("UDP packet received.");

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

	public void sendPacket(DatagramPacket packet) {
		try {
			int r = (int) (100 * Math.random());
			if (r > -1) {
				socket.send(packet);
				// System.out.println("YAY, " + r);
			} else {
				System.out.println("DROP IT LIKE ITS HOT, " + r);
			}
		} catch (IOException e) {
			System.err
					.println("Could not send packet on UDP." + e.getMessage());
		}
	}
}
