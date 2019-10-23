package Connection;

import java.net.*;
import java.io.*;

public class Client {

    private Socket socket = null;

    public Client(String address, int port) throws IOException {
        try
        {
            socket = new Socket(address, port);
            System.out.println("Connected");
        }
        catch(UnknownHostException u)
        {
            System.out.println(u);
        }
    }

}
