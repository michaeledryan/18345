package edu.cmu.ece.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;

import edu.cmu.ece.DCException;
import edu.cmu.ece.backend.UDPManager;
import edu.cmu.ece.packet.HTTPRequestPacket;
import edu.cmu.ece.packet.UDPPacket;
import edu.cmu.ece.packet.UDPPacketType;
import edu.cmu.ece.routing.RoutingTable;

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
	private boolean sending = false;
	private boolean gotAck = false;

	private PriorityBlockingQueue<UDPPacket> packetQueue = new PriorityBlockingQueue<UDPPacket>();
	private AbstractSet<Integer> received = new ConcurrentSkipListSet<Integer>();
	private int nextSeqNumToSend;

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
	 *            the global udp manager, so we know where to send our udp
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
		
		//Set bitrate if it doesn't yet exist
		String ip = client.getInetAddress().getHostAddress();
		RoutingTable router = RoutingTable.getInstance();
		if(!router.bitRateSet(ip))
			router.setBitRate(ip, 0);

		System.out.println("HTTP Client: "
				+ incoming.getInetAddress().getHostAddress() + ":"
				+ incoming.getPort());
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
						client.getInetAddress().getHostAddress(), request, in,
						out, textOut, client, this);
				// Clear received queue and its next element
				nextSeqNumToSend = 0;
				received.clear();

				// Set this flag to detect whether we have received any response
				// data to the original request
				gotAck = false;

				// System.out.println("HTTP request received, client " + id);
				responder.determineRequest();

				// Check if we must close the connection.
				String connection = request.getHeader("Connection");
				if (connection != null && connection.equalsIgnoreCase("close"))
					listening = false;

				// Check if we need to just die
				if (Thread.interrupted())
					return;
			} catch (IOException e) {
				System.err.format(
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

	public boolean getGotAck() {
		return this.gotAck;
	}

	/**
	 * Adds a given UDPPacket to the client. Just adds everything
	 * 
	 * @param packet
	 *            the packet to be added/
	 */
	public void addToQueue(UDPPacket packet) {
		gotAck = true;
		
		System.out.println(" Adding to a queue!");
		if (client.isClosed()) {
			// System.out.println("\tClient is dead, dismiss incoming");
			packetQueue.clear();

			// Send kill message to sender
			try {
				UDPManager.getInstance().sendPacket(
						new UDPPacket(packet.getClientID(), packet
								.getRequestID(), packet.getRemoteIP(), packet
								.getRemotePort(),
								new byte[0], UDPPacketType.KILL, packet
										.getSequenceNumber()).getPacket());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}

			return;
		}

		// System.out.println("\tWaiting on " + nextSeqNumToSend);
		int seqNum = packet.getSequenceNumber();
		if (received.contains(seqNum))
			return;

		packetQueue.add(packet);
		received.add(seqNum);
		// Send a NACK if we don't have the packet we need. This will ensure resending.
		if (seqNum > nextSeqNumToSend) {
			try {
				UDPManager.getInstance().sendPacket(
						new UDPPacket(id, packet.getRequestID(), packet
								.getRemoteIP(), packet.getRemotePort(),
								new byte[0], UDPPacketType.NAK,
								nextSeqNumToSend).getPacket());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (!sending) {
			sendQueueToClient();
		}
		return;
	}

	/**
	 * Called any time we have received a packet. If we have the next packet in
	 * order, then send everything we can.
	 */
	private void sendQueueToClient() {
		sending = true;

		System.out.println("\tsending from a queue!");
		while (!packetQueue.isEmpty()
				&& packetQueue.peek().getSequenceNumber() == nextSeqNumToSend) {
			UDPPacket packet;
			try {
				packet = packetQueue.take();
			} catch (InterruptedException e1) {
				System.err.println("Could not get packet to send to client.");
				return;
			}

			nextSeqNumToSend++;

			byte[] packetData = packet.getData();
			try {
				out.write(packetData, 0, packetData.length);
				out.flush();
			} catch (IOException e) {
				System.err.println("Could not mirror packet to client: " + e);
				if (client.isClosed()) {
					System.out.println("\tClient is dead.");
					packetQueue.clear();
					gotAck = false;
					break;
				}
			}

		}
		
		sending = false;
	}

}
