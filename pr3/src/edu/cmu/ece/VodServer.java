package edu.cmu.ece;

import edu.cmu.ece.backend.UDPManager;
import edu.cmu.ece.backend.UDPSender;
import edu.cmu.ece.frontend.HTTPServer;
import edu.cmu.ece.frontend.ParseConf;

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
		int httpPort = 8345;
		int udpPort = 8346;

		// Parse args
		if (args.length > 1) {
			if (args.length > 4) {
				System.err.println("Discarding extra args.");
			}

			if (!args[0].equals("-c")) {

				try {
					httpPort = Integer.parseInt(args[0]);
					udpPort = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					System.err.println("Port must be an integer");
					System.exit(1);
				}
			} else {
				ParseConf parser = new ParseConf(args[1]);
				httpPort = parser.getFrontendPort();
				udpPort = parser.getBackendPort();
			}

			/*
			 * GLOBAL TODO: Global packet size - we must make sure the HTTP
			 * packets have a max size that is less than the max packet size of
			 * our UDP packet plus its own headers. Not sure how to do this
			 * ~mnye
			 */

		}
		
		// Run servers on their own threads
		UDPManager.setPort(udpPort);
		HTTPServer http = new HTTPServer(httpPort);
		new Thread(UDPManager.getInstance()).start();
		new Thread(UDPSender.getInstance()).start();
		new Thread(http).start();
	}
}
