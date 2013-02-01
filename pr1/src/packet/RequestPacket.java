package packet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import server.DCException;

public class RequestPacket {
	private String request;
	private Map<String, String> headers = new HashMap<String, String>();

	public RequestPacket(BufferedReader packet) throws DCException, IOException {
		// First parse header line to get request
		System.out.println("Now parsing packet...");
		String line = packet.readLine();
		System.out.println(line);

		// Get request from middle field
		if (line == null)
			throw new DCException();
		String[] tokens = line.split(" ");
		if (tokens.length < 3)
			throw new IOException();
		request = tokens[1];
		if (request.equals("/"))
			request = "index.html";

		// Then loop through header and add to map
		while (!(line = packet.readLine()).equals("")) {
			System.out.format("%s\n", line);
			tokens = line.split(":");
			if (tokens.length >= 2)
				headers.put(tokens[0], tokens[1].trim());
			else
				System.out.println(tokens);
		}

		System.out.println("Packet parsed.");
	}

	public String getRequest() {
		return request;
	}

	public String getHeader(String key) {
		return headers.get(key);
	}
}
