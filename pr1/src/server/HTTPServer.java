package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServer implements Runnable {

	private int portNum;
	private ServerSocket socket;

	public HTTPServer(int port) {
		portNum = port;
	}

	@Override
	public void run() {
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
				handle = new ClientHandler(client);
				new Thread(handle).start();
			} catch (IOException e) {
				System.out.println("Error connecting to client");
			}
		}
	}

}
