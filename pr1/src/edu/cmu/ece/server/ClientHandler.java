package edu.cmu.ece.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import edu.cmu.ece.packet.RequestPacket;
import edu.cmu.ece.packet.ResponsePacket;


/**
 * Manages a connection to a given client.
 * 
 * @author Michaels
 * 
 */
public class ClientHandler implements Runnable {

	private static int clientCount = 0;
	private int id;
	private boolean listening = true;

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
	 * @throws IOException
	 *             If input and output streasm could not be initialized.
	 */
	public ClientHandler(Socket incoming) throws IOException {
		id = ++clientCount;
		client = incoming;
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		out = client.getOutputStream();
		textOut = new PrintWriter(out, true);
	}

	/**
	 * Continually listens for the client, parses requests, and sends responses.
	 */
	@Override
	public void run() {
		System.out.format("Connected to client #%d\n", id);

		RequestPacket request = null;

		while (listening) {
			try {
				
				// Parse request, send response
				request = new RequestPacket(id, in);
				ResponsePacket response = new ResponsePacket(id, request, out,
						textOut);
				if (!response.sendResponse())
					listening = false;

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
				System.out.println("Disconnecting from client on timeout... ");
				textOut.print("500 Internal Server Error\r\n");
				textOut.print("Connection: Close\r\n\r\n");
				listening = false;
			}
		}

		// Close connection
		System.out.format("Goodbye client %d\n", id);
		try {
			in.close();
			textOut.close();
			out.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
