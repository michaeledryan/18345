package edu.cmu.ece.backend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;

public class UDPManager implements Runnable {
	private static int packetLength = 1 << 10;
	private int portNum;
	private DatagramSocket socket;
	private LinkedBlockingQueue<DatagramPacket> sendQueue;

	public UDPManager(int port) {
		portNum = port;
		sendQueue = new LinkedBlockingQueue<DatagramPacket>();
	}

	@Override
	public void run() {
		// Open a socket, set its timeout to let us send outgoing packets
		try {
			socket = new DatagramSocket(portNum);
			socket.setSoTimeout(1);
			System.out.format("Now listening for UDP on port %d\n", portNum);
		} catch (IOException e) {
			System.out.format("Could not listen for UDP on port %d\n", portNum);
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
			} catch (SocketTimeoutException e) {
				// Do nothing, this is expected
			} catch (IOException e) {
				System.out.println("Error receiving packet on UDP.");
			}

			// Flush the outgoing packet queue
			try {
				this.flushQueue();
			} catch (IOException e) {
				System.out.println("Error sending packet on UDP.");
			}
		}
	}

	public void queuePacket(DatagramPacket packet) {
		sendQueue.add(packet);
	}

	private void flushQueue() throws IOException {
		while (!sendQueue.isEmpty()) {
			try {
				DatagramPacket packet = sendQueue.take();
				socket.send(packet);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
