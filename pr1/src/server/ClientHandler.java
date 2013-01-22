package server;

import java.io.*;
import java.net.*;

import packet.*;

public class ClientHandler implements Runnable {

	private static int clientCount = 0;
	private int id;

	private Socket client;
	private BufferedReader in;
	private PrintWriter out;

	public ClientHandler(Socket incoming) throws IOException {
		id = ++clientCount;
		client = incoming;
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		out = new PrintWriter(client.getOutputStream(), true);
	}

	@Override
	public void run() {
		System.out.format("Connected to client #%d\n", id);
		// out.format("You are client #%d\n", id);

		RequestPacket request;
		try {
			request = new RequestPacket(in);
		} catch (IOException e) {
			System.out.format("Failed to read request packet for client %d.\n",
					id);
		}

		out.print("HTTP/1.1 404 NOT FOUND\r\n\r\n");
		out.flush();
		System.out.format("Goodbye client %d\n", id);
		try {
			in.close();
			out.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
