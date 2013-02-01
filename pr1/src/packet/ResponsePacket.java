package packet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Assembles a response to the GET request.
 */
public class ResponsePacket {
	private RequestPacket request;
	private PrintWriter out;
	private File target;
	private Map<String, String> headers = new HashMap<String, String>();

	private String header;

	public ResponsePacket(RequestPacket request, PrintWriter output) {
		this.request = request;
		this.out = output;
	}

	public void sendResponse() {
		String filename = "www/" + request.getRequest();
		target = new File(filename);
		if (!target.exists()) {
			this.send404();
			return;
		}

		header = "HTTP/1.1 200 OK\r\n";
		makeHeaders();
		out.write(header);
		out.flush();

		System.out.println("\n\nPrinting response header:");
		System.out.println(header);

		/*
		 * TODO: find a better way to copy then reading the entire array into a
		 * buffer then pushing back out
		 */
		char[] buffer = new char[(int) target.length()];
		try {
			new FileReader(target).read(buffer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not read file.");
			return;
		}
		out.print(buffer);
		out.print("\r\n\r\n");
		out.flush();
	}

	private void send404() {
		header = "HTTP/1.1 404 Not Found\r\n";
		makeHeaders();

		System.out.println("\n\nPrinting 404 header:");
		System.out.println(header);
		out.write(header);
		out.flush();
	}

	/*
	 * Not sure if we need this, but I figured I'd mimic breaking down the other
	 * packet
	 */
	private void makeHeaders() {
		/*
		 * headers.put("Connection:", "Keep-Alive"); headers.put("Date:", new
		 * Date().toString()); headers.put("Content-Length:",
		 * Long.toString(target.length()));
		 */

		String connection = request.getHeader("Connection");
		if (connection != null && connection.equals("Close"))
			header += "Connection: Close\r\n";
		else
			header += "Connection: Keep-Alive\r\n";
		header += "Date: " + new Date().toString() + "\r\n";
		header += "Content-Length: " + target.length() + "\r\n";
		header += "\r\n";
	}

	public String getHeader() {
		return header;
	}

}
