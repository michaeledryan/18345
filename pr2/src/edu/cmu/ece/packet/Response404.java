package edu.cmu.ece.packet;

import java.io.PrintWriter;
import java.util.Date;

public class Response404 {
	/**
	 * Sends a 404 to the client.
	 * 
	 * @return
	 */
	public static void send404(RequestPacket request, PrintWriter out) {
		String header = "HTTP/1.1 404 Not Found\r\n";
		String connection = request.getHeader("Connection");
		if (connection != null && connection.equalsIgnoreCase("close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + ResponseHeader.formatDate(new Date()) + "\r\n";

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
}
