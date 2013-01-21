package server;

public class ClientHandler implements Runnable {

	@Override
	public void run() {
		System.out.println("You made a connection!");
	}

}
