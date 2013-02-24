package edu.cmu.ece.packet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.ece.server.DCException;

/**
 * Parses an HTTP request. Creates a map of header fields to values.
 * 
 * @author Michaels
 * 
 */
public class RequestPacket {
	private int clientId;
	private String request;
	private Map<String, String> headers = new HashMap<String, String>();

	public RequestPacket(int id, BufferedReader packet) throws DCException,
			IOException {

		// First parse the header line to get the request
		clientId = id;
		String line = packet.readLine();

		// Get the request from the middle field
		if (line == null)
			throw new DCException();
		String[] tokens = line.split(" ");
		if (tokens.length < 3)
			throw new IOException("Received invalid header.");
		request = tokens[1];
		if (request.equals("/"))
			request = "index.html";

		// Loop through header, adding fields and values to map
		while (!(line = packet.readLine()).equals("")) {
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
