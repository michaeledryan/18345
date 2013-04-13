package edu.cmu.ece.packet;

import java.io.PrintWriter;
import java.util.Date;

import edu.cmu.ece.backend.PeerData;
import edu.cmu.ece.routing.NetworkGraph;

public class HTTPResponses {
	/**
	 * Sends a 404 to the client.
	 * 
	 * @return
	 */
	public static void send404(HTTPRequestPacket request, PrintWriter out) {
		String header = "HTTP/1.1 404 Not Found\r\n";
		String connection = request.getHeader("Connection");
		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + HTTPResponseHeader.formatDate(new Date()) + "\r\n";

		// Create simple 404 page
		String page = "<html><head><title>404 Not Found</title></head>"
				+ "<body><center><h1>404 Not Found</h1> The page you requested "
				+ "was not found.</center></body></html>";

		// Add the 404 page info
		header += "Content-Type: text/html\r\n";
		header += "Content-Length: " + page.length() + "\r\n";
		header += "\r\n";

		out.write(header);
		out.write(page);
		out.flush();
	}

	public static void sendPeerConfigMessage(String path,
			HTTPRequestPacket request, PrintWriter out, PeerData peerdata) {
		String header = "HTTP/1.1 200 OK\r\n";
		String connection = request.getHeader("Connection");

		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + HTTPResponseHeader.formatDate(new Date()) + "\r\n";

		// Create simple page
		String page = "<!doctype html><head><title>Peer configuration</title></head>"
				+ "<body><ul> Config data: </ul><li>File path:"
				+ path
				+ "</li><li> Peer hostname: "
				+ peerdata.getIP()
				+ "</li><li>Peer port: "
				+ peerdata.getPort()
				+ "</li></body></html>";

		// Add the page info
		header += "Content-Type: text/html\r\n";
		header += "Content-Length: " + page.length() + "\r\n";
		header += "\r\n";

		out.write(header);
		out.write(page);
		out.flush();

	}

	/**
	 * Respond to add requests that give a UUID.
	 * 
	 * @param path
	 * @param request
	 * @param out
	 * @param uuid
	 * @param rate
	 */
	public static void sendPeerUUIDConfigMessage(String path,
			HTTPRequestPacket request, PrintWriter out, String uuid, String rate) {
		String header = "HTTP/1.1 200 OK\r\n";
		String connection = request.getHeader("Connection");

		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + HTTPResponseHeader.formatDate(new Date()) + "\r\n";

		// Create simple page
		String page = "<!doctype html><head><title>Peer configuration</title></head>"
				+ "<body><ul> Config data: </ul><li>File path:"
				+ path
				+ "</li><li> Peer uuid: "
				+ uuid
				+ "</li><li>Peer rate: "
				+ rate + "</li></body></html>";

		// Add the page info
		header += "Content-Type: text/html\r\n";
		header += "Content-Length: " + page.length() + "\r\n";
		header += "\r\n";

		out.write(header);
		out.write(page);
		out.flush();

	}

	/**
	 * Respond to addneighbor requests.
	 * 
	 * @param request
	 * @param out
	 * @param uuid
	 * @param peerdata
	 * @param frontend
	 */
	public static void sendAddNeighborMessage(HTTPRequestPacket request,
			PrintWriter out, String uuid, String ip, String frontend,
			String backend) {
		String header = "HTTP/1.1 200 OK\r\n";
		String connection = request.getHeader("Connection");

		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + HTTPResponseHeader.formatDate(new Date()) + "\r\n";

		// Create simple page
		String page = "<!doctype html><head><title>Peer configuration</title></head>"
				+ "<body><ul> Config data: </ul><li>Peer uuid:"
				+ uuid
				+ "</li><li> Peer hostname: "
				+ ip
				+ "</li><li>Peer backend port: "
				+ backend
				+ "</li><li>Peer frontend port: "
				+ frontend
				+ "</li></body></html>";

		// Add the page info
		header += "Content-Type: text/html\r\n";
		header += "Content-Length: " + page.length() + "\r\n";
		header += "\r\n";

		out.write(header);
		out.write(page);
		out.flush();

	}

	public static void sendBitRateConfigMessage(int rate,
			HTTPRequestPacket request, PrintWriter out) {
		String header = "HTTP/1.1 200 OK\r\n";
		String connection = request.getHeader("Connection");

		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + HTTPResponseHeader.formatDate(new Date()) + "\r\n";

		// Create simple page
		String page = "<!doctype html><head><title>Back-end Configuration</title></head>"
				+ "<body><h3> New bit rate: " + rate + "</h3></body></html>";

		// Add the page info
		header += "Content-Type: text/html\r\n";
		header += "Content-Length: " + page.length() + "\r\n";
		header += "\r\n";

		out.write(header);
		out.write(page);
		out.flush();

	}

	public static void sendUUID(HTTPRequestPacket request, PrintWriter out) {
		String header = "HTTP/1.1 200 OK\r\n";
		String connection = request.getHeader("Connection");

		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + HTTPResponseHeader.formatDate(new Date()) + "\r\n";

		String page = "{\"uuid\":"
				+ NetworkGraph.getInstance().getUUID().toString() + "\"}";

		header += "Content-Type: text/html\r\n";
		header += "Content-Length: " + page.length() + "\r\n";
		header += "\r\n";

		out.write(header);
		out.write(page);
		out.flush();

	}
	
	public static void sendNeighbors(HTTPRequestPacket request, PrintWriter out) {
		String header = "HTTP/1.1 200 OK\r\n";
		String connection = request.getHeader("Connection");

		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + HTTPResponseHeader.formatDate(new Date()) + "\r\n";

		String page = NetworkGraph.getInstance().getNeighborJSONforWeb();
		
		header += "Content-Type: text/html\r\n";
		header += "Content-Length: " + page.length() + "\r\n";
		header += "\r\n";
		
		out.write(header);
		out.write(page);
		out.flush();
		
	}
	
	public static void sendNetworkMap(HTTPRequestPacket request, PrintWriter out) {
		String header = "HTTP/1.1 200 OK\r\n";
		String connection = request.getHeader("Connection");

		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + HTTPResponseHeader.formatDate(new Date()) + "\r\n";

		String page = NetworkGraph.getInstance().getNetworkMapJSONforWeb();

		header += "Content-Type: text/html\r\n";
		header += "Content-Length: " + page.length() + "\r\n";
		header += "\r\n";

		out.write(header);
		out.write(page);
		out.flush();

	}
}