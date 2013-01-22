package server;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class HTTPServer implements Runnable {

	private int portNum;
	private ServerSocket socket;
	private Set<ClientHandler> connections = new HashSet<ClientHandler>();

	public HTTPServer(int port) {
		portNum = port;
	}

	@Override
	public void run() {
		System.out.println("I'm a server!");
		try {
			socket = new ServerSocket(portNum);
			System.out.format("Now listening on port %d\n", portNum);
		} catch (IOException e) {
			System.out.format("Could not listen on port %d\n", portNum);
			System.exit(-1);
		}

		// Listen for clients
		while (true) {
			Socket client;
			ClientHandler handle;
			try {
				client = socket.accept();
				System.out.println("Client found!\n");

				handle = new ClientHandler(client);
				connections.add(handle);
				new Thread(handle).run();
			} catch (IOException e) {
				System.out.println("Error connecting to client");
			}
		}
	}

}
