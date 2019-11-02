package Connection;

import Model.MessageModel;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientTalker {
    public static void sendRequestTo(String address, int port,Action action, Object data)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try{
                    Socket socket = new Socket(address, port);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    Signal request = new Signal(action,true,data,"");
                    Signal.sendResponse(request,objectOutputStream);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }
}
