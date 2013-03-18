package edu.cmu.ece.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.cmu.ece.DCException;
import edu.cmu.ece.backend.RoutingTable;
import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;

/**
 * Manages a connection to a given client.
 * 
 * @author Michaels
 * 
 */
public class HTTPClientHandler implements Runnable {

	private static int clientCount = 0;
	private int id;
	private boolean listening = true;
	private Queue<UDPPacket> packetQueue = new ConcurrentLinkedQueue<UDPPacket>();
	private int queueLimit;
	private int seqNum = 1;
	private boolean reachedEnd = false;

	private Socket client;
	private BufferedReader in;
	private OutputStream out;
	private PrintWriter textOut;

	/**
	 * Constructor. Sets pertinent fields.
	 * 
	 * @param incoming
	 *            the Socket connected to the client.yup
	 * 
	 * @param udp_man
	 *            the global udp manager, so we know where to send our upd
	 *            packets to
	 * 
	 * @throws IOException
	 *             If input and output streams could not be initialized.
	 */
	public HTTPClientHandler(Socket incoming) throws IOException {
		id = ++clientCount;
		client = incoming;
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		out = client.getOutputStream();
		textOut = new PrintWriter(out, true);
		RoutingTable.getInstance().addtoIds(id, this);
	}

	/**
	 * Continually listens for the client, parses requests, and sends responses.
	 */
	@Override
	public void run() {
		HTTPRequestPacket request = null;

		while (listening) {
			try {

				// Parse request, send response
				request = new HTTPRequestPacket(in);
				HTTPRequestHandler responder = new HTTPRequestHandler(id,
						request, out, textOut);
				responder.determineRequest();

				// Check if we must close the connection.
				String connection = request.getHeader("Connection");
				if (connection != null && connection.equalsIgnoreCase("close"))
					listening = false;
			} catch (IOException e) {
				System.out.format(
						"Failed to read request packet for client %d: %s\n",
						id, e.getMessage());
				listening = false;
			} catch (DCException e) {
				textOut.print("500 Internal Server Error\r\n");
				textOut.print("Connection: Close\r\n\r\n");
				listening = false;
			}
		}

		// Close connection
		try {
			in.close();
			textOut.close();
			out.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return The ID of the client.
	 */
	public int getClientID() {
		return this.id;
	}

	/**
	 * Adds a given UDPPacket to the client.
	 * 
	 * @param packet
	 *            the packet to be added/
	 */
	public void addToQueue(UDPPacket packet) {
		// System.out.println("Added packet to queue");
		if (packet.getSequenceNumber() != seqNum) {
			System.out.println("Out of order: " + packet.getSequenceNumber()
					+ ", " + seqNum);
			// TODO: Send a NAK?
		}
		seqNum++;
		packetQueue.add(packet);
		if (packet.getType() == UDPPacketType.END) {
			System.out.println("GOT END");
			reachedEnd = true;
			queueLimit = packet.getSequenceNumber();
			// System.out
			// .println("sequence number: " + packet.getSequenceNumber());
			// System.out.println("Queue size: " + packetQueue.size());
		}
		if (reachedEnd && (queueLimit == packetQueue.size())) {
			// System.out.println("GOT EVERYTHING");
			sendQueueToClient();
		}
	}

	/**
	 * Called when the queue has every necessary packet. Sends data to the
	 * client.
	 */
	private void sendQueueToClient() {
		System.out.print("Mirroring packet(s)...");
		for (UDPPacket packet : packetQueue) {
			byte[] packetData = packet.getData();
			try {
				out.write(packetData, 0, packetData.length);
				// System.out.println(client.isClosed());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		packetQueue = new ConcurrentLinkedQueue<UDPPacket>();
		reachedEnd = false;
		queueLimit = 0;

	}

}
