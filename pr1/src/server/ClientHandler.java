package server;

import java.io.*;
import java.net.*;

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
		out.format("You are client #%d\n", id);
	}

}
