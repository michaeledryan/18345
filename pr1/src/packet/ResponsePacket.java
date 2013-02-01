package packet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Assembles a response to the GET request.
 */
public class ResponsePacket {
	private RequestPacket request;
	private OutputStream out;
	private PrintWriter textOut;
	private File target;
	private Map<String, String> headers = new HashMap<String, String>();

	private String header;

	public ResponsePacket(RequestPacket request, OutputStream output) {
		this.request = request;
		this.out = output;
		this.textOut = new PrintWriter(out, true);
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
		textOut.write(header);
		textOut.flush();

		System.out.println("\n\nPrinting response header:");
		System.out.println(header);

		/*
		 * TODO: find a better way to copy then reading the entire array into a
		 * buffer then pushing back out
		 */
		byte[] buffer = new byte[(int) target.length()];
		try {
			new FileInputStream(target).read(buffer);
			out.write(buffer, 0, (int) target.length());
			out.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not read/write file.");
			return;
		}
	}

	private void send404() {
		header = "HTTP/1.1 404 Not Found\r\n";
		makeHeaders();

		System.out.println("\n\nPrinting 404 header:");
		System.out.println(header);
		textOut.write(header);
		textOut.flush();
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
		header += "Cache-Control: max-age=0\r\n";
		header += "Content-Length: " + target.length() + "\r\n";
		header += "Content-Type: "
				+ URLConnection.guessContentTypeFromName(target.getName())
				+ "\r\n";
		header += "\r\n";
	}

	public String getHeader() {
		return header;
	}

}
