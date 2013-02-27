package edu.cmu.ece;

import edu.cmu.ece.backend.UDPManager;
import edu.cmu.ece.frontend.HTTPServer;

/**
 * Main class. Executes the program.
 * 
 * @author Michaels
 */
public class VodServer {

	/**
	 * Main method. Sets up the server.
	 * 
	 * @param args
	 *            port number
	 */
	public static void main(String[] args) {
		int httpPort = 18345;
		int udpPort = 18346;

		// Parse args
		if (args.length > 1) {
			if (args.length > 2) {
				System.out.println("Discarding extra args.");
			}

			try {
				httpPort = Integer.parseInt(args[0]);
				udpPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Port must be an integer");
				System.exit(1);
			}
		}

		// Run servers on their own threads
		UDPManager udp = new UDPManager(udpPort);
		HTTPServer http = new HTTPServer(httpPort, udp);
		new Thread(udp).start();
		new Thread(http).start();
	}

}
