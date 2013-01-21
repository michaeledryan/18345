package echo;

import java.io.*;
import java.net.*;


public class Listener implements Runnable
{
    
    ServerSocket socket;
    int port;
    @Override
    public void run()
    {
        // First bind to socket
        try
        {
            port = 8888;
            socket = new ServerSocket(port);
            System.out.println("Listener now running");
        } 
        catch (IOException e)
        {
            System.out.format("Could not listen on port: %d\n", port);
            System.exit(-1);
        }
        
        // Listen for clients
        while(true)
        {
            Socket client;
            BufferedReader in;
            PrintWriter out;
            try
            {
                System.out.print("\nListening... ");
                client = socket.accept();
                in = new BufferedReader(new InputStreamReader(
                                            client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);
                System.out.println("Client found!\n");
                out.println("Type \"bye\" to end terminate connection.");
                
                String incoming = "";
                while(!incoming.equals("bye"))
                {
                    incoming = in.readLine();
                    System.out.format("Client: %s\n",incoming);
                    out.println(incoming);
                }
                in.close();
                out.close();
                client.close();
                System.out.println("Closed connection.");
                
            }
            catch(IOException e)
            {
                System.out.println("Error connecting to client");
            }
        }
    }
    
}
