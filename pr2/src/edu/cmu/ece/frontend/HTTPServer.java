package edu.cmu.ece.frontend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import edu.cmu.ece.backend.UDPManager;

/**
 * Main thread that spins off ClientHandlers to deal with connections.
 * 
 * @author Michaels
 * 
 */
public class HTTPServer implements Runnable {

	private int portNum;
	private UDPManager udp;
	private ServerSocket socket;

	/**
	 * Constructor
	 * 
	 * @param port
	 *            the port on which we listen
	 */
	public HTTPServer(int port, UDPManager udp_man) {
		portNum = port;
		udp = udp_man;
	}

	/**
	 * Runs the server.
	 */
	@Override
	public void run() {
		// open a socket
		try {
			socket = new ServerSocket(portNum);
			System.out.format("Now listening for HTTP on port %d\n", portNum);
		} catch (IOException e) {
			System.out
					.format("Could not listen for HTTP on port %d\n", portNum);
			System.exit(-1);
		}

		// Listen for clients
		while (true) {
			Socket client;
			ClientHandler handle;
			try {
				client = socket.accept();
				handle = new ClientHandler(client, udp);
				new Thread(handle).start();
			} catch (IOException e) {
				System.out.println("Error connecting to client on HTTP.");
			}
		}
	}

}
