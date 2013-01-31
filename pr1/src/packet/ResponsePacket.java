package packet;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Asembles a response to the GET request.
 * 
 * @author Michael
 * 
 */
public class ResponsePacket {
	private RequestPacket request;
	private Map<String, String> headers = new HashMap<String, String>();
	private String response;

	public ResponsePacket(RequestPacket request) {
		this.request = request;

		String filename = "C:\\Users\\Michael\\Documents\\GitHub\\18345\\pr1\\content\\"
				+ request.getRequest();
		FileReader target = null;
		try {
			target = new FileReader(new File(filename));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		response = "HTTP/1.1 200 OK\r\n";

		makeHeaders();

		char[] b = new char[114];

		try {
			target.read(b);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// System.out.println(new String(b));
		response += "\r\n" + new String(b);
		System.out.println("\n\nPrinting response packet:");
		System.out.println(response);

	}

	/*
	 * Not sure if we need this, but I figured I'd mimic breaking down the other
	 * packet
	 */
	private void makeHeaders() {
		headers.put("Connection:", "Keep-Alive");
		headers.put("Date:", new Date().toString());
		headers.put("Content-Length:", "114");

		response += "Connection: Keep-Alive\n" + "Date: "
				+ new Date().toString() + "\nContent-Length: " + "114\n";

	}

	public String getResponse() {
		return response;
	}

}
