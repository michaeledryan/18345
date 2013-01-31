package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

	//	do {
		RequestPacket request = null;
		try {
			request = new RequestPacket(in);
		} catch (IOException e) {
			System.out.format("Failed to read request packet for client %d.\n",
					id);
			listening = false;
		}

		if (request.getRequest().equals("favicon.ico"))
			out.print("HTTP/1.1 404 NOT FOUND\r\n\r\n");
		else {
		ResponsePacket response = new ResponsePacket(request);
			//out.print("HTTP/1.1 404 NOT FOUND\r\n\r\n");
			out.print(response.getResponse());
		}
		out.flush();
		System.out.format("Goodbye client %d\n", id);
	//	} while (listening);
		try {
			in.close();
			out.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
