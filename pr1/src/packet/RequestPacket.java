package packet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import server.DCException;

public class RequestPacket {
	private int clientId;
	private String request;
	private Map<String, String> headers = new HashMap<String, String>();

	public RequestPacket(int id, BufferedReader packet) throws DCException,
			IOException {
		// First parse header line to get request
		clientId = id;
		String line = packet.readLine();
		System.out.println("\n\nNow parsing packet from client " + clientId
				+ "...");
		System.out.println(line);

		// Get request from middle field
		if (line == null)
			throw new DCException();
		String[] tokens = line.split(" ");
		if (tokens.length < 3)
			throw new IOException("Received invalid header.");
		request = tokens[1];
		if (request.equals("/"))
			request = "index.html";

		// Then loop through header and add to map
		while (!(line = packet.readLine()).equals("")) {
			System.out.format("%s\n", line);
			tokens = line.split(":");
			if (tokens.length >= 2)
				headers.put(tokens[0], tokens[1].trim());
		}
	}

	public String getRequest() {
		return request;
	}

	public String getHeader(String key) {
		return headers.get(key);
	}
}
