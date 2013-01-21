package echo;

public class EchoServer
{
    
    public static void main(String[] args)
    {
        Listener l = new Listener();
        System.out.println("Starting echo server");
        l.run();
    }
    
}
