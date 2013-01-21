package server;

import java.util.HashSet;
import java.util.Set;

public class HTTPServer implements Runnable {

	private static int portNum = 80;
	private Set<ClientHandler> connections = new HashSet<ClientHandler>();

	@Override
	public void run() {
		System.out.println("I'm a server!");

	}

}
