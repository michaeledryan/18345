package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import packet.RequestPacket;
import packet.ResponsePacket;

public class ClientHandler implements Runnable {

	private static int clientCount = 0;
	private int id;
	private boolean listening = true;

	private Socket client;
	private BufferedReader in;
	private OutputStream out;
	private PrintWriter textOut;

	public ClientHandler(Socket incoming) throws IOException {
		id = ++clientCount;
		client = incoming;
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		out = client.getOutputStream();
		textOut = new PrintWriter(out, true);
	}

	@Override
	public void run() {
		System.out.format("Connected to client #%d\n", id);

		// do {
		RequestPacket request = null;

		while (listening) {
			try {
				System.out.println("Now wait for incoming from client " + id);
				request = new RequestPacket(in);
				System.out.println("Packet from client " + id + " parsed.");
				ResponsePacket response = new ResponsePacket(request, out,
						textOut);
				System.out.println("Response to client " + id + " sent.");
				if (!response.sendResponse())
					listening = false;

				String connection = request.getHeader("Connection");
				if (connection != null && connection.equalsIgnoreCase("close"))
					listening = false;
			} catch (IOException e) {
				System.out.format(
						"Failed to read request packet for client %d: %s\n",
						id, e.getMessage());
				listening = false;
			} catch (DCException e) {
				System.out.print("Client disconnected... ");
				listening = false;
			}
		}

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
